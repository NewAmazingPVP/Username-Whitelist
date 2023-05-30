package newamazingpvp.usernamewhitelist;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Plugin(id = "username-whitelist", name = "Username Whitelist", version = "1.0")
public class UsernameWhitelist {
    private final ProxyServer server;
    private final Logger logger;
    private Set<String> whitelist;
    private Map<String, String> playerIPs;

    @Inject
    public UsernameWhitelist(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.whitelist = new HashSet<>();
        this.playerIPs = new HashMap<>();
        loadWhitelist();
        loadPlayerIPs();

        server.getCommandManager().register("whitelist", new WhitelistCommand());
        server.getCommandManager().register("resetIP", new ResetIPCommand());
    }

    private void loadWhitelist() {
        try {
            Path whitelistPath = new File("plugins/username-whitelist/whitelist.txt").toPath();
            Files.createDirectories(whitelistPath.getParent());
            if (!Files.exists(whitelistPath)) {
                Files.createFile(whitelistPath);
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(whitelistPath.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    whitelist.add(line.trim().toLowerCase());
                }
            }
        } catch (Exception e) {
            logger.error("Error loading whitelist.txt", e);
        }
    }

    private void saveWhitelist() {
        try {
            Path whitelistPath = new File("plugins/username-whitelist/whitelist.txt").toPath();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(whitelistPath.toFile()))) {
                for (String username : whitelist) {
                    writer.write(username);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            logger.error("Error saving whitelist.txt", e);
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        loadWhitelist();
        loadPlayerIPs();
        String username = event.getPlayer().getUsername().toLowerCase();
        if (!whitelist.contains(username)) {
            event.setResult(LoginEvent.ComponentResult.denied(Component.text("You are not whitelisted on this server. Join discord.gg/PN8egFY3ap and let the owner know or ask your friends to /whitelist you")));
        } else {
            String currentIP = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();

            if (playerIPs.containsKey(username)) {
                String previousIP = playerIPs.get(username);
                if (!isIPClose(currentIP, previousIP)) {
                    event.setResult(LoginEvent.ComponentResult.denied(Component.text("Your IP has changed significantly. You have been kicked.")));
                    return;
                }
            }
            playerIPs.put(username, currentIP);
            savePlayerIPs();
            loadPlayerIPs();
        }
    }


    private class WhitelistCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length < 2) {
                invocation.source().sendMessage(Component.text("Usage: /whitelist <add|remove> <username>"));
                return;
            }

            String action = args[0].toLowerCase();
            String username = args[1].toLowerCase();

            if ("add".equals(action)) {
                whitelist.add(username);
                saveWhitelist();
                invocation.source().sendMessage(Component.text("Added " + username + " to the whitelist."));
            } else if ("remove".equals(action)) {
                whitelist.remove(username);
                saveWhitelist();
                invocation.source().sendMessage(Component.text("Removed " + username + " from the whitelist."));
            } else {
                invocation.source().sendMessage(Component.text("Usage: /whitelist <add|remove> <username>"));
            }
        }
    }

    private class ResetIPCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length > 1) {
                invocation.source().sendMessage(Component.text("Usage: /resetIP <username>"));
                return;
            }

            String username = args[0].toLowerCase();
            playerIPs.remove(username);
            savePlayerIPs();
            invocation.source().sendMessage(Component.text("IP address for " + username + " has been removed."));
        }
    }

    private void loadPlayerIPs() {
        try {
            Path ipsPath = new File("plugins/username-whitelist/ip.json").toPath();
            Files.createDirectories(ipsPath.getParent());
            if (!Files.exists(ipsPath)) {
                Files.createFile(ipsPath);
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(ipsPath.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":", 2);
                    playerIPs.put(parts[0], parts[1]);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading ip.json", e);
        }
    }

    private void savePlayerIPs() {
        try {
            Path ipsPath = new File("plugins/username-whitelist/ip.json").toPath();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ipsPath.toFile()))) {
                for (Map.Entry<String, String> entry : playerIPs.entrySet()) {
                    writer.write(entry.getKey() + ":" + entry.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            logger.error("Error saving ip.json", e);
        }
    }

    private boolean isIPClose(String ip1, String ip2) {
        String[] parts1 = ip1.split("\\.");
        String[] parts2 = ip2.split("\\.");
        return parts1[0].equals(parts2[0]) && parts1[1].equals(parts2[1]) && parts1[2].equals(parts2[2]);
    }
}
