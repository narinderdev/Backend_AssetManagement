package com.example.eam.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum InsurancePolicyType {
    LIABILITY("Liability"),
    PROPERTY("Property"),
    COMPREHENSIVE("Comprehensive"),
    FIRE("Fire"),
    EQUIPMENT_BREAKDOWN("Equipment Breakdown"),
    WELL_CONTROL("Well Control"),
    ENVIRONMENTAL_POLLUTION("Environmental/Pollution"),
    PIPELINE("Pipeline"),
    OFFSHORE_MARINE("Offshore/Marine"),
    CARGO("Cargo"),
    BUSINESS_INTERRUPTION("Business Interruption");

    private final String label;

    InsurancePolicyType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static InsurancePolicyType fromValue(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(v -> v.label.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown policy type: " + value));
    }
}
