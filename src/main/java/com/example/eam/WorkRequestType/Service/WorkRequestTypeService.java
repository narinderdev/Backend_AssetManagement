package com.example.eam.WorkRequestType.Service;

import com.example.eam.WorkRequestType.Dto.WorkRequestTypeCreateRequest;
import com.example.eam.WorkRequestType.Dto.WorkRequestTypeResponse;
import com.example.eam.WorkRequestType.Entity.WorkRequestType;
import com.example.eam.WorkRequestType.Repository.WorkRequestTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkRequestTypeService {

    private final WorkRequestTypeRepository workRequestTypeRepository;

    @Transactional(readOnly = true)
    public List<WorkRequestTypeResponse> listTypes() {
        return workRequestTypeRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkRequestType requireByCode(String rawCode) {
        String code = normalizeCode(rawCode);
        return workRequestTypeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Work request type not found: " + code
                ));
    }

    @Transactional
    public WorkRequestType getOrCreateByCode(String rawCode, String rawDescription) {
        String code = normalizeCode(rawCode);
        String description = normalizeDescription(rawDescription);

        return workRequestTypeRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> {
                    try {
                        return workRequestTypeRepository.save(
                                WorkRequestType.builder()
                                        .code(code)
                                        .description(description)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException ex) {
                        return workRequestTypeRepository.findByCodeIgnoreCase(code)
                                .orElseThrow(() -> ex);
                    }
                });
    }

    @Transactional
    public WorkRequestType getOrCreateDefaultExpenseType() {
        return workRequestTypeRepository.findByCodeIgnoreCase("E")
                .orElseGet(() -> workRequestTypeRepository.save(
                        WorkRequestType.builder()
                                .code("E")
                                .description("Expense maintenance (minor)")
                                .build()
                ));
    }

    private String normalizeCode(String rawCode) {
        if (rawCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Work request type code is required");
        }
        String trimmed = rawCode.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Work request type code cannot be blank");
        }
        return trimmed.toUpperCase();
    }

    private String normalizeDescription(String rawDescription) {
        if (rawDescription == null) return null;
        String trimmed = rawDescription.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private WorkRequestTypeResponse toResponse(WorkRequestType entity) {
        if (entity == null) return null;
        return WorkRequestTypeResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .build();
    }
}
