package com.ne.wasac.util;

import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.exception.BusinessRuleException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies whitelisted in-memory sorting for filtered query results.
 */
public final class QuerySort {

    private QuerySort() {
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static <T> List<T> apply(List<T> items, String sortBy, SortDirection sortDir,
                                    Map<String, Comparator<T>> allowed, String defaultField) {
        String field = blankToNull(sortBy);
        if (field == null) {
            field = defaultField;
        }
        Comparator<T> comparator = allowed.get(field);
        if (comparator == null) {
            throw new BusinessRuleException("Invalid sortBy '" + field + "'. Allowed: " + allowed.keySet());
        }
        if (sortDir == SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        return items.stream().sorted(comparator).toList();
    }

    public static Set<String> allowedFields(Map<String, ?> allowed) {
        return allowed.keySet();
    }
}
