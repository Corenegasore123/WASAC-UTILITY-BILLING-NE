package com.ne.wasac.security;

import com.ne.wasac.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("accessGuard")
@RequiredArgsConstructor
public class AccessGuard {

    private final BillRepository billRepository;

    public boolean isOwnCustomer(Long customerId) {
        SecurityUser user = SecurityUtils.currentUser();
        return user.getUser().getCustomer() != null
                && user.getUser().getCustomer().getId().equals(customerId);
    }

    public boolean isOwnBill(Long billId) {
        SecurityUser user = SecurityUtils.currentUser();
        if (user.getUser().getCustomer() == null) {
            return false;
        }
        return billRepository.findById(billId)
                .map(b -> b.getCustomer().getId().equals(user.getUser().getCustomer().getId()))
                .orElse(false);
    }
}
