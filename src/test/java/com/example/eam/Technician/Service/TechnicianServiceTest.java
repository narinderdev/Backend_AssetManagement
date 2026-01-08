package com.example.eam.Technician.Service;

import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Technician.Dto.TechnicianCreateRequest;
import com.example.eam.Technician.Dto.TechnicianPatchRequest;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Repository.TechnicianRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @InjectMocks
    private TechnicianService technicianService;

    @Test
    void createTechnician_conflictOnEmail() {
        when(technicianRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);

        TechnicianCreateRequest req = new TechnicianCreateRequest();
        req.setFirstName("A");
        req.setLastName("B");
        req.setEmail("a@b.com");
        req.setTechnicianType(TechnicianType.FULL_TIME);
        req.setStatus(TechnicianStatus.ACTIVE);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> technicianService.createTechnician(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void patchTechnician_blankFirstNameRejected() {
        Technician existing = Technician.builder()
                .id(3L)
                .firstName("John")
                .lastName("Doe")
                .technicianType(TechnicianType.FULL_TIME)
                .status(TechnicianStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .build();

        when(technicianRepository.findById(3L)).thenReturn(Optional.of(existing));

        TechnicianPatchRequest req = new TechnicianPatchRequest();
        req.setFirstName(" ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> technicianService.patchTechnician(3L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void patchTechnician_updatesStatus() {
        Technician existing = Technician.builder()
                .id(4L)
                .firstName("Jane")
                .lastName("Smith")
                .technicianType(TechnicianType.CONTRACT)
                .status(TechnicianStatus.ACTIVE)
                .build();

        when(technicianRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(technicianRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TechnicianPatchRequest req = new TechnicianPatchRequest();
        req.setStatus(TechnicianStatus.INACTIVE);

        var resp = technicianService.patchTechnician(4L, req);

        assertEquals(TechnicianStatus.INACTIVE, resp.getStatus());
    }
}
