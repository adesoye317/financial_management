package com.financal.mgt.Financal.Management.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Insufficient wallet balance");
    }
}
