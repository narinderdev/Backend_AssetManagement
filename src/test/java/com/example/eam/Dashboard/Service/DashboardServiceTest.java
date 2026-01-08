package com.example.eam.Dashboard.Service;

import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Dashboard.Dto.DashboardResponse;
import com.example.eam.Enum.AssetCriticality;
import com.example.eam.Enum.AssetStatus;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import com.example.eam.ServiceMaintenance.Repository.ServiceMaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ServiceMaintenanceRepository serviceMaintenanceRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void stubCounts() {
        when(serviceMaintenanceRepository.countByDeletedFalseAndStatusIn(anySet())).thenReturn(5L);
        when(serviceMaintenanceRepository.countByDeletedFalseAndStatusInAndRequestDateBetween(anySet(), any(), any()))
                .thenReturn(2L, 1L);

        when(workOrderRepository.countByStatusInAndDeletedFalse(anySet()))
                .thenReturn(7L, 3L, 2L); // active, completed, pending
        when(workOrderRepository.countByStatusInAndCreatedAtBetweenAndDeletedFalse(anySet(), any(), any()))
                .thenReturn(1L, 0L);

        when(workOrderRepository.countByTargetCompletionDateBeforeAndStatusInAndDeletedFalse(any(LocalDate.class), anySet()))
                .thenReturn(1L);
        when(workOrderRepository.countByTargetCompletionDateBetweenAndStatusInAndDeletedFalse(any(LocalDate.class), any(LocalDate.class), anySet()))
                .thenReturn(2L, 1L);

        when(workOrderRepository.countByStatusAndDeletedFalse(WorkOrderStatus.IN_PROGRESS)).thenReturn(4L);

        when(assetRepository.countByCriticalityAndStatusIn(AssetCriticality.CRITICAL,
                EnumSet.of(AssetStatus.OUT_OF_SERVICE, AssetStatus.UNDER_MAINTENANCE)))
                .thenReturn(2L);
    }

    @BeforeEach
    void stubLists() {
        LocalDateTime now = LocalDateTime.now();
        WorkOrder woRecent = WorkOrder.builder()
                .workOrderId("WO-1")
                .woTitle("Recent")
                .priority(PriorityLevel.HIGH)
                .status(WorkOrderStatus.NEW)
                .targetCompletionDate(LocalDate.now().plusDays(1))
                .createdAt(now.minusDays(1))
                .build();

        WorkOrder woCost = WorkOrder.builder()
                .workOrderId("WO-2")
                .woTitle("Costly")
                .priority(PriorityLevel.MEDIUM)
                .status(WorkOrderStatus.COMPLETED)
                .actualTotalCost(new BigDecimal("150.00"))
                .createdAt(now.minusDays(10))
                .build();

        lenient().when(workOrderRepository.findByDeletedFalseAndCreatedAtBetween(any(), any()))
                .thenReturn(List.of(woCost));
        lenient().when(workOrderRepository.findTop5ByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(woRecent));
    }

    @Test
    void getDashboard_returnsAggregatedMetrics() {
        DashboardResponse response = dashboardService.getDashboard();

        assertNotNull(response.getOpenServiceRequests());
        assertEquals(7L, response.getActiveWorkOrders().getValue());
        assertEquals(2L, response.getCriticalAssetsDown().getValue());

        assertEquals(3L, response.getWorkOrdersByStatus().getCompleted());
        assertEquals(4L, response.getWorkOrdersByStatus().getInProgress());
        assertEquals(2L, response.getWorkOrdersByStatus().getPending());

        assertNotNull(response.getMaintenanceCostSummary());
        assertEquals(6, response.getMaintenanceCostSummary().size());

        assertNotNull(response.getRecentWorkOrders());
        assertFalse(response.getRecentWorkOrders().isEmpty());
    }
}
