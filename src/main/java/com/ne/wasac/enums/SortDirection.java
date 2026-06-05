package com.ne.wasac.enums;

/**
 * Sort order for list and search endpoints.
 */
public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        return SortDirection.valueOf(value.trim().toUpperCase());
    }
}
