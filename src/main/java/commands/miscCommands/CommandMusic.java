package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.Date;

public class CommandMusic extends Command {
    public CommandMusic() {
        setAliases(new String[] {"mood","oogirl","cracked","rickroll","sorry", "ph", "crabrave", "thomas", "uwu", "sunglas", "nashex","lushi","die", "chonky", "chonkylog", "photag", "pogchamp"});
        setEligibleRoles(new String[] {"mod"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.GAME);
    }


    @Override
    public void execute(Message msg, String alias, String[] args) {

//        if (("sunglas").equalsIgnoreCase(alias)) {
//            msg.getTextChannel().sendTyping().queue();
//            msg.getTextChannel().sendMessage("Welcome to the swamp bitches").tts(true).queueAfter(1L, TimeUnit.MINUTES);
//            msg.delete().queue();
//            return;
//        }
//
//        if (!msg.getMember().getVoiceState().inVoiceChannel()) {
//            return;
//        }
//
//        VoiceChannel voiceChannel = msg.getMember().getVoiceState().getChannel();
//        AudioManager audioManager = msg.getGuild().getAudioManager();
//        Member selfMember = msg.getGuild().getSelfMember();
//
//        if (!selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
//            Utils.sendPM(msg.getAuthor(), "I am missing the permission to join " + msg.getMember().getEffectiveName() + "'s voice channel.");
//            return;
//        }
//
//        if (RaidHub.getRaid(voiceChannel) != null && !voiceChannel.getName().toLowerCase().contains("veteran")) {
//            Utils.sendPM(msg.getAuthor(), "I cannot play sound bytes in Raids 🤡");
//            return;
//        }
//
//        audioManager.openAudioConnection(voiceChannel);
//
//        PlayerManager manager = PlayerManager.getInstance();
//
//        manager.loadAndPlay(msg.getTextChannel(), "music/" + alias + ".wav");
//        manager.getGuildMusicManager(msg.getGuild()).player.setVolume(10);
//
//        if (alias.equalsIgnoreCase("mood")) {
//            msg.getChannel().sendMessage("https://cdn1.participoll.com/wp-content/uploads/2019/07/21074300/fireplace-burning-fire-animated-gif.gif")
//                    .complete().delete().queueAfter(1L, TimeUnit.MINUTES);
//        }
//
//        if (alias.equalsIgnoreCase("thomas") || alias.equalsIgnoreCase("uwu")) manager.getGuildMusicManager(msg.getGuild()).player.setVolume(100);
//
//        if (alias.equalsIgnoreCase("cracked")) {
//            manager.getGuildMusicManager(msg.getGuild()).player.setVolume(100);
//            msg.getChannel().sendMessage("https://gifdownload.net/wp-content/uploads/2019/02/earth-explode-gif-4.gif")
//                    .complete().delete().queueAfter(10L, TimeUnit.SECONDS);
//        }
//
//        if (alias.equalsIgnoreCase("rickroll")) {
//            manager.getGuildMusicManager(msg.getGuild()).player.setVolume(10);
//            msg.getChannel().sendMessage("https://media4.giphy.com/media/ZE5DmCqNMr3yDXq1Zu/source.gif")
//                    .complete().delete().queueAfter(10L, TimeUnit.SECONDS);
//        }
//
//
//        msg.delete().queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Music");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Mod\n";
        commandDescription += "Syntax: -alias <command alias>\n";
        commandDescription +="Aliases: " + aliases + "\n";
        commandDescription += "\nPlays our favorite sound bites" + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
