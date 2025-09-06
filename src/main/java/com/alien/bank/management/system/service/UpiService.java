package com.alien.bank.management.system.service;

import com.alien.bank.management.system.entity.UpiRequest;
import com.alien.bank.management.system.entity.UpiVpa;
import com.alien.bank.management.system.model.upi.CreateUpiIdRequest;
import com.alien.bank.management.system.model.upi.CreateUpiIdResponse;

import java.util.List;

public interface UpiService {
    UpiVpa createVpa(Long accountId);
    List<UpiVpa> getMyVpas();
    void setDefaultVpa(String vpa);

    // New UPI ID management methods
    CreateUpiIdResponse createUpiId(CreateUpiIdRequest request);
    void deleteUpiId(String vpa);
    UpiVpa getUpiIdByVpa(String vpa);

    Long sendMoney(String fromVpa, String toVpa, double amount, String note, String pin);

    UpiRequest createCollectRequest(String payerVpa, String payeeVpa, double amount, String reason);
    List<UpiRequest> getPendingRequests(String vpa);
    Long approveRequest(Long requestId, String pin);
    void rejectRequest(Long requestId);

    void setOrChangeUpiPin(String newPin);

    // History via VPA
    java.util.List<com.alien.bank.management.system.entity.Transaction> getTransactionsByVpa(String vpa);
    
    // Search VPAs
    List<UpiVpa> searchVpas(String query);
}


