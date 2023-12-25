package dev.mcsm.commands;

import dev.mcsm.utils.BugHandler;
import dev.mcsm.utils.SQLite;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandManager extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String command = event.getName();
        switch (command) {
            case "status" -> {
                OptionMapping ip = event.getOption("ip");
                OptionMapping port = event.getOption("port");
                OptionMapping displayName = event.getOption("display-name");
                OptionMapping bedrock = event.getOption("bedrock");
                if (ip == null) {
                    event.reply("You must provide an IP.").setEphemeral(true).queue();
                    return;
                }
                boolean bedrockBoolean = bedrock != null && bedrock.getAsBoolean();
                int portInt;
                if (port != null) {
                    portInt = Integer.parseInt(port.getAsString());
                } else if (bedrockBoolean) { // Bedrock
                    portInt = 19132;
                } else { // Java
                    portInt = 25565;
                }
                String displayNameString = displayName == null ? ip.getAsString() : displayName.getAsString();
                event.reply("Checking status...").setEphemeral(true).queue();
                QueuedMessageHandler.handleStatusCommand(event.getChannel(), ip.getAsString(), portInt, bedrockBoolean, displayNameString);
            }
            case "invite" -> {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("Invite Me");
                builder.setDescription("Invite me to your server using the invite link below:\n[Invite Link](https://discord.com/api/oauth2/authorize?client_id=1123986281303654501&permissions=518080785472&scope=bot)");
                builder.setColor(Color.GREEN);
                event.replyEmbeds(builder.build()).queue();
            }
            case "set" -> {
                String subcommand = event.getSubcommandName();
                assert subcommand != null;
                if (subcommand.equals("update-channel")) {
                    TextChannel channel = Objects.requireNonNull(event.getOption("channel")).getAsChannel().asTextChannel();
                    SQLite.setUpdateChannel(Integer.parseInt(Objects.requireNonNull(event.getGuild()).getId()), Integer.parseInt(channel.getId()));
                }
            }
            case "bug" -> {
                String bug = Objects.requireNonNull(event.getOption("bug")).getAsString();
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("Successfully reported bug");
                builder.setDescription("Your bug has been successfully reported. A developer will respond to you shortly.");
                builder.setColor(Color.GREEN);
                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                BugHandler.reportNewBug(event.getUser(), bug);
            }
            case "discord" -> {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("Discord");
                builder.setDescription("Join our Discord server [here](https://discord.gg/4NDc3NVyck).");
                builder.setColor(Color.GREEN);
                event.replyEmbeds(builder.build()).queue();
            }
            case "help" -> {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("<:logo:1124378771995242496> MC Status Bot");
                MessageEmbed.Field commandsField = new MessageEmbed.Field("<:keyboardmccm:1124378090202734714> Commands", """
                        `/status` - Get the status of a Minecraft server.
                        `/invite` - Get the invite link for the bot.
                        `/bug` - Report a bug to the developers.
                        `/discord` - Get the Discord invite link for our support server!
                        """, false);
                MessageEmbed.Field infoField = new MessageEmbed.Field("<:info:1124383061098897449> Info", """
                        MCSM (Minecraft Server Monitor is a bot that shows multiple different statistics about your and others Minecraft servers! You can check the uptime and player count for other servers too! The bot is currently in beta, so if you find any bugs, please report them to the support server. If you have any suggestions, please also report them to the support server. Thanks for using MCSM!
                        """, false);
                MessageEmbed.Field sponsorField = new MessageEmbed.Field("<:money:1124384767316607037> Sponsors", """
                        MCSM is proudly sponsored by [<:springracks:1124386689234771969> Springracks](https://springracks.com) who generously provides MCSM with the servers to run on. You can get your own server from them here. Thank you again to SpringRacks for sponsoring MCSM!
                        """, false);
                MessageEmbed.Field supportField = new MessageEmbed.Field("<:help:1124378125954994318> Get Support", """
                        If you need any help with MCSM, please join our support server [here](https://discord.gg/4NDc3NVyck).
                        """, false);
                builder.addField(commandsField);
                builder.addField(infoField);
                builder.addField(sponsorField);
                builder.addField(supportField);
                builder.setColor(Color.GREEN);
                event.replyEmbeds(builder.build()).queue();
            }
        }
    }

    /**
     * This is for guild commands. (ONLY USE FOR TESTING)
     * @param event GuildReadyEvent
     */
//    @Override
//    public void onGuildReady(@NotNull GuildReadyEvent event) {
//        event.getGuild().updateCommands().addCommands(getCommands()).queue();
//    }

    /**
     * This is for guild commands. (ONLY USE FOR TESTING)
     * @param event GuildJoinEvent
     */
//    @Override
//    public void onGuildJoin(@NotNull GuildJoinEvent event) {
//        event.getGuild().updateCommands().addCommands(getCommands()).queue();
//    }

    @NotNull
    private List<CommandData> getCommands() {
        List<CommandData> commandData = new ArrayList<>();

        // Status command
        OptionData statusOptionIp = new OptionData(OptionType.STRING, "ip", "The IP of the server.").setRequired(true);
        OptionData statusOptionPort = new OptionData(OptionType.INTEGER, "port", "The port of the server.").setRequired(false);
        OptionData statusOptionDisplayName = new OptionData(OptionType.STRING, "display-name", "The display name of the server.").setRequired(false);
        OptionData statusOptionBedrock = new OptionData(OptionType.BOOLEAN, "bedrock", "Whether the server is bedrock or not.").setRequired(false);
        commandData.add(Commands.slash("status", "Get the status of a Minecraft server.").addOptions(statusOptionIp, statusOptionPort, statusOptionDisplayName, statusOptionBedrock));

        // Invite command
        commandData.add(Commands.slash("invite", "Get the invite link for the bot"));

        // Bug command
        OptionData bugOption = new OptionData(OptionType.STRING, "bug", "The bug to report.").setRequired(true);
        commandData.add(Commands.slash("bug", "Report a bug to the developers.").addOptions(bugOption));

        // Discord command
        commandData.add(Commands.slash("discord", "Get the Discord invite link for our support server!"));

        // Help command
        commandData.add(Commands.slash("help", "Get help with MCSM."));

        return commandData;
    }

    /**
     * This is for global commands. (ONLY USE FOR PRODUCTION)
     * @param event ReadyEvent
     */
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        event.getJDA().updateCommands().addCommands(getCommands()).queue();
    }

}
