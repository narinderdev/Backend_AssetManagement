package com.example.eam.WorkOrder.Repository;


import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.AssetCriticality;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    boolean existsByPmPlan_IdAndPmDueDate(Long pmPlanId, LocalDate pmDueDate);

    boolean existsByWorkOrderId(String workOrderId);

    Optional<WorkOrder> findByIdAndDeletedFalse(Long id);
    Optional<WorkOrder> findByIdAndDeletedFalseAndCompanyId(Long id, Long companyId);
    Optional<WorkOrder> findTopByWorkOrderIdAndDeletedFalseAndCompanyIdOrderByIdDesc(String workOrderId, Long companyId);
    Optional<WorkOrder> findTopByWoNumberAndDeletedFalseAndCompanyIdOrderByIdDesc(String woNumber, Long companyId);

    Page<WorkOrder> findByDeletedFalse(Pageable pageable);
    Page<WorkOrder> findByDeletedFalseAndCompanyId(Long companyId, Pageable pageable);

    Page<WorkOrder> findByDeletedFalseAndStatusIn(Collection<WorkOrderStatus> statuses, Pageable pageable);
    Page<WorkOrder> findByDeletedFalseAndStatusInAndCompanyId(Collection<WorkOrderStatus> statuses, Long companyId, Pageable pageable);

    Optional<WorkOrder> findByLinkedRequest_Id(Long serviceRequestPkId);
    Optional<WorkOrder> findByLinkedRequest_IdAndCompanyId(Long serviceRequestPkId, Long companyId);

    long countByStatusInAndDeletedFalse(Collection<WorkOrderStatus> statuses);
    long countByStatusInAndDeletedFalseAndCompanyId(Collection<WorkOrderStatus> statuses, Long companyId);

    long countByStatusAndDeletedFalse(WorkOrderStatus status);
    long countByStatusAndDeletedFalseAndCompanyId(WorkOrderStatus status, Long companyId);

    long countByStatusInAndCreatedAtBetweenAndDeletedFalse(Collection<WorkOrderStatus> statuses,
                                                           LocalDateTime start,
                                                           LocalDateTime end);
    long countByStatusInAndCreatedAtBetweenAndDeletedFalseAndCompanyId(Collection<WorkOrderStatus> statuses,
                                                                       LocalDateTime start,
                                                                       LocalDateTime end,
                                                                       Long companyId);

    long countByTargetCompletionDateBeforeAndStatusInAndDeletedFalse(LocalDate date,
                                                                     Collection<WorkOrderStatus> statuses);
    long countByTargetCompletionDateBeforeAndStatusInAndDeletedFalseAndCompanyId(LocalDate date,
                                                                                 Collection<WorkOrderStatus> statuses,
                                                                                 Long companyId);

    long countByTargetCompletionDateBetweenAndStatusInAndDeletedFalse(LocalDate start,
                                                                      LocalDate end,
                                                                      Collection<WorkOrderStatus> statuses);

    long countByTargetCompletionDateAfterAndStatusInAndDeletedFalse(LocalDate date,
                                                                    Collection<WorkOrderStatus> statuses);
    long countByTargetCompletionDateAfterAndStatusInAndDeletedFalseAndCompanyId(LocalDate date,
                                                                                Collection<WorkOrderStatus> statuses,
                                                                                Long companyId);

    List<WorkOrder> findTop5ByDeletedFalseOrderByCreatedAtDesc();
    List<WorkOrder> findTop5ByDeletedFalseAndCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<WorkOrder> findTop5ByTargetCompletionDateAfterAndStatusInAndDeletedFalseOrderByTargetCompletionDateAsc(
            LocalDate date,
            Collection<WorkOrderStatus> statuses
    );
    List<WorkOrder> findTop5ByTargetCompletionDateAfterAndStatusInAndDeletedFalseAndCompanyIdOrderByTargetCompletionDateAsc(
            LocalDate date,
            Collection<WorkOrderStatus> statuses,
            Long companyId
    );

    List<WorkOrder> findTop5ByTargetCompletionDateBeforeAndStatusInAndDeletedFalseOrderByTargetCompletionDateAsc(
            LocalDate date,
            Collection<WorkOrderStatus> statuses
    );
    List<WorkOrder> findTop5ByTargetCompletionDateBeforeAndStatusInAndDeletedFalseAndCompanyIdOrderByTargetCompletionDateAsc(
            LocalDate date,
            Collection<WorkOrderStatus> statuses,
            Long companyId
    );

    List<WorkOrder> findByDeletedFalseAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<WorkOrder> findByDeletedFalseAndCreatedAtBetweenAndCompanyId(LocalDateTime start, LocalDateTime end, Long companyId);

    long countByDeletedFalse();
    long countByDeletedFalseAndCompanyId(Long companyId);

    long countByWorkOrderTypeTemplate_Id(Long workOrderTypeId);


    @Query("""
        select distinct wo from WorkOrder wo
        left join wo.assignedTeam team
        left join com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember tm
            on tm.team = team
        where wo.deleted = false
          and (:companyId is null or wo.companyId = :companyId)
          and (
                wo.assignedTechnician.id = :technicianId
             or tm.technician.id = :technicianId
          )
    """)
    Page<WorkOrder> findByTechnicianOrTeamMember(
            @Param("technicianId") Long technicianId,
            @Param("companyId") Long companyId,
            Pageable pageable
    );

    @Query("""
        select count(distinct wo.asset.id)
        from WorkOrder wo
        where wo.deleted = false
          and (:companyId is null or wo.companyId = :companyId)
          and wo.asset is not null
          and wo.asset.criticality = :criticality
          and wo.status in :statuses
          and wo.downtimeStart is not null
          and wo.downtimeEnd is null
    """)
    long countActiveCriticalAssetsDown(@Param("companyId") Long companyId,
                                       @Param("criticality") AssetCriticality criticality,
                                       @Param("statuses") Collection<WorkOrderStatus> statuses);

    long countByAsset_CriticalityAndStatusInAndDowntimeStartBetweenAndDeletedFalse(
            AssetCriticality criticality,
            Collection<WorkOrderStatus> statuses,
            LocalDateTime start,
            LocalDateTime end
    );
    long countByAsset_CriticalityAndStatusInAndDowntimeStartBetweenAndDeletedFalseAndCompanyId(
            AssetCriticality criticality,
            Collection<WorkOrderStatus> statuses,
            LocalDateTime start,
            LocalDateTime end,
            Long companyId
    );

    @Query("""
        select count(distinct wo.id) from WorkOrder wo
        left join com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember tm
            on tm.team = wo.assignedTeam
        where wo.deleted = false
          and (:companyId is null or wo.companyId = :companyId)
          and wo.status in :statuses
          and wo.plannedEndDateTime > :rangeStart
          and wo.plannedStartDateTime < :rangeEnd
          and (
                wo.assignedTechnician.id = :technicianId
             or tm.technician.id = :technicianId
          )
    """)
    long countActiveBookingsForTechnician(
            @Param("technicianId") Long technicianId,
            @Param("companyId") Long companyId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("statuses") Collection<WorkOrderStatus> statuses
    );

    @Query("""
        select wo from WorkOrder wo
        where wo.deleted = false
          and (:companyId is null or wo.companyId = :companyId)
          and wo.plannedStartDateTime is not null
          and wo.plannedEndDateTime is not null
          and wo.status in :statuses
          and wo.plannedEndDateTime > :rangeStart
          and wo.plannedStartDateTime < :rangeEnd
          and (
                (:technicianId is not null and wo.assignedTechnician.id = :technicianId)
             or (:teamId is not null and wo.assignedTeam.id = :teamId)
          )
    """)
    List<WorkOrder> findBookingsForAssignments(
            @Param("technicianId") Long technicianId,
            @Param("teamId") Long teamId,
            @Param("companyId") Long companyId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("statuses") Collection<WorkOrderStatus> statuses
    );

    @Query("""
        select distinct wo from WorkOrder wo
        left join com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember tm
            on tm.team = wo.assignedTeam
        where wo.deleted = false
          and (:companyId is null or wo.companyId = :companyId)
          and wo.plannedStartDateTime is not null
          and wo.plannedEndDateTime is not null
          and wo.status in :statuses
          and wo.plannedEndDateTime > :rangeStart
          and wo.plannedStartDateTime < :rangeEnd
          and (
                wo.assignedTechnician.id = :technicianId
             or tm.technician.id = :technicianId
          )
    """)
    List<WorkOrder> findBookingsForTechnicianCalendar(
            @Param("technicianId") Long technicianId,
            @Param("companyId") Long companyId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("statuses") Collection<WorkOrderStatus> statuses
    );

    @Query("""
        select distinct
            coalesce(wo.assignedTechnician.id, tm.technician.id)
        from WorkOrder wo
        left join com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember tm
            on tm.team = wo.assignedTeam
        where wo.deleted = false
          and (:companyId is null or wo.companyId = :companyId)
          and wo.status in :statuses
          and wo.plannedEndDateTime > :rangeStart
          and wo.plannedStartDateTime < :rangeEnd
          and (wo.assignedTechnician.id is not null or tm.technician.id is not null)
    """)
    List<Long> findDistinctTechnicianIdsWithBookings(
            @Param("companyId") Long companyId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("statuses") Collection<WorkOrderStatus> statuses
    );

    List<WorkOrder> findByDeletedFalseOrderByUpdatedAtDesc(org.springframework.data.domain.Pageable pageable);
    List<WorkOrder> findByDeletedFalseAndCompanyIdOrderByUpdatedAtDesc(Long companyId, org.springframework.data.domain.Pageable pageable);

    @Query("""
        select wo from WorkOrder wo
        left join fetch wo.asset a
        where wo.deleted = false
          and (:companyId is null or wo.companyId = :companyId)
          and (
                (:assetId is null and :assetDbId is null)
             or (
                    a is not null
                and (
                        (:assetId is not null and a.assetId = :assetId)
                     or (:assetDbId is not null and a.id = :assetDbId)
                )
             )
          )
          and (
                (:workOrderId is null and :workOrderDbId is null)
             or (
                        (:workOrderId is not null and (wo.workOrderId = :workOrderId or wo.woNumber = :workOrderId))
                     or (:workOrderDbId is not null and wo.id = :workOrderDbId)
             )
          )
          and (:startDate is null or wo.createdAt >= :startDate)
          and (:endDate is null or wo.createdAt < :endDate)
    """)
    List<WorkOrder> findForBudgetReport(
            @Param("companyId") Long companyId,
            @Param("assetId") String assetId,
            @Param("assetDbId") Long assetDbId,
            @Param("workOrderId") String workOrderId,
            @Param("workOrderDbId") Long workOrderDbId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
