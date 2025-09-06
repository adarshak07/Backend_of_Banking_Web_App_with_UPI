package com.alien.bank.management.system.exception;

public class InsufficientCoinsException extends RuntimeException {
    private final int available;

    public InsufficientCoinsException(int available) {
        super("INSUFFICIENT_COINS");
        this.available = available;
    }

    public int getAvailable() {
        return available;
    }
}


