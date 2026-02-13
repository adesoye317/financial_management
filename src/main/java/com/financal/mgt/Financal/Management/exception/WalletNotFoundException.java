package com.financal.mgt.Financal.Management.exception;


public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException() {
        super("Wallet not found");
    }
}
