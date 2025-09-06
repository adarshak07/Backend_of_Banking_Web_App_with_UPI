package com.alien.bank.management.system.exception;

public class InsufficientFundsException extends RuntimeException {
    private final double available;

    public InsufficientFundsException(double available) {
        super("INSUFFICIENT_FUNDS");
        this.available = available;
    }

    public double getAvailable() {
        return available;
    }
}


