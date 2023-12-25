package dev.mcsm;

import dev.mcsm.commands.CommandManager;
import dev.mcsm.commands.QueuedMessageHandler;
import dev.mcsm.listener.EventListener;
import dev.mcsm.utils.SQLite;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Bot {
    @Getter
    private static ShardManager shardManager = null;

    public Bot() {
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault("MTEyMzk4NjI4MTMwMzY1NDUwMQ.G6x2aD.X1TCYJLYl-XMxwDobdwaSJJ2vYXC5eySv_TQJc");
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.watching("your servers"));
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS);
        shardManager = builder.build();

        shardManager.addEventListener(new CommandManager());
        shardManager.addEventListener(new EventListener());
    }

    public static void main(String[] args) {
        Bot bot = new Bot();
        SQLite.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveQueuedMessages();
            getShardManager().setStatus(OnlineStatus.OFFLINE);
            getShardManager().shutdown();
        }));
    }

    public static void loadQueuedMessages() {
        List<String> serialized = SQLite.getData();
        for (String serializedString : serialized) {
            try {
                byte[] data = Base64.getDecoder().decode(serializedString);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                PreparedMessage preparedMessage = (PreparedMessage) ois.readObject();
                ois.close();
                TextChannel channel = Bot.shardManager.getTextChannelById(preparedMessage.channelId());
                assert channel != null;
                channel.retrieveMessageById(preparedMessage.messageId()).queue(message -> {
                    QueuedMessageHandler.QueuedMessage queuedMessage = new QueuedMessageHandler.QueuedMessage(preparedMessage.ip(), preparedMessage.port(), preparedMessage.bedrock(), preparedMessage.displayName(), message);
                    long updateTime = System.currentTimeMillis() + 2000; // current + 2 seconds
                    QueuedMessageHandler.queuedMessages.put(updateTime, queuedMessage);
                }, throwable -> {
                    System.out.println("Error loading queued message (probably deleted): " + throwable.getMessage());
                });
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void saveQueuedMessages() {
        List<String> serialized = new ArrayList<>();
        for (QueuedMessageHandler.QueuedMessage queuedMessage : QueuedMessageHandler.queuedMessages.values()) {
            try {
                PreparedMessage preparedMessage = new PreparedMessage(queuedMessage.ip(), queuedMessage.port(), queuedMessage.bedrock(), queuedMessage.displayName(), queuedMessage.message().getIdLong(), queuedMessage.message().getChannel().getIdLong());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(preparedMessage);
                oos.close();
                String serializedString = Base64.getEncoder().encodeToString(baos.toByteArray());
                serialized.add(serializedString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        SQLite.store(serialized);
    }

    public record PreparedMessage(String ip, int port, boolean bedrock, String displayName, long messageId, long channelId) implements Serializable { }
}
