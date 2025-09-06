package com.alien.bank.management.system.service;

import com.alien.bank.management.system.model.payments.UpiPaymentRequest;
import com.alien.bank.management.system.model.payments.UpiPaymentResponse;

public interface PaymentService {
    UpiPaymentResponse upiPay(UpiPaymentRequest request);
}


