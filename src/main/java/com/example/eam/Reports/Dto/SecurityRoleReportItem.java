package com.example.eam.Reports.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.List;

@Data
@Builder
public class SecurityRoleReportItem {
    private String role;
    private Map<String, List<String>> objects;
}
