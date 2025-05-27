package com.protoxon.proxyTransfer;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

import static com.protoxon.proxyTransfer.utils.Color.*;

@Plugin(id = "proxytransfer", name = "ProxyTransfer", version = "1.0", authors = {"protoxon"})
public class ProxyTransfer {

    private static final ChannelIdentifier TRANSFER_CHANNEL = MinecraftChannelIdentifier.create("proxytransfer", "forward");

    @Inject
    public static Logger logger;
    public static ProxyServer proxy;
    public static ProxyTransfer plugin;

    @Inject //injects the proxy server and logger into the plugin class
    public ProxyTransfer(ProxyServer proxy, Logger logger) {
        ProxyTransfer.logger = logger;
        ProxyTransfer.proxy = proxy;
        plugin = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        System.out.println(startMessage());
        TransferCommand.register();
        // Register the slimelabs network channel
        proxy.getChannelRegistrar().register(TRANSFER_CHANNEL);
        // Register the plugin message listener
        proxy.getEventManager().register(this, new TransferMessageListener());
    }

    // Start message displayed when the plugin is enabled
    public String startMessage() {
        return GREEN + "[ProxyTransfer] " + YELLOW + "v" + getVersion() + RESET + " by " + MAGENTA + getAuthors() + RESET;
    }

    // Returns the plugin version listed in the plugin annotation or unknown if not specified
    public String getVersion() {
        Optional<PluginContainer> pluginContainer = proxy.getPluginManager().getPlugin("proxytransfer");
        if (pluginContainer.isPresent()) {
            PluginDescription description = pluginContainer.get().getDescription();
            return description.getVersion().orElse("unknown");
        }
        return "unknown";
    }

    // Returns the plugin authors listed in the plugin annotation or unknown if not specified
    public String getAuthors() {
        return proxy.getPluginManager().getPlugin("proxytransfer")
                .map(p -> String.join(", ", p.getDescription().getAuthors()))
                .orElse("unknown");
    }

    public static class TransferMessageListener {
        @Subscribe
        public void onPluginMessage(PluginMessageEvent event) {
            if (!event.getIdentifier().equals(TRANSFER_CHANNEL)) {
                return;
            }
            String resultString = new String(event.getData());
            String[] parts = resultString.split(";");
            Player player = proxy.getPlayer(parts[0]).get();
            String host = parts[1];
            int port = Integer.parseInt(parts[2]);
            Transfer.transferPlayer(player, host, port);
        }
    }
}
