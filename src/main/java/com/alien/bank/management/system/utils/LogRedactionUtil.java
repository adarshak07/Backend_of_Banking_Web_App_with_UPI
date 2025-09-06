package com.alien.bank.management.system.utils;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class LogRedactionUtil {
    
    // Patterns for sensitive data
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern CVV_PATTERN = Pattern.compile("\\b\\d{3,4}\\b");
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b\\d{16}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\+?[1-9]\\d{1,14}\\b");
    
    /**
     * Redacts sensitive information from a string for logging purposes
     * @param input The string to redact
     * @return Redacted string with sensitive data masked
     */
    public String redactSensitiveData(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String redacted = input;
        
        // Redact card numbers (16 digits with optional separators)
        redacted = CARD_NUMBER_PATTERN.matcher(redacted).replaceAll("****-****-****-****");
        
        // Redact CVV (3-4 digits)
        redacted = CVV_PATTERN.matcher(redacted).replaceAll("***");
        
        // Redact PAN (16 digits)
        redacted = PAN_PATTERN.matcher(redacted).replaceAll("****-****-****-****");
        
        // Redact email addresses (keep domain)
        redacted = EMAIL_PATTERN.matcher(redacted).replaceAll(matchResult -> {
            String email = matchResult.group();
            String[] parts = email.split("@");
            if (parts.length == 2) {
                return "***@" + parts[1];
            }
            return "***@***";
        });
        
        // Redact phone numbers (keep last 4 digits)
        redacted = PHONE_PATTERN.matcher(redacted).replaceAll(matchResult -> {
            String phone = matchResult.group();
            if (phone.length() >= 4) {
                return "***" + phone.substring(phone.length() - 4);
            }
            return "***";
        });
        
        return redacted;
    }
    
    /**
     * Redacts sensitive information from an object for logging
     * @param obj The object to redact
     * @return Redacted string representation
     */
    public String redactSensitiveData(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        return redactSensitiveData(obj.toString());
    }
    
    /**
     * Checks if a string contains sensitive data
     * @param input The string to check
     * @return true if sensitive data is detected
     */
    public boolean containsSensitiveData(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        return CARD_NUMBER_PATTERN.matcher(input).find() ||
               CVV_PATTERN.matcher(input).find() ||
               PAN_PATTERN.matcher(input).find() ||
               EMAIL_PATTERN.matcher(input).find() ||
               PHONE_PATTERN.matcher(input).find();
    }
}
