package dev.mcsm.utils;

import dev.mcsm.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BugHandler extends ListenerAdapter {

    private static final TextChannel bugChannel = Bot.getShardManager().getTextChannelById(1132719170233049159L);

    public static void reportNewBug(@NotNull User user, String bug) {
        SQLite.newBug(Long.parseLong(user.getId()), bug);
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("New Bug");
        embedBuilder.addField("User", user.getAsMention(), false);
        embedBuilder.addField("Bug", bug, false);
        embedBuilder.addField("ID", String.valueOf(SQLite.getBugId(bug)), false);
        Button button = Button.danger("bug:" + SQLite.getBugId(bug), "Message Author");
        Button statusChange = Button.primary("status:" + SQLite.getBugId(bug), "Change Status");

        assert bugChannel != null;
        bugChannel.sendMessageEmbeds(embedBuilder.build()).setActionRow(button, statusChange).queue(message -> {
            SQLite.setMessageId(SQLite.getBugId(bug), message.getIdLong());

            message.createThreadChannel("Bug Thread (#" + SQLite.getBugId(bug) + ")").queue(thread -> {
                thread.sendMessage("This is the bug thread for bug #" + SQLite.getBugId(bug)).queue();
                SQLite.setThreadId(SQLite.getBugId(bug), thread.getIdLong());
                System.out.println(thread.getIdLong());
            });
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent e) {
        String customId = e.getButton().getId();
        assert customId != null;
        if (customId.startsWith("bug:")) {
            int bugId = Integer.parseInt(customId.split(":")[1]);
            TextInput replyBody = TextInput.create("response", "Respond to the bug reporter", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Hello user,")
                    .setMinLength(10)
                    .build();
            Modal replyModal = Modal.create("devresponse:" + bugId, "Bug Response")
                    .addComponents(ActionRow.of(replyBody))
                    .build();
            e.replyModal(replyModal).queue();
        } else if (customId.startsWith("response:")) {
            int bugId = Integer.parseInt(customId.split(":")[1]);
            TextInput replyBody = TextInput.create("response", "Respond to the developer", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Hello user,")
                    .setMinLength(10)
                    .build();
            Modal replyModal = Modal.create("response:" + bugId, "Respond to the developer")
                    .addComponents(ActionRow.of(replyBody))
                    .build();
            e.replyModal(replyModal).queue();
        } else if (customId.startsWith("status:")) {
            int bugId = Integer.parseInt(customId.split(":")[1]);
            e.reply("Select a status").addActionRow(
                    StringSelectMenu.create("status:" + bugId)
                            .addOption("Open", "open")
                            .addOption("In Progress", "inprogress")
                            .addOption("Resolved", "resolved")
                            .addOption("Closed", "closed")
                            .build()
            ).setEphemeral(true).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String customId = event.getModalId();
        if (customId.startsWith("devresponse:")) {
            int bugId = Integer.parseInt(customId.split(":")[1]);
            String reply = Objects.requireNonNull(event.getValue("response")).getAsString();
            User user = Bot.getShardManager().getUserById(SQLite.getUserId(bugId));
            assert user != null;
            user.openPrivateChannel().queue(pc -> {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Developer Response (#" + bugId + ")");
                embedBuilder.setDescription(reply);

                Button button = Button.primary("response:" + bugId, "Respond");
                pc.sendMessageEmbeds(embedBuilder.build()).setActionRow(button).queue();
            });
            event.reply("Successfully responded to the bug reporter").setEphemeral(true).queue();
        } else if (customId.startsWith("response:")) {
            int bugId = Integer.parseInt(customId.split(":")[1]);
            String reply = Objects.requireNonNull(event.getValue("response")).getAsString();
            User user = Bot.getShardManager().getUserById(SQLite.getUserId(bugId));
            assert user != null;
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Reporter Response");
            embedBuilder.setDescription(reply);

            Button button = Button.primary("bug:" + bugId, "Respond");
            assert bugChannel != null;
            long threadId = SQLite.getThreadId(bugId);
            System.out.println(threadId);
            ThreadChannel threadChannel = bugChannel.getGuild().getThreadChannelById(threadId);
            assert threadChannel != null;
            threadChannel.sendMessageEmbeds(embedBuilder.build()).setActionRow(button).queue();
            event.reply("Successfully responded to the developer").setEphemeral(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String customId = event.getComponentId();
        if (customId.startsWith("status:")) {
            int bugId = Integer.parseInt(customId.split(":")[1]);
            String status = Objects.requireNonNull(event.getValues()).get(0);
            switch (status) {
                case "open" -> SQLite.updateBugStatus(bugId, BugStatus.OPEN);
                case "inprogress" -> SQLite.updateBugStatus(bugId, BugStatus.IN_PROGRESS);
                case "resolved" -> SQLite.updateBugStatus(bugId, BugStatus.RESOLVED);
                case "closed" -> SQLite.updateBugStatus(bugId, BugStatus.CLOSED);
            }
            User user = Bot.getShardManager().getUserById(SQLite.getUserId(bugId));
            assert user != null;
            user.openPrivateChannel().queue(pc -> {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Bug Status Update (#" + bugId + ")");
                embedBuilder.setDescription("Your bug has been updated to " + status);

                Button button = Button.primary("response:" + bugId, "Respond");
                pc.sendMessageEmbeds(embedBuilder.build()).setActionRow(button).queue();
            });
            assert bugChannel != null;
            ThreadChannel threadChannel = bugChannel.getGuild().getThreadChannelById(SQLite.getThreadId(bugId));
            assert threadChannel != null;
            threadChannel.sendMessage("Bug status updated to " + status).queue();
            Message message = bugChannel.retrieveMessageById(SQLite.getMessageId(bugId)).complete();
            assert message != null;
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("New Bug");
            embedBuilder.addField("User", user.getAsMention(), false);
            embedBuilder.addField("Bug", Objects.requireNonNull(SQLite.getBug(bugId)), false);
            embedBuilder.addField("ID", String.valueOf(SQLite.getBugId(SQLite.getBug(bugId))), false);
            embedBuilder.addField("Status", status, false);
            Button button = Button.danger("bug:" + SQLite.getBugId(SQLite.getBug(bugId)), "Message Author");
            Button statusChange = Button.primary("status:" + SQLite.getBugId(SQLite.getBug(bugId)), "Change Status");
            message.editMessageEmbeds(embedBuilder.build()).setActionRow(button, statusChange).queue();

            event.reply("Successfully updated status").setEphemeral(true).queue();
        }
    }
}
