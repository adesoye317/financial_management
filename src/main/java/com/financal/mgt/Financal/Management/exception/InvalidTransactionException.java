package com.financal.mgt.Financal.Management.exception;


public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
