package dev.mcsm.commands;

import dev.mcsm.utils.Status;
import dev.mcsm.utils.StatusStorage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

public class QueuedMessageHandler {
    public static final NavigableMap<Long, QueuedMessage> queuedMessages = new ConcurrentSkipListMap<>();
    public static void handleStatusCommand(@NotNull MessageChannelUnion channel, String ip, int port, boolean bedrock, String displayName) {
        channel.sendMessageEmbeds(buildEmbed(displayName, bedrock, port, ip).build()).queue(sent -> {
            long updateTime = System.currentTimeMillis() + 120000; // current + 2 minutes
            queuedMessages.put(updateTime, new QueuedMessage(ip, port, bedrock, displayName, sent));
        });
    }

    public record QueuedMessage(String ip, int port, boolean bedrock, String displayName, Message message) implements Serializable { }

    public static void startUpdating() {
        new Thread(() -> {
            long lastUpdateTime = 0;
            while (true) {
                if (!queuedMessages.isEmpty() && System.currentTimeMillis() >= queuedMessages.firstKey() && System.currentTimeMillis() - lastUpdateTime >= 2000) {
                    QueuedMessage queuedMessage = queuedMessages.pollFirstEntry().getValue();
                    update(queuedMessage);
                    lastUpdateTime = System.currentTimeMillis();
                    System.out.println("Updated at " + lastUpdateTime);

                    try {
                        Thread.sleep(2000); // 1 second (prevent busy waiting)
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    private static void update(@NotNull QueuedMessage queuedMessage) {
        EmbedBuilder builder = buildEmbed(queuedMessage.displayName(), queuedMessage.bedrock(), queuedMessage.port(), queuedMessage.ip());
        queuedMessage.message().editMessageEmbeds(builder.build()).queue(success -> {
            long updateTime = System.currentTimeMillis() + 120000; // current + 2 minutes
            queuedMessages.put(updateTime, queuedMessage);
        }, throwable -> {
            System.out.println("Error updating message (probably deleted): " + throwable.getMessage());
        });
    }

    @NotNull
    private static EmbedBuilder buildEmbed(String displayName, boolean bedrock, int port, String ip) {
        EmbedBuilder builder = new EmbedBuilder();
        long unix = System.currentTimeMillis() / 1000 + 120; // current + 2 minutes
        StatusStorage status;
        if (bedrock) {
            status = Status.bedrockStatus(ip, port);
        } else {
            status = Status.javaStatus(ip, port);
        }

        if (status.getVersionNum().equals("Error")) {
            return builder.setTitle("Error getting status for \"" + displayName + "\"").setColor(0xA9A9A9); // grey color
        }

        builder.setTitle("Status for " + displayName + " (Updating <t:" + unix + ":R>)");
        String online = status.isOnline() ? "Online" : "Offline";
        MessageEmbed.Field statusField = new MessageEmbed.Field("Status <:status:1124804509814755428>", "<:arrowRight:1124446773457465559> " + online , true);
        MessageEmbed.Field playerCountField = new MessageEmbed.Field("Players <:player:1124804509814755428>", "<:arrowRight:1124446773457465559> " + status.getPlayerCount().getOnline() + "/" + status.getPlayerCount().getMax(), true);
        MessageEmbed.Field versionField = new MessageEmbed.Field("Version <:version:1124804509814755428>", "<:arrowRight:1124446773457465559> " + status.getVersionNum(), true);


        if (bedrock) {
            builder.addField(statusField);
            builder.addField(playerCountField);
            builder.addField(versionField);
        } else {
            MessageEmbed.Field motdField = new MessageEmbed.Field("MOTD <:motd:1124804509814755428>", "<:arrowRight:1124446773457465559> " + Status.removeColorCodes(status.getMotd()), true);
            builder.addField(statusField);
            builder.addField(playerCountField);
            builder.addField(versionField);
            builder.addField(motdField);
//            builder.setThumbnail(convert(status.getFavicon()));
        }

        if (status.isOnline()) {
            builder.setColor(0x00FF00); // Green
        } else {
            builder.setColor(0xFF0000); // Red
        }

        builder.setFooter("IP: " + ip + " • Port: " + port + " • Type: " + status.getType().toString().toLowerCase());
        return builder;
    }

    @NotNull
    private static String convert(@NotNull String base64) {
        if (base64.startsWith("data:image/png;base64,")) {
            base64 = base64.substring("data:image/png;base64,".length());
        }
        byte[] decodedBytes = Base64.getDecoder().decode(base64);
        String fileName = UUID.randomUUID() + ".png";

        Path path = Path.of(fileName);
        try {
            Files.write(path, decodedBytes, StandardOpenOption.CREATE);
            return "url" + path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
