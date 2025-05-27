package com.protoxon.proxyTransfer;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.protoxon.proxyTransfer.ProxyTransfer.logger;
import static com.protoxon.proxyTransfer.ProxyTransfer.proxy;

public class TransferCommand {
    public static void register() {
        CommandManager commandManager = proxy.getCommandManager();
        // Create the root command
        LiteralArgumentBuilder<CommandSource> root = LiteralArgumentBuilder.literal("transfer");

        root.executes(TransferCommand::handleRootCommand); // Handle the execution of /sls when no arguments are given

        // Register subcommands
        root.then(transfer()); // TRANSFER

        // Create the Brigadier command
        BrigadierCommand brigadierCommand = new BrigadierCommand(root);

        // Create command metadata
        CommandMeta commandMeta = commandManager.metaBuilder("transfer")
                .plugin(ProxyTransfer.plugin)
                .build();

        // Register the command
        commandManager.register(commandMeta, brigadierCommand);
    }

    private static int handleRootCommand(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        source.sendMessage(Component.text("Invalid Command Usage!"));
        if(source.hasPermission("proxytransfer.others")) {
            source.sendMessage(Component.text("/transfer <host> [player|local|all]"));
            return 0;
        }
        source.sendMessage(Component.text("/transfer <host>"));
        return 0;
    }

    public static RequiredArgumentBuilder<CommandSource, String> transfer() {
        return RequiredArgumentBuilder.<CommandSource, String>argument("target", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    String input = builder.getRemaining().toLowerCase();
                    // Show suggestions only if the user is typing the second argument
                    if (input.contains(" ")) {
                        builder.suggest("all");
                        builder.suggest("local");
                        proxy.getAllPlayers().forEach(player -> builder.suggest(player.getUsername()));
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    CommandSource source = context.getSource();

                    if (!(source instanceof Player executor)) {
                        source.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
                        return 0;
                    }

                    String input = StringArgumentType.getString(context, "target").trim();
                    String[] parts = input.split("\\s+"); // Split by space(s)

                    if (parts.length == 0) {
                        source.sendMessage(Component.text("Invalid usage. Please provide a host.", NamedTextColor.RED));
                        return 0;
                    }

                    String hostPort = parts[0];
                    String[] hostParts = hostPort.split(":", 2);
                    String host = hostParts[0];
                    int port = (hostParts.length > 1) ? getPort(hostParts[1]) : 25565;

                    if (port == -1) {
                        source.sendMessage(Component.text("Error: Invalid port.", NamedTextColor.RED));
                        return 0;
                    }

                    // Handle optional second argument
                    String playerContext = (parts.length > 1) ? parts[1] : null;

                    if (playerContext == null) {
                        Transfer.transferPlayer(executor, host, port);
                        return 1;
                    }

                    if (!source.hasPermission("proxytransfer.others")) {
                        source.sendMessage(Component.text("You don't have permission to transfer others. You must have the \"proxytransfer.others\" permission.", NamedTextColor.RED));
                        return 0;
                    }

                    switch (playerContext.toLowerCase()) {
                        case "all":
                            for (Player p : proxy.getAllPlayers()) {
                                Transfer.transferPlayer(p, host, port);
                            }
                            return 1;

                        case "local":
                            for (Player p : getPlayersOnSameServer(source)) {
                                Transfer.transferPlayer(p, host, port);
                            }
                            return 1;

                        default:
                            Player target = getPlayer(playerContext);
                            if (target == null) {
                                source.sendMessage(Component.text("Player " + playerContext + " not found.", NamedTextColor.RED));
                                return 0;
                            }
                            Transfer.transferPlayer(target, host, port);
                            return 1;
                    }
                });
    }

    public static int getPort(String context) {
        String[] parts = context.split(":", 2); // limit to 2 parts
        if (parts.length < 2) return 25565; // no port specified
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return -1; // invalid port
        }
    }

    public static Player getPlayer(String username) {
        Optional<Player> player = proxy.getPlayer(username);
        return player.orElse(null);
    }

    public static Collection<Player> getPlayersOnSameServer(CommandSource source) {
        if (!(source instanceof Player player)) {
            logger.error("You must be a player to use the local argument!");
            return Collections.emptyList();
        }
        return player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServer().getPlayersConnected())
                .orElse(Collections.emptyList());
    }
}