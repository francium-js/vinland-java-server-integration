package org.example.mc.siteintegration.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.example.mc.siteintegration.managers.UserManager;
import org.example.mc.siteintegration.utils.PlayerMessageUtil;

public class LoginCommand implements CommandExecutor {

    private final UserManager userManager;
    private PlayerMessageUtil messageUtil;

    public LoginCommand(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        this.messageUtil = new PlayerMessageUtil(player);

        if (args.length != 1) {
            messageUtil.toChat("&cАвторизуйтесь: /login <пароль>");
            return true;
        }

        String password = args[0];

        if (userManager.validatePassword(player.getName(), password)) {
            userManager.updateSession(player.getName(), player.getAddress().getAddress().getHostAddress());

            freePlayer(player);

            messageUtil.toChat("&2Успішна авторизація!");
        } else {
            messageUtil.toChat("&cНеправильний пароль!");
        }

        return true;
    }

    private void freePlayer(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS); 
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.2f);
        player.setAllowFlight(true);
        player.setInvulnerable(false);
    }
}

