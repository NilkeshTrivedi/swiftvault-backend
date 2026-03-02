package com.swiftvault.backend.util;

import java.util.UUID;

public class IdGenerator {

    private IdGenerator() {}

    public static String userId()        { return "USR-" + uuid(8); }
    public static String accountNumber() { return "ACC-" + uuid(8); }
    public static String transactionId() { return "TXN-" + uuid(12); }
    public static String fdId()          { return "FD-"  + uuid(10); }
    public static String rdId()          { return "RD-"  + uuid(10); }
    public static String loanId()        { return "LN-"  + uuid(10); }
    public static String cardId()        { return "CRD-" + uuid(10); }

    private static String uuid(int len) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, len).toUpperCase();
    }
}
