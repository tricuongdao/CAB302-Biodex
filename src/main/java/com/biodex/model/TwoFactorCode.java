package com.biodex.model;

/**
 * A one-time code issued for two-factor sign in. Mirrors the {@code two_factor_codes} table.
 * Only the hash of the code is ever stored.
 */
public class TwoFactorCode {

    private int codeId;
    private int userId;
    private String codeHash;
    private String expiresAt;
    private boolean used;
    private String createdAt;

    public TwoFactorCode() {
    }

    public TwoFactorCode(int userId, String codeHash, String expiresAt) {
        this.userId = userId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public int getCodeId() {
        return codeId;
    }

    public void setCodeId(int codeId) {
        this.codeId = codeId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
