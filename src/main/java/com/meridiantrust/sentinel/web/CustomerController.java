package com.meridiantrust.sentinel.web;

import com.meridiantrust.sentinel.dto.AlertSummaryView;
import com.meridiantrust.sentinel.dto.CustomerDetailView;
import com.meridiantrust.sentinel.dto.TransactionView;
import com.meridiantrust.sentinel.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Customer 360 for analysts: KYC detail, transaction timeline and linked alerts.
 * Full PII (name, national ID) is only unmasked for SUPERVISOR/ADMIN (Business Rule 8).
 */
@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR','ADMIN')")
@Tag(name = "Customers", description = "Customer detail, timeline and alerts")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{customerRef}")
    @Operation(summary = "Customer KYC detail (PII unmasked only for SUPERVISOR/ADMIN)")
    public CustomerDetailView detail(@PathVariable String customerRef, Authentication auth) {
        return customerService.detail(customerRef, canSeePii(auth));
    }

    @GetMapping("/{customerRef}/transactions")
    @Operation(summary = "Full transaction timeline for a customer")
    public List<TransactionView> timeline(@PathVariable String customerRef) {
        return customerService.timeline(customerRef);
    }

    @GetMapping("/{customerRef}/alerts")
    @Operation(summary = "All alerts raised for a customer")
    public List<AlertSummaryView> alerts(@PathVariable String customerRef) {
        return customerService.alerts(customerRef);
    }

    private boolean canSeePii(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR")
                        || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
