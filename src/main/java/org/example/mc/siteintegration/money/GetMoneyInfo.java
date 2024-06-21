package org.example.mc.siteintegration.money;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.example.mc.siteintegration.utils.PlayerError;
import org.example.mc.siteintegration.utils.PlayerMessageUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class GetMoneyInfo implements CommandExecutor {

    private Player player;
    private PlayerMessageUtil messageUtil;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Ця команда може бути активована тільки гравцем");
                return true;
            }

            this.player = (Player) sender;
            this.messageUtil = new PlayerMessageUtil(player);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                fetchGetMoneyCount(httpClient);
            }
            
        } catch (PlayerError e){
            messageUtil.toActionBar(e.getMessage());
            return false;
        } catch (Exception e){
            Bukkit.getLogger().warning(e.getMessage());
            messageUtil.toChat("Помилка в плагіні, будь ласка повідомте адмінів");
            return false;
        }

        return true;
    }

    private void fetchGetMoneyCount(CloseableHttpClient httpClient) throws Exception {
        messageUtil.toActionBar("&eТриває операція");

            String url = "http://localhost:8080/mc/user_inventory/money/" + player.getName();
            HttpGet request = new HttpGet(url);

            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            switch (statusCode) {
                case 200: break;
                case 404: throw new PlayerError("&cГравця з таким ніком не знайдено");
                case 500: throw new PlayerError("&cВнутрішня помилка сервера");
                default: throw new PlayerError("&cОперація не успішна :(");
            }

            String responseBody = EntityUtils.toString(response.getEntity());

            JSONParser parser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);
            int moneyCount = Integer.parseInt(jsonResponse.get("money").toString());

            messageUtil.toActionBar("&aУспішна операція !");

            player.sendMessage("");
            messageUtil.toChat(moneyCalculator(moneyCount));
            player.sendMessage("");
    }

    private String moneyCalculator(int count) {
        if (count <= 64) {
            return ("У вас &b" + count + "⟡ шт. алмазної руди");
        } else {
            int remainder = count % 64;
            double shulkers = count / 1728.0;
            return String.format("У вас &b%d⟡ &7[%d ст. і %d шт. / %.2f шалкера]", count, (count - remainder) / 64, remainder, shulkers);

        }
    }
}