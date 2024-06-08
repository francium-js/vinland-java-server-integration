package org.example.mc.siteintegration.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TradeTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("валюта(інформація)", "валюта(забрати)", "предмети(забрати)", "валюта(закинути)", "предмети(закинути)");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<>());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("валюта(забрати)") || args[0].equalsIgnoreCase("валюта(закинути)"))) {
            List<String> completions = new ArrayList<>();
            completions.add("[кількість, або стаки 1st - 27st]");

            return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("предмети(забрати)")) {
            List<String> completions = new ArrayList<>();
            completions.add("[id квитка]");

            return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
        }

        return new ArrayList<>();
    }
}
