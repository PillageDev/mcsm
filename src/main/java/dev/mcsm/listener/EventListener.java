package dev.mcsm.listener;

import dev.mcsm.Bot;
import dev.mcsm.commands.QueuedMessageHandler;
import dev.mcsm.utils.BugHandler;
import dev.mcsm.utils.SQLite;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

public class EventListener extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Bot.loadQueuedMessages();
        System.out.println("Loaded " + QueuedMessageHandler.queuedMessages.size() + " queued messages.");
        System.out.println("Starting update thread...");
        QueuedMessageHandler.startUpdating();
        SQLite.deleteAllData();
        Bot.getShardManager().addEventListener(new BugHandler());

        // delete command by id
        for (Guild guild : Bot.getShardManager().getGuilds()) {
            for (Command command : guild.retrieveCommands().complete()) {
                if (command.getId().equals("1124374198089949324")) {
                    command.delete().queue();
                }
                if (command.getId().equals("1132488572390359121")) {
                    command.delete().queue();
                }
                if (command.getId().equals("1124842579838652436")) {
                    command.delete().queue();
                }
            }
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent e) {

    }
}
