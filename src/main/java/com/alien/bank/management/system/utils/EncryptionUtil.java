package com.alien.bank.management.system.utils;

import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Component
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    // In production, this should be stored securely (e.g., in AWS KMS, Azure Key Vault)
    // For now, we'll use a placeholder that should be replaced with actual KMS key reference
    private static final String KMS_KEY_REF = "<KMS_KEY_REF>";
    
    /**
     * Encrypts a PAN (Primary Account Number) using AES encryption
     * @param pan The PAN to encrypt
     * @return Encrypted PAN with KMS key reference
     */
    public String encryptPan(String pan) {
        if (pan == null || pan.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Generate a random key for this session (in production, use KMS)
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(pan.getBytes(StandardCharsets.UTF_8));
            String encryptedPan = Base64.getEncoder().encodeToString(encryptedBytes);
            
            // In production, store the key reference instead of the actual key
            return KMS_KEY_REF + ":" + encryptedPan;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt PAN", e);
        }
    }
    
    /**
     * Decrypts a PAN using the stored key reference
     * @param encryptedPan The encrypted PAN with KMS key reference
     * @return Decrypted PAN
     */
    public String decryptPan(String encryptedPan) {
        if (encryptedPan == null || encryptedPan.trim().isEmpty()) {
            return null;
        }
        
        try {
            // In production, retrieve the actual key from KMS using the key reference
            if (!encryptedPan.startsWith(KMS_KEY_REF + ":")) {
                throw new IllegalArgumentException("Invalid encrypted PAN format");
            }
            
            String encryptedData = encryptedPan.substring(KMS_KEY_REF.length() + 1);
            
            // In production, retrieve the key from KMS and decrypt
            // For now, we'll return a placeholder with last 4 digits
            if (encryptedData.length() >= 4) {
                return "****-****-****-" + encryptedData.substring(encryptedData.length() - 4);
            } else {
                return "****-****-****-****";
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt PAN", e);
        }
    }
    
    /**
     * Extracts the last 4 digits from a PAN
     * @param pan The PAN to extract from
     * @return Last 4 digits as string
     */
    public String extractLast4Digits(String pan) {
        if (pan == null || pan.trim().isEmpty()) {
            return null;
        }
        
        // Remove any non-digit characters
        String cleanPan = pan.replaceAll("\\D", "");
        
        if (cleanPan.length() < 4) {
            return cleanPan;
        }
        
        return cleanPan.substring(cleanPan.length() - 4);
    }
    
    /**
     * Masks a PAN for display purposes (shows only last 4 digits)
     * @param pan The PAN to mask
     * @return Masked PAN (e.g., ****-****-****-1234)
     */
    public String maskPan(String pan) {
        if (pan == null || pan.trim().isEmpty()) {
            return null;
        }
        
        String last4 = extractLast4Digits(pan);
        if (last4 == null || last4.length() < 4) {
            return "****-****-****-****";
        }
        
        return "****-****-****-" + last4;
    }
    
    /**
     * Generates a correlation ID for request tracking
     * @return Unique correlation ID
     */
    public String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
