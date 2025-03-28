package com.jelly.farmhelperv2.remote.command.discordCommands.impl;

import com.google.gson.JsonObject;
import com.jelly.farmhelperv2.remote.WebsocketHandler;
import com.jelly.farmhelperv2.remote.command.discordCommands.DiscordCommand;
import com.jelly.farmhelperv2.remote.command.discordCommands.Option;
import com.jelly.farmhelperv2.remote.waiter.Waiter;
import com.jelly.farmhelperv2.remote.waiter.WaiterHandler;
import com.jelly.farmhelperv2.util.AvatarUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.Objects;

public class SendCommand extends DiscordCommand {
    public static final String name = "sendcommand";
    public static final String description = "Run a minecraft command";

    public SendCommand() {
        super(name, description);
        addOptions(new Option(OptionType.STRING, "command", "The command WITHOUT THE SLASH", true, false),
                new Option(OptionType.STRING, "ign", "The IGN of the instance", false, true)
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        WaiterHandler.register(new Waiter(
                15000,
                name,
                action -> {
                    String username = action.args.get("username").getAsString();
                    String cmd = action.args.get("command").getAsString();
                    String uuid = action.args.get("uuid").getAsString();
                    boolean error = action.args.has("error");

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.addField("Username", username, false);
                    if (error) {
                        embedBuilder.addField("Error", action.args.get("error").getAsString(), false);
                    } else {
                        embedBuilder.addField("Ran command:", cmd, false);
                    }
                    int random = (int) (Math.random() * 0xFFFFFF);
                    embedBuilder.setColor(random);
                    embedBuilder.setFooter("-> FarmHelper Remote Control", "https://cdn.discordapp.com/attachments/861700235890130986/1144673641951395982/icon.png");
                    String avatar = AvatarUtils.getAvatarUrl(uuid);
                    embedBuilder.setAuthor("Instance name -> " + username, avatar, avatar);

                    try {
                        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                    }
                },
                timeoutAction -> event.getHook().sendMessage("Can't run command").queue(),
                event
        ));

        int cmd = Objects.requireNonNull(event.getOption("command")).getAsInt();
        JsonObject args = new JsonObject();
        args.addProperty("command", cmd);
        if (event.getOption("ign") != null) {
            String ign = Objects.requireNonNull(event.getOption("ign")).getAsString();
            if (WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.containsValue(ign)) {
                WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.forEach((webSocket, s) -> {
                    if (s.equals(ign)) {
                        sendMessage(webSocket, args);
                    }
                });
            } else {
                event.getHook().sendMessage("There isn't any instances connected with that IGN").queue();
            }
        } else {
            WebsocketHandler.getInstance().getWebsocketServer().minecraftInstances.forEach((webSocket, s) -> sendMessage(webSocket, args));
        }
    }
}
