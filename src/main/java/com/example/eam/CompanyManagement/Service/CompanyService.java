package com.example.eam.CompanyManagement.Service;

import com.example.eam.CompanyManagement.Dto.CompanyCreateRequest;
import com.example.eam.CompanyManagement.Dto.CompanyPatchRequest;
import com.example.eam.CompanyManagement.Dto.CompanyResponse;
import com.example.eam.CompanyManagement.Entity.Company;
import com.example.eam.CompanyManagement.Repository.CompanyRepository;
import com.example.eam.Roles.Entity.AppPermission;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.AppPermissionRepository;
import com.example.eam.Roles.Repository.RoleRepository;
import com.example.eam.User.entity.UserCompany;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.UserCompanyRepository;
import com.example.eam.User.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UsersRepository usersRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final RoleRepository roleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private static final Set<String> COMPANY_SORT_FIELDS = Set.of(
            "id",
            "companyLegalName",
            "companyTradeName",
            "companyNumber",
            "address",
            "city",
            "country",
            "postalCode",
            "active",
            "createdAt",
            "updatedAt"
    );

    @Transactional
    public CompanyResponse create(CompanyCreateRequest req) {
        Users user = usersRepository.findByIdAndDeletedFalse(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String companyNumber = req.getCompanyNumber().trim();

        Company saved = companyRepository.findByCompanyNumberIgnoreCase(companyNumber)
                .map(existing -> {
                    if (existing.isActive()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Company number already exists");
                    }

                    existing.setCompanyLegalName(req.getCompanyLegalName().trim());
                    existing.setCompanyTradeName(req.getCompanyTradeName().trim());
                    existing.setCompanyNumber(companyNumber);
                    existing.setAddress(req.getAddress().trim());
                    existing.setCity(req.getCity().trim());
                    existing.setCountry(req.getCountry().trim());
                    existing.setPostalCode(req.getPostalCode().trim());
                    existing.setActive(req.getActive() == null || req.getActive());
                    return companyRepository.save(existing);
                })
                .orElseGet(() -> {
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
                    return companyRepository.save(company);
                });

        ensureUserCompanyMapping(user, saved);
        ensureCompanyAdminRole(saved.getId());

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
        String currentUserEmail = resolveCurrentUserEmailOrThrow();
        return userCompanyRepository.findActiveCompanyByUserEmailAndCompanyId(currentUserEmail, id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> list(Pageable pageable, boolean includeInactive) {
        String currentUserEmail = resolveCurrentUserEmailOrThrow();
        Pageable effectivePageable = normalizeCompanySort(pageable);
        return userCompanyRepository.findCompaniesByUserEmail(currentUserEmail, includeInactive, effectivePageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> listCompaniesByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must be greater than 0");
        }

        Users targetUser = usersRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String currentUserEmail = resolveCurrentUserEmailOrThrow();
        Users currentUser = usersRepository.findByEmailAndDeletedFalse(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        if (!isCurrentUserAdmin() && !Objects.equals(currentUser.getId(), targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this user's companies");
        }

        return userCompanyRepository.findByUser_IdAndCompany_ActiveTrue(targetUser.getId()).stream()
                .map(UserCompany::getCompany)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(java.util.Comparator.comparing(Company::getId, java.util.Comparator.nullsLast(Long::compareTo)))
                .map(this::toResponse)
                .toList();
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

    private void ensureUserCompanyMapping(Users user, Company company) {
        if (!userCompanyRepository.existsByUser_IdAndCompany_Id(user.getId(), company.getId())) {
            userCompanyRepository.save(UserCompany.builder()
                    .user(user)
                    .company(company)
                    .build());
        }
    }

    private void ensureCompanyAdminRole(Long companyId) {
        List<AppPermission> permissions = appPermissionRepository.findByActiveTrueOrderByModuleAscSortOrderAsc();

        Role companyAdmin = roleRepository.findFirstByNameIgnoreCaseAndCompanyIdOrderByIdAsc("Admin", companyId)
                .orElseGet(() -> Role.builder()
                        .companyId(companyId)
                        .name("Admin")
                        .description("Full access")
                        .active(true)
                        .build());

        companyAdmin.setPermissions(new HashSet<>(permissions));
        roleRepository.save(companyAdmin);
    }

    private String resolveCurrentUserEmailOrThrow() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
            Object principal = auth.getPrincipal();
            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
            String email = String.valueOf(principal).trim();
            if (email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
            return email;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private Pageable normalizeCompanySort(Pageable pageable) {
        if (pageable == null || pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> mappedOrders = pageable.getSort().stream()
                .map(this::mapCompanySortOrder)
                .toList();

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(mappedOrders));
    }

    private Sort.Order mapCompanySortOrder(Sort.Order order) {
        String requestedProperty = order.getProperty();
        String normalizedProperty = requestedProperty == null ? "" : requestedProperty.trim();

        if (normalizedProperty.startsWith("company.")) {
            normalizedProperty = normalizedProperty.substring("company.".length());
        }

        String mappedProperty = COMPANY_SORT_FIELDS.contains(normalizedProperty)
                ? "company." + normalizedProperty
                : "company.companyLegalName";

        return new Sort.Order(
                order.getDirection(),
                mappedProperty,
                order.getNullHandling()
        );
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }

        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority != null && authority.getAuthority() != null
                    && "ROLE_Admin".equalsIgnoreCase(authority.getAuthority().trim())) {
                return true;
            }
        }
        return false;
    }
}
