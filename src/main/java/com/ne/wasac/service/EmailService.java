package com.ne.wasac.service;

import com.ne.wasac.enums.RoleName;
import com.ne.wasac.model.Bill;
import com.ne.wasac.model.Customer;
import com.ne.wasac.model.Notification;
import com.ne.wasac.model.Payment;
import com.ne.wasac.repository.AppUserRepository;
import com.ne.wasac.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

/**
 * Central email dispatcher for account, billing, and payment events.
 * Failures are logged and never crash the calling transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final BillReceiptPdfService billReceiptPdfService;
    private final AppUserRepository appUserRepository;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    /** Sends credentials after admin creates a customer account. */
    public void sendCustomerCredentials(String to, String fullName, String email, String temporaryPassword) {
        String html = """
                <p>Dear %s,</p>
                <p>Your WASAC customer account has been created.</p>
                <ul><li>Username: %s</li><li>Temporary password: %s</li></ul>
                <p>Change your password on first login.</p>
                """.formatted(fullName, email, temporaryPassword);
        sendSafe(to, "WASAC Customer Account Created", html, null);
    }

    /** Sends staff credentials after admin creates an Operator or Finance account. */
    public void sendStaffCredentials(String to, String fullName, String email, String temporaryPassword, RoleName role) {
        String roleLabel = toRoleLabel(role);
        String html = """
                <p>Dear %s,</p>
                <p>You have been registered as a <b>%s</b> on WASAC.</p>
                <ul><li>Username: %s</li><li>Temporary password: %s</li></ul>
                <p>Change your password on first login.</p>
                """.formatted(fullName, roleLabel, email, temporaryPassword);
        sendSafe(to, "WASAC Staff Account Created", html, null);
    }

    /** Notifies user they were upgraded to Operator or Finance (not sent for Customer). */
    public void sendRoleUpgradeNotification(String to, String fullName, RoleName role) {
        String roleLabel = toRoleLabel(role);
        String html = """
                <p>Dear %s,</p>
                <p>You have been upgraded to <b>%s</b> on WASAC.</p>
                """.formatted(fullName, roleLabel);
        sendSafe(to, "WASAC Role Upgrade", html, null);
    }

    private String toRoleLabel(RoleName role) {
        return switch (role) {
            case ROLE_OPERATOR -> "Operator";
            case ROLE_FINANCE -> "Finance";
            case ROLE_CUSTOMER -> "Customer";
            case ROLE_ADMIN -> "Administrator";
        };
    }

    /** Reminds staff to change password on first login. */
    public void sendPasswordChangeReminder(String to, String fullName) {
        String html = "<p>Dear %s,</p><p>Please change your temporary password before using the system.</p>"
                .formatted(fullName);
        sendSafe(to, "WASAC Password Change Required", html, null);
    }

    /** Bill generated notification to customer. */
    public void sendBillGenerated(Customer customer, Bill bill, Notification notification) {
        String to = resolveCustomerEmail(customer);
        if (to == null) {
            log.warn("No email for customer {} — bill generated email skipped", customer.getId());
            return;
        }
        String html = """
                <p>Dear %s,</p>
                <p>Your %s utility bill of <b>%s FRW</b> for %d/%d has been successfully processed.</p>
                <p>Bill reference: <b>%s</b>. Due date: <b>%s</b>.</p>
                """.formatted(customer.getFullName(), bill.getMeter().getMeterType(),
                bill.getTotalAmount(), bill.getBillingMonth(), bill.getBillingYear(),
                bill.getBillReference(), bill.getDueDate());
        boolean sent = sendSafe(to, "WASAC Bill Ready", html, notification);
        markNotificationEmailSent(sent, notification, customer.getId(), "BILL_GENERATED", bill.getId());
    }

    /** Bill approved by finance. */
    public void sendBillApproved(Customer customer, Bill bill) {
        String to = resolveCustomerEmail(customer);
        if (to == null) {
            log.warn("No email for customer {} — bill approved email skipped", customer.getId());
            return;
        }
        String html = """
                <p>Dear %s,</p>
                <p>Your %s utility bill <b>%s</b> of <b>%s FRW</b> has been reviewed and approved.</p>
                <p>You may now make payment before the due date (<b>%s</b>).</p>
                """.formatted(customer.getFullName(), bill.getMeter().getMeterType(),
                bill.getBillReference(), bill.getTotalAmount(), bill.getDueDate());
        sendSafe(to, "WASAC Bill Approved", html, null);
    }

    /** Payment received with remaining balance. */
    public void sendPaymentReceived(Customer customer, Bill bill, BigDecimal amountPaid) {
        String html = """
                <p>Dear %s,</p>
                <p>We received <b>%s FRW</b> for bill %s. Remaining balance: <b>%s FRW</b>.</p>
                """.formatted(customer.getFullName(), amountPaid, bill.getBillReference(), bill.getOutstandingBalance());
        sendSafe(customer.getEmail(), "WASAC Payment Received", html, null);
    }

    /** Bill fully paid confirmation with PDF receipt attached. */
    public void sendBillFullyPaid(Customer customer, Bill bill, Payment payment) {
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            log.warn("No email for customer {} — bill paid notification skipped", customer.getId());
            return;
        }
        String html = """
                <p>Dear %s,</p>
                <p>Your bill <b>%s</b> is fully paid. Balance: <b>0 FRW</b>.</p>
                <p>Please find your payment receipt attached as a PDF.</p>
                """.formatted(customer.getFullName(), bill.getBillReference());
        EmailAttachment attachment = buildReceiptAttachment(bill, payment);
        boolean sent = sendSafe(customer.getEmail(), "WASAC Bill Paid", html, null, attachment);
        markNotificationEmailSent(sent, null, customer.getId(), "BILL_FULLY_PAID", bill.getId());
    }

    private EmailAttachment buildReceiptAttachment(Bill bill, Payment payment) {
        try {
            byte[] pdf = billReceiptPdfService.generate(bill, payment);
            return new EmailAttachment("WASAC-Receipt-" + bill.getBillReference() + ".pdf", pdf);
        } catch (Exception ex) {
            log.warn("Could not generate receipt PDF for bill {}: {}", bill.getId(), ex.getMessage());
            return null;
        }
    }

    /** Late payment warning after penalty applied. */
    public void sendLatePaymentWarning(Customer customer, Bill bill) {
        String html = "<p>Dear %s,</p><p>Bill %s is overdue. A late penalty has been applied.</p>"
                .formatted(customer.getFullName(), bill.getBillReference());
        sendSafe(customer.getEmail(), "WASAC Late Payment Warning", html, null);
    }

    /** Sends OTP to recover a forgotten password. */
    public boolean sendPasswordResetOtp(String to, String fullName, String otp, int expiryMinutes) {
        String html = """
                <p>Dear %s,</p>
                <p>Your WASAC password recovery code is:</p>
                <h2 style="letter-spacing:4px;">%s</h2>
                <p>This code expires in <b>%d minutes</b>. Use it to set a new password.</p>
                <p>If you did not request this, ignore this email.</p>
                """.formatted(fullName, otp, expiryMinutes);
        return sendSafe(to, "WASAC Password Recovery Code", html, null);
    }

    /** Sends registration OTP to activate a new account. */
    public boolean sendRegistrationOtp(String to, String fullName, String otp, int expiryMinutes) {
        String html = """
                <p>Dear %s,</p>
                <p>Your WASAC verification code is:</p>
                <h2 style="letter-spacing:4px;">%s</h2>
                <p>This code expires in <b>%d minutes</b>. Enter it to activate your account.</p>
                <p>If you did not register, ignore this email.</p>
                """.formatted(fullName, otp, expiryMinutes);
        return sendSafe(to, "WASAC Email Verification Code", html, null);
    }

    /** Reminds customer before bill due date. */
    public void sendPaymentReminder(Customer customer, Bill bill, long daysUntilDue) {
        String html = """
                <p>Dear %s,</p>
                <p>Your bill <b>%s</b> of <b>%s FRW</b> is due in <b>%d day(s)</b> (%s).</p>
                <p>Outstanding balance: <b>%s FRW</b>.</p>
                """.formatted(customer.getFullName(), bill.getBillReference(), bill.getTotalAmount(),
                daysUntilDue, bill.getDueDate(), bill.getOutstandingBalance());
        sendSafe(customer.getEmail(), "WASAC Payment Reminder", html, null);
    }

    /** Warns before meter disconnection. */
    public void sendDisconnectionWarning(Customer customer, String meterNumber) {
        String html = "<p>Dear %s,</p><p>Meter %s will be disconnected due to extended non-payment.</p>"
                .formatted(customer.getFullName(), meterNumber);
        sendSafe(customer.getEmail(), "WASAC Disconnection Warning", html, null);
    }

    /** Links email delivery to the DB-trigger notification row when present. */
    private void markNotificationEmailSent(boolean sent, Notification notification,
                                           Long customerId, String eventType, Long referenceId) {
        if (!sent) {
            return;
        }
        Notification target = notification != null ? notification
                : notificationRepository.findByCustomer_IdAndEventTypeAndReferenceId(
                        customerId, eventType, referenceId).orElse(null);
        if (target != null) {
            target.setEmailSent(true);
            notificationRepository.save(target);
        }
    }

    private boolean sendSafe(String to, String subject, String htmlBody, Notification notification) {
        return sendSafe(to, subject, htmlBody, notification, null);
    }

    /**
     * Sends HTML email; returns true on success.
     * Catches all exceptions so callers are not interrupted.
     */
    private boolean sendSafe(String to, String subject, String htmlBody, Notification notification,
                             EmailAttachment attachment) {
        if (!mailEnabled) {
            log.info("Mail disabled — would send to {} subject {}{}", to, subject,
                    attachment == null ? "" : " with attachment " + attachment.filename());
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (attachment != null) {
                helper.addAttachment(attachment.filename(),
                        new ByteArrayResource(attachment.content()), "application/pdf");
            }
            mailSender.send(message);
            log.info("Email sent to {}{}", to, attachment == null ? "" : " with attachment " + attachment.filename());
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
            return false;
        }
    }

    private record EmailAttachment(String filename, byte[] content) {}

    /** Uses customer profile email, falling back to linked login account email. */
    private String resolveCustomerEmail(Customer customer) {
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            return customer.getEmail().trim();
        }
        return appUserRepository.findFirstByCustomer_Id(customer.getId())
                .map(user -> user.getEmail())
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .orElse(null);
    }
}
