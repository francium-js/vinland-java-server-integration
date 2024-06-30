package org.example.mc.siteintegration.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class User {
    private String username;
    private String passwordHash;
    private UUID uuid;
    private String ip;
    private Instant mcSession;
    private boolean isAuth;

    public User(String username, String passwordHash, UUID uuid, String ip, Instant mcSession) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.uuid = uuid;
        this.ip = ip;
        this.mcSession = mcSession;
        this.isAuth = false;
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

    public boolean isAuth() {
        return isAuth;
    }

    public void setAuth(boolean auth) {
        isAuth = auth;
    }

    public void setFreePlayer(Player player){
        player.removePotionEffect(PotionEffectType.BLINDNESS); 
        player.setAllowFlight(true);
        player.setInvulnerable(false);
    }
}
