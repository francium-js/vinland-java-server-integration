package org.example.mc.siteintegration.money;

import java.util.concurrent.CompletableFuture;

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

          
            fetchGetMoneyCount();
            
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

    private void fetchGetMoneyCount() throws Exception {
        messageUtil.toActionBar("&eТриває операція");

            String url = "http://localhost:8080/mc/user/money/" + player.getName();
            HttpGet request = new HttpGet(url);

            CompletableFuture.runAsync(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) { 

            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();

            String responseBody = EntityUtils.toString(response.getEntity());
            JSONParser parser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

            if(statusCode >= 300 && statusCode < 600) {
                

                String errorMessage = jsonResponse.get("message").toString();

                throw new PlayerError("&c" + errorMessage);
            }

            int moneyCount = Integer.parseInt(jsonResponse.get("money").toString());

            messageUtil.toActionBar("&aУспішна операція !");

            player.sendMessage("");
            messageUtil.toChat(moneyCalculator(moneyCount));
            player.sendMessage("");

        } catch (PlayerError e){
            messageUtil.toChat(e.getMessage());
        } catch(Exception e){
            throw new Error(e);
        }
    });
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