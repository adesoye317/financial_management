package com.financal.mgt.Financal.Management.exception;

public class DuplicateWalletException extends RuntimeException {
    public DuplicateWalletException() {
        super("Wallet already exists for this user");
    }
}
