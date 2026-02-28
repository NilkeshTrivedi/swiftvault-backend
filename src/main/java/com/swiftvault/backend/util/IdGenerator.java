package com.swiftvault.backend.util;

import java.util.UUID;

public class IdGenerator {

    private IdGenerator() {} // prevent instantiation

    public static String userId() {
        return "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String accountNumber() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String transactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}