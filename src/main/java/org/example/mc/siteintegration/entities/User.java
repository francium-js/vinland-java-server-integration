package org.example.mc.siteintegration.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {
    private String username;
    private String passwordHash;
    private UUID uuid;
    private String ip;
    private Instant mcSession;

    public User(String username, String passwordHash, UUID uuid, String ip, Instant mcSession) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.uuid = uuid;
        this.ip = ip;
        this.mcSession = mcSession;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getIp() {
        return ip;
    }

    public Instant getMcSession() {
        return mcSession;
    }

    public void setMcSession(Instant mcSession) {
        this.mcSession = mcSession;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
