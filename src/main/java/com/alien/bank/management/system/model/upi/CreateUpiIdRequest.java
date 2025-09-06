package com.alien.bank.management.system.model.upi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUpiIdRequest {
    
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    @NotBlank(message = "VPA is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$", message = "Invalid VPA format")
    @Size(max = 100, message = "VPA must not exceed 100 characters")
    private String vpa;
    
    @NotBlank(message = "UPI PIN is required")
    @Size(min = 4, max = 6, message = "UPI PIN must be between 4 and 6 digits")
    @Pattern(regexp = "^[0-9]+$", message = "UPI PIN must contain only digits")
    private String upiPin;
}
