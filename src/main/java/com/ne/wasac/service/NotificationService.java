package com.ne.wasac.service;

import com.ne.wasac.dto.NotificationResponse;
import com.ne.wasac.enums.SortDirection;
import com.ne.wasac.repository.NotificationRepository;
import com.ne.wasac.util.QuerySort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Map<String, Comparator<NotificationResponse>> SORT_FIELDS = Map.of(
            "id", Comparator.comparing(NotificationResponse::getId),
            "createdAt", Comparator.comparing(NotificationResponse::getCreatedAt),
            "eventType", Comparator.comparing(NotificationResponse::getEventType, String.CASE_INSENSITIVE_ORDER),
            "customerId", Comparator.comparing(NotificationResponse::getCustomerId),
            "emailSent", Comparator.comparing(NotificationResponse::isEmailSent));

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> findByCustomer(Long customerId, String sortBy, SortDirection sortDir) {
        return sort(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(DtoMapper::toNotificationResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> search(Long customerId, String eventType, Boolean emailSent, String q,
                                             String sortBy, SortDirection sortDir) {
        return sort(notificationRepository.search(customerId, QuerySort.blankToNull(eventType),
                        emailSent, QuerySort.blankToNull(q))
                .stream().map(DtoMapper::toNotificationResponse).toList(), sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> filterAndSort(Long customerId, String eventType, Boolean emailSent, String q,
                                                    String sortBy, SortDirection sortDir) {
        if (customerId != null || QuerySort.blankToNull(eventType) != null || emailSent != null
                || QuerySort.blankToNull(q) != null) {
            return search(customerId, eventType, emailSent, q, sortBy, sortDir);
        }
        return sort(notificationRepository.findAll().stream().map(DtoMapper::toNotificationResponse).toList(),
                sortBy, sortDir);
    }

    private List<NotificationResponse> sort(List<NotificationResponse> items, String sortBy, SortDirection sortDir) {
        return QuerySort.apply(items, sortBy, sortDir == null ? SortDirection.DESC : sortDir,
                SORT_FIELDS, "createdAt");
    }
}
