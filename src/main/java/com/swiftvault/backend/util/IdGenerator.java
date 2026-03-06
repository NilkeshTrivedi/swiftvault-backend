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
    // ═══════════════════════════════════════════════════════════════════════════
// ADD THESE METHODS to your existing IdGenerator.java
// Path: src/main/java/com/swiftvault/backend/util/IdGenerator.java
// ═══════════════════════════════════════════════════════════════════════════
//
// Paste these static methods inside the IdGenerator class body.
// Each ID has a meaningful prefix for easy debugging in logs/DB queries.
//
// ═══════════════════════════════════════════════════════════════════════════

    /** e.g. ALERT-7F3K2A9B1C */
    public static String alertId() {
        return "ALERT-" + randomAlphaNum(10).toUpperCase();
    }

    /** e.g. RULE-4X8P1Q */
    public static String ruleId() {
        return "RULE-" + randomAlphaNum(6).toUpperCase();
    }

    /** e.g. REF-SWIFT7823 → actual code generated in ReferralService */
    public static String referralId() {
        return "REF-" + randomAlphaNum(8).toUpperCase();
    }

    /** Short 6-char alphanumeric — used as fallback in ReferralService */
    public static String shortId() {
        return randomAlphaNum(6).toUpperCase();
    }

    // ── Private helper (add only if not already present) ──────────────────
    private static String randomAlphaNum(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < length; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
        return sb.toString();
    }

    private static String uuid(int len) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, len).toUpperCase();
    }
}
