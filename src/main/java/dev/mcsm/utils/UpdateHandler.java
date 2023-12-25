package dev.mcsm.utils;

import dev.mcsm.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class UpdateHandler {

    public static void sendUpdate(EmbedBuilder embed) {
        for (long channelId : SQLite.fetchAllUpdateChannels()) {
            TextChannel channel = Bot.getShardManager().getTextChannelById(channelId);
            assert channel != null;
            channel.sendMessageEmbeds(embed.build()).queue(success -> {
                System.out.println("Sent update to " + channel.getName() + " (" + channel.getId() + ")");
            }, throwable -> {
                System.out.println("Error sending update to " + channel.getName() + " (" + channel.getId() + "): " + throwable.getMessage() + "(probably deleted)");
            });
        }
    }
}
