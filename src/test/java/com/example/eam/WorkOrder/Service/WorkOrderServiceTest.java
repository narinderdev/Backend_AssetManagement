package com.example.eam.WorkOrder.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Entity.AssetLocation;
import com.example.eam.Asset.Repository.AssetLocationRepository;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.WorkType;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.ServiceMaintenance.Repository.ServiceMaintenanceRepository;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamRepository;
import com.example.eam.WorkOrder.Dto.WorkOrderCompletionRequest;
import com.example.eam.WorkOrder.Dto.WorkOrderCreateRequest;
import com.example.eam.WorkOrder.Dto.WorkOrderPatchRequest;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Repository.WorkOrderCheckLogRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderLaborEntryRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderMaterialPlanRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderMaterialUsageRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetLocationRepository assetLocationRepository;
    @Mock
    private ServiceMaintenanceRepository serviceMaintenanceRepository;
    @Mock
    private TechnicianRepository technicianRepository;
    @Mock
    private TechnicianTeamRepository technicianTeamRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private WorkOrderLaborEntryRepository workOrderLaborEntryRepository;
    @Mock
    private WorkOrderMaterialUsageRepository workOrderMaterialUsageRepository;
    @Mock
    private WorkOrderMaterialPlanRepository workOrderMaterialPlanRepository;
    @Mock
    private WorkOrderCheckLogRepository workOrderCheckLogRepository;

    @InjectMocks
    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        lenient().when(workOrderLaborEntryRepository.findByWorkOrder_Id(any())).thenReturn(Collections.emptyList());
        lenient().when(workOrderMaterialUsageRepository.findByWorkOrder_Id(any())).thenReturn(Collections.emptyList());
        lenient().when(workOrderMaterialPlanRepository.findByWorkOrder_Id(any())).thenReturn(Collections.emptyList());
        lenient().when(workOrderCheckLogRepository.findByWorkOrder_Id(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void createWorkOrder_withAsset_usesAssetLocationAndGeneratesId() {
        Asset asset = Asset.builder()
                .id(10L)
                .assetId("AST-1")
                .assetName("Pump")
                .build();
        AssetLocation location = AssetLocation.builder()
                .asset(asset)
                .location("Plant 1")
                .build();

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(assetLocationRepository.findByAsset_Id(10L)).thenReturn(Optional.of(location));
        when(workOrderRepository.existsByWorkOrderId(anyString())).thenReturn(false);
        when(workOrderRepository.save(any())).thenAnswer(invocation -> {
            WorkOrder wo = invocation.getArgument(0);
            wo.setId(42L);
            return wo;
        });

        WorkOrderCreateRequest request = new WorkOrderCreateRequest();
        request.setAssetId(10L);
        request.setWorkType(WorkType.CORRECTIVE);
        request.setPriority(PriorityLevel.MEDIUM);
        request.setWoTitle("Fix pump");

        var response = workOrderService.createWorkOrder(request);

        assertEquals("Plant 1", response.getLocation());
        assertEquals(WorkOrderStatus.NEW, response.getStatus());
        assertNotNull(response.getWorkOrderId());
        assertTrue(response.getWorkOrderId().startsWith("WO-"));
    }

    @Test
    void createWorkOrder_withoutAssetAndLocation_throwsBadRequest() {
        WorkOrderCreateRequest request = new WorkOrderCreateRequest();
        request.setWorkType(WorkType.CORRECTIVE);
        request.setPriority(PriorityLevel.MEDIUM);
        request.setWoTitle("Missing location");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> workOrderService.createWorkOrder(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void patchWorkOrder_rejectsCompletedStatusInPatch() {
        WorkOrder existing = baseWorkOrderBuilder().status(WorkOrderStatus.NEW).build();

        when(workOrderRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));

        WorkOrderPatchRequest request = new WorkOrderPatchRequest();
        request.setStatus(WorkOrderStatus.COMPLETED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> workOrderService.patchWorkOrder(5L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("technician completion"));
    }

    @Test
    void patchWorkOrder_allowsTransitionFromNewToApproved() {
        WorkOrder existing = baseWorkOrderBuilder().status(WorkOrderStatus.NEW).build();
        existing.setId(7L);

        when(workOrderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));
        when(workOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrderPatchRequest request = new WorkOrderPatchRequest();
        request.setStatus(WorkOrderStatus.APPROVED);

        var response = workOrderService.patchWorkOrder(7L, request);

        assertEquals(WorkOrderStatus.APPROVED, response.getStatus());
    }

    @Test
    void recordTechnicianCompletion_requiresInProgressStatus() {
        WorkOrder existing = baseWorkOrderBuilder().status(WorkOrderStatus.NEW).build();

        when(workOrderRepository.findByIdAndDeletedFalse(8L)).thenReturn(Optional.of(existing));

        WorkOrderCompletionRequest request = new WorkOrderCompletionRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> workOrderService.recordTechnicianCompletion(8L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void closeWorkOrder_requiresCompletedStatus() {
        WorkOrder existing = baseWorkOrderBuilder().status(WorkOrderStatus.NEW).build();

        when(workOrderRepository.findByIdAndDeletedFalse(9L)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> workOrderService.closeWorkOrder(9L, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    private WorkOrder.WorkOrderBuilder baseWorkOrderBuilder() {
        return WorkOrder.builder()
                .workOrderId("WO-TEST")
                .location("Default Loc")
                .workType(WorkType.CORRECTIVE)
                .priority(PriorityLevel.MEDIUM)
                .woTitle("Test WO")
                .status(WorkOrderStatus.NEW)
                .source(com.example.eam.Enum.WorkOrderSource.MANUAL)
                .deleted(false);
    }
}
