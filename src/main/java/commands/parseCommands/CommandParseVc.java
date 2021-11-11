package commands.parseCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.jsoup.Jsoup;
import parsing.ParseVc;
import shatters.SqlConnector;
import utils.SSLhelper;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CommandParseVc extends Command {
    public CommandParseVc() {
        setAliases(new String[] {"parsevc", "parse", "pm"});
        setEligibleRoles(new String[] {"arl","tSec","eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.PARSE);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        if (msg.getAttachments().size() == 0) {
            Utils.errorMessage("Cannot Parse VC", "User did not attach an image to the parsevc command", msg.getTextChannel(), 10L);
            return;
        }
        if (!msg.getMember().getVoiceState().inVoiceChannel() && args.length == 0) {
            Utils.errorMessage("Cannot Parse VC", "User is not currently in voice channel", msg.getTextChannel(), 10L);
            return;
        }

        VoiceChannel voiceChannel;
        if (args.length == 0) voiceChannel = msg.getMember().getVoiceState().getChannel();
        else {
            try {
                voiceChannel = Goldilocks.jda.getVoiceChannelById(args[0]);
            } catch (Exception e) {
                Utils.errorMessage("Cannot Parse VC", "Unable to find VC", msg.getTextChannel(), 10L);
                return;
            }
        }
        TextChannel textChannel = msg.getTextChannel();
        Member member = msg.getMember();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Processing " + member.getEffectiveName() + "'s Parse Image");
        embedBuilder.setColor(Goldilocks.BLUE);
        embedBuilder.setDescription("```\nProcessing players in the image...\n```");
        embedBuilder.setTimestamp(new Date().toInstant());

        Message.Attachment attachment =  msg.getAttachments().get(0);
        Message message = textChannel.sendMessage(embedBuilder.build()).complete();
        attachment.downloadToFile("parseImages/" + attachment.getFileName())
                .exceptionally(t ->
                { // handle failure
                    t.printStackTrace();
                    return null;
                }).thenAccept(parse -> {

            //Imageocr
            File inputImage = new File("parseImages/" + attachment.getFileName());
            Goldilocks.TIMER.schedule(() -> {
                try {
                    ParseVc.parseVc(voiceChannel, textChannel, member, inputImage, message);
                } catch (IOException e) {
                    e.printStackTrace();
                    Utils.errorMessage("Failed to Parse Vc", "Check console logs", msg.getTextChannel(), 10L);
                }
            }, 0L, TimeUnit.SECONDS);

        });

        if (Database.isShatters(msg.getGuild())) SqlConnector.logFieldForMember(msg.getMember(), Arrays.asList(new String[]{"assists", "currentweekassists"}), 1);
        if (Database.isPub(msg.getGuild())) {
            SqlConnector.logFieldForMember(msg.getMember(), Arrays.asList(new String[]{"parses", "currentweekparses"}), 1, "halls");
            try {
                org.jsoup.Connection.Response response = Jsoup.connect("https://a.vibot.tech:3002/api/currentweek/update")
                        .method(org.jsoup.Connection.Method.POST)
                        .sslSocketFactory(SSLhelper.socketFactory())
                        .ignoreContentType(true)
                        .data("guildid", "343704644712923138")
                        .data("currentweektype", "2")
                        .execute();
            } catch (Exception e) {
                System.out.print("Failed to update mod current week.");
            }
        }
        Database.addParse(msg.getMember());
        Database.logEvent(msg.getMember(), Database.EventType.PARSE, System.currentTimeMillis() / 1000, msg.getTextChannel(), alias);

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Parse");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security\n";
        commandDescription += "Syntax: ;pm\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nParses a given voice channel." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
