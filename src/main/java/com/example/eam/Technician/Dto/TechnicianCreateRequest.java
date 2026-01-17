package com.example.eam.Technician.Dto;

import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TechnicianCreateRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotNull
    private TechnicianType technicianType=TechnicianType.FULL_TIME;

    private String skills;

    private String phoneNumber;

    @Email
    private String email;

    private String address;

    private TechnicianStatus status;

    private LocalDate hireDate;

    private String workShift;

    private String certifications;

    private String notes;
}
