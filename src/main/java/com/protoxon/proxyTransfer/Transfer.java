package com.protoxon.proxyTransfer;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Transfer {
    public static void transferPlayer(Player player, String host, int port) {
        InetSocketAddress targetAddress = new InetSocketAddress(host, port);
        attachCookie(player, "slimelabs.net");
        player.transferToHost(targetAddress);
    }

    /**
     * Attaches a cookie to the players client so they can be sent back
     * @param player the player
     * @param data the server domain name or ip address
     */
    public static void attachCookie(Player player, String data) {
        Key cookieKey = Key.key("protoxon", "proxytransfer");
        byte[] cookieData = data.getBytes(StandardCharsets.UTF_8);
        player.storeCookie(cookieKey, cookieData);
    }
}
