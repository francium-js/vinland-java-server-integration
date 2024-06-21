package org.example.mc.siteintegration.commands;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.example.mc.siteintegration.items.PostItems;
import org.example.mc.siteintegration.items.PullItems;
import org.example.mc.siteintegration.money.GetMoneyInfo;
import org.example.mc.siteintegration.money.PostMoney;
import org.example.mc.siteintegration.money.PullMoney;

public class TradeCommandExecutor implements CommandExecutor {
    private Player player;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ця команда може бути активована тільки гравцем");
            return true;
        }

        player = (Player) sender;

        if (args.length == 0) {
            sendMessageToActionBar("&cВкажіть підкоманду");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, args.length - 1);

        switch (subCommand) {
            case "валюта(інформація)":
                return new GetMoneyInfo().onCommand(sender, command, label, newArgs);
            case "валюта(забрати)":
            case "валюта(закинути)":
                if (newArgs.length < 1) {
                    sendMessageToActionBar("&cВкажіть кількість валюти для передачі [кількість, або стаки 1st - 27st]");
                    return true;
                }

                Integer amount = parseAmount(newArgs[0]);
                if (amount == null) {
                    return true;
                }

                if (subCommand.equals("валюта(забрати)")) {
                    return new PullMoney().onCommand(sender, command, label, new String[]{amount.toString()});
                } else {
                    return new PostMoney().onCommand(sender, command, label, new String[]{amount.toString()});
                }
            case "предмети(забрати)":
                if (newArgs.length == 0) {
                    sendMessageToActionBar("&cВкажіть id квитка");
                    return false;
                }

                try {
                    Integer.parseInt(newArgs[0]);
                } catch (NumberFormatException e) {
                    sendMessageToActionBar("&cНеправильний id квитка");
                    return false;
                }

                return new PullItems().onCommand(sender, command, label, newArgs);
            case "предмети(закинути)":
                return new PostItems().onCommand(sender, command, label, newArgs);
            default:
                sendMessageToActionBar("&cНевідома операція: " + subCommand);
                return false;
        }
    }

    private Integer parseAmount(String input) {
        try {
            if (input.endsWith("st")) {
                input = input.substring(0, input.length() - 2);
                int stacks = Integer.parseInt(input);
                if (stacks < 1 || stacks > 27) {
                    sendMessageToActionBar("&cКількість стаків повинна бути від 1st до 27st");
                    return null;
                }
                return stacks * 64;
            } else {
                int amount = Integer.parseInt(input);
                if (amount < 1 || amount > 1728) {
                    sendMessageToActionBar("&cКількість валюти повинна бути від 1 до 1728");
                    return null;
                }
                return amount;
            }
        } catch (NumberFormatException e) {
            sendMessageToActionBar("&cНеправильний формат кількості валюти [має бути кількість, або стаки 1st - 27st]");
            return null;
        }
    }

    private String giveTextWithColor(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void sendMessageToPlayerChat(String text) {
        player.sendMessage(giveTextWithColor(text));
    }

    private void sendMessageToActionBar(String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(giveTextWithColor(text)));
    }
}