package org.example.mc.siteintegration.commands;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.example.mc.siteintegration.managers.UserManager;
import org.example.mc.siteintegration.utils.PlayerMessageUtil;

public class RegCommand implements CommandExecutor {

    private final UserManager userManager;
    private PlayerMessageUtil messageUtil;

    public RegCommand(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            this.messageUtil = new PlayerMessageUtil(player);

            if (args.length != 2) {
                return true;
            }

            String password = args[0];
            String repeatPassword = args[1];

            if (!password.equals(repeatPassword)) {
                messageUtil.toChat("&cПаролі не співпадають!");
                return true;
            }

            if (userManager.getUser(player.getName()) != null) {
                messageUtil.toChat("&cВи уже зареєстровані!");
                return true;
            }

            UUID uuid = player.getUniqueId();
            String ip = player.getAddress().getAddress().getHostAddress();

            userManager.registerUser(player.getName(), password, uuid, ip);

            freePlayer(player);
            messageUtil.clearTitle();
            messageUtil.toChat("&2Успішна реєстрація!");
            return true;
        }

        return false;
    }
    private void freePlayer(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS); 
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.2f);
        player.setAllowFlight(true);
        player.setInvulnerable(false);
    }
}
