package com.example.eam.WorkOrder.Service;

import com.example.eam.WorkOrder.Entity.WoNumberPool;
import com.example.eam.WorkOrder.Repository.WoNumberPoolRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WoNumberPoolService {

    private static final int BATCH_SIZE = 100;
    private static final int LOW_POOL_THRESHOLD = 10;

    private final WoNumberPoolRepository woNumberPoolRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public String allocateWoNumber(Long workOrderId) {
        if (workOrderId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Work order ID is required for number allocation");
        }

        ensurePoolCapacity();

        String claimed = claimNextAvailable(workOrderId);
        if (claimed != null) {
            return claimed;
        }

        generateBatch(BATCH_SIZE);
        claimed = claimNextAvailable(workOrderId);

        if (claimed == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No work order numbers available");
        }

        ensurePoolCapacity();
        return claimed;
    }

    private void ensurePoolCapacity() {
        long available = woNumberPoolRepository.countByAssignedFalse();
        if (available < LOW_POOL_THRESHOLD) {
            generateBatch(BATCH_SIZE);
        }
    }

    private void generateBatch(int batchSize) {
        long currentMax = fetchMaxWithLock();
        List<WoNumberPool> batch = new ArrayList<>(batchSize);
        for (int i = 1; i <= batchSize; i++) {
            batch.add(WoNumberPool.builder()
                    .woNumber(String.valueOf(currentMax + i))
                    .assigned(false)
                    .assignedAt(null)
                    .assignedToWoId(null)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        try {
            woNumberPoolRepository.saveAll(batch);
        } catch (DataIntegrityViolationException ex) {
            // If a race caused duplicates, retry generation once with refreshed max
            long refreshedMax = fetchMaxWithLock();
            batch.clear();
            for (int i = 1; i <= batchSize; i++) {
                batch.add(WoNumberPool.builder()
                        .woNumber(String.valueOf(refreshedMax + i))
                        .assigned(false)
                        .assignedAt(null)
                        .assignedToWoId(null)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            woNumberPoolRepository.saveAll(batch);
        }
    }

    private long fetchMaxWithLock() {
        Query q = entityManager.createNativeQuery("""
                SELECT ISNULL(MAX(CAST(wo_number AS BIGINT)), 0)
                FROM wo_number_pool WITH (UPDLOCK, HOLDLOCK)
                """);
        Object result = q.getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }

    private String claimNextAvailable(Long workOrderId) {
        Query q = entityManager.createNativeQuery("""
                DECLARE @claimed TABLE (wo_number NVARCHAR(50));

                UPDATE TOP (1) wo_number_pool WITH (UPDLOCK, READPAST, ROWLOCK)
                SET is_assigned = 1,
                    assigned_to_wo_id = :workOrderId,
                    assigned_at = SYSUTCDATETIME()
                OUTPUT inserted.wo_number INTO @claimed(wo_number)
                WHERE is_assigned = 0
                ORDER BY id;

                SELECT wo_number FROM @claimed;
                """);
        q.setParameter("workOrderId", workOrderId);
        @SuppressWarnings("unchecked")
        List<String> rows = q.getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }
}
