package com.example.eam.CompanyManagement.Service;

import com.example.eam.CompanyManagement.Dto.CompanyCreateRequest;
import com.example.eam.CompanyManagement.Dto.CompanyPatchRequest;
import com.example.eam.CompanyManagement.Dto.CompanyResponse;
import com.example.eam.CompanyManagement.Entity.Company;
import com.example.eam.CompanyManagement.Repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public CompanyResponse create(CompanyCreateRequest req) {
        String companyNumber = requireUniqueCompanyNumber(req.getCompanyNumber(), null);

        Company company = Company.builder()
                .companyLegalName(req.getCompanyLegalName().trim())
                .companyTradeName(req.getCompanyTradeName().trim())
                .companyNumber(companyNumber)
                .address(req.getAddress().trim())
                .city(req.getCity().trim())
                .country(req.getCountry().trim())
                .postalCode(req.getPostalCode().trim())
                .active(req.getActive() == null || req.getActive())
                .build();

        Company saved = companyRepository.save(company);
        return toResponse(saved);
    }

    @Transactional
    public CompanyResponse patch(Long id, CompanyPatchRequest req) {
        Company company = getOrThrowActive(id);

        updateIfNotBlank(req.getCompanyLegalName(), company::setCompanyLegalName);
        updateIfNotBlank(req.getCompanyTradeName(), company::setCompanyTradeName);
        updateIfNotBlank(req.getAddress(), company::setAddress);
        updateIfNotBlank(req.getCity(), company::setCity);
        updateIfNotBlank(req.getCountry(), company::setCountry);
        updateIfNotBlank(req.getPostalCode(), company::setPostalCode);

        if (req.getCompanyNumber() != null) {
            String companyNumber = requireUniqueCompanyNumber(req.getCompanyNumber(), company.getId());
            company.setCompanyNumber(companyNumber);
        }

        if (req.getActive() != null) {
            company.setActive(req.getActive());
        }

        Company saved = companyRepository.save(company);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(Long id) {
        return toResponse(getOrThrowActive(id));
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> list(Pageable pageable, boolean includeInactive) {
        Page<Company> page = includeInactive
                ? companyRepository.findAll(pageable)
                : companyRepository.findByActiveTrue(pageable);

        return page.map(this::toResponse);
    }

    @Transactional
    public void delete(Long id) {
        Company company = getOrThrowActive(id);
        company.setActive(false);
        companyRepository.save(company);
    }

    private Company getOrThrowActive(Long id) {
        return companyRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private void updateIfNotBlank(String value, Consumer<String> setter) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value.trim());
        }
    }

    private String requireUniqueCompanyNumber(String rawCompanyNumber, Long currentId) {
        if (rawCompanyNumber == null || rawCompanyNumber.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyNumber is required");
        }
        String companyNumber = rawCompanyNumber.trim();

        companyRepository.findByCompanyNumberIgnoreCase(companyNumber).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Company number already exists");
            }
        });

        return companyNumber;
    }

    private CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .companyLegalName(company.getCompanyLegalName())
                .companyTradeName(company.getCompanyTradeName())
                .companyNumber(company.getCompanyNumber())
                .address(company.getAddress())
                .city(company.getCity())
                .country(company.getCountry())
                .postalCode(company.getPostalCode())
                .active(company.isActive())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
