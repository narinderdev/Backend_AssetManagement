package com.example.eam.ServiceMaintenance.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Entity.AssetLocation;
import com.example.eam.Asset.Repository.AssetLocationRepository;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.Enum.ServiceRequestStatus;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestCreateDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestUpdateDto;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import com.example.eam.ServiceMaintenance.Repository.ServiceMaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceMaintenanceServiceTest {

    @Mock
    private ServiceMaintenanceRepository serviceRepo;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetLocationRepository assetLocationRepository;

    @InjectMocks
    private ServiceMaintenanceService service;

    @BeforeEach
    void stubRequestIdGeneration() {
        lenient().when(serviceRepo.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        lenient().when(serviceRepo.existsByRequestId(any())).thenReturn(false);
    }

    @Test
    void create_withAssetUsesAssetLocation() {
        Asset asset = Asset.builder().id(3L).assetId("A-1").assetName("Pump").build();
        AssetLocation loc = AssetLocation.builder().asset(asset).location("Plant X").build();

        when(assetRepository.findById(3L)).thenReturn(Optional.of(asset));
        when(assetLocationRepository.findByAsset_Id(3L)).thenReturn(Optional.of(loc));
        when(serviceRepo.save(any())).thenAnswer(invocation -> {
            ServiceMaintenance sr = invocation.getArgument(0);
            sr.setId(10L);
            return sr;
        });

        ServiceRequestCreateDto dto = new ServiceRequestCreateDto();
        dto.setAssetId(3L);
        dto.setRequesterName("Ops");
        dto.setMaintenanceType(MaintenanceType.CORRECTIVE);
        dto.setPriority(RequestPriority.MEDIUM);
        dto.setShortTitle("Leak");

        var resp = service.create(dto);

        assertEquals("Plant X", resp.getLocation());
        assertEquals(ServiceRequestStatus.NEW, resp.getStatus());
        assertNotNull(resp.getRequestId());
    }

    @Test
    void update_rejectsWhenApproved() {
        ServiceMaintenance existing = ServiceMaintenance.builder()
                .id(5L)
                .requestId("SR-000001")
                .status(ServiceRequestStatus.APPROVED)
                .build();

        when(serviceRepo.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));

        ServiceRequestUpdateDto dto = new ServiceRequestUpdateDto();
        dto.setShortTitle("New title");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(5L, dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
