package org.example.mc.siteintegration.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PlayerMessageUtil {

    private Player player;

    public PlayerMessageUtil(Player player) {
        this.player = player;
    }

    public void toChat(String text) {
        player.sendMessage(giveTextWithColor(text));
    }

    public void toActionBar(String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(giveTextWithColor(text)));
    }

    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(giveTextWithColor(title), giveTextWithColor(subtitle), fadeIn, stay, fadeOut);
    }

    public void clearTitle() {
        player.sendTitle("", "", 0, 0, 0);
    }

    private String giveTextWithColor(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
