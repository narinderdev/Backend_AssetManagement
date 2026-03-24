package com.example.eam.Common;

import java.util.Optional;

public final class CompanyContextHolder {

    private static final ThreadLocal<Long> COMPANY_ID = new ThreadLocal<>();

    private CompanyContextHolder() {
    }

    public static void setCompanyId(Long companyId) {
        COMPANY_ID.set(companyId);
    }

    public static Optional<Long> getCompanyId() {
        return Optional.ofNullable(COMPANY_ID.get());
    }

    public static void clear() {
        COMPANY_ID.remove();
    }
}
