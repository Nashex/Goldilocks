package parsing;

import main.Database;
import main.Goldilocks;
import moderation.NameChange;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import sheets.GoogleSheets;
import utils.MemberSearch;
import utils.Utils;
import verification.BackgroundCheck;
import verification.CompactPlayerProfile;
import verification.PlayerProfile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ParseVc {

    public static void parseVc(VoiceChannel voiceChannel, TextChannel parseChannel, Member parser, File parseImage, Message message) throws IOException {
        Long timeStarted = System.currentTimeMillis();
        List<Member> membersInVc = voiceChannel.getMembers();
        List<String> playersInRun = ImageOcr.retrievePlayersFromOcr(parseImage.getAbsolutePath());
        System.out.println(parseChannel.getGuild().getName() + " | " + String.join(", ", playersInRun.subList(1, playersInRun.size())));

        new CharacterParse().CharParse(playersInRun, voiceChannel, parseChannel, parser, false);
        List<Member> playersInVcNotInRunList = new ArrayList<>();
        HashMap<Member, String> memberIgnMap = new HashMap<>();
        String playerString = "";
        String playersNotInVc = "";
        String playersInVcNotInRun = "";
        String nameChanges = "";
        String allPlayers = "";
        String playerChannels = "";
        String kickCommands = "";
        String findCommand = "";
        String vcNotInWa = "";
        String waNotInVc = "";

        for (Member member : membersInVc) {
            if (playersInRun.contains(member.getEffectiveName().replace("|", ",").split(" , ")[0].trim().toLowerCase().replaceAll("[^A-Za-z]", ""))) {
                memberIgnMap.put(member, member.getEffectiveName().replace("|", ",").split(" , ")[0].toLowerCase().replaceAll("[^A-Za-z]", "").trim());
                playerString += ", " + member.getEffectiveName();
            } else if (member.getEffectiveName().contains("|") && playersInRun.contains(member.getEffectiveName().toLowerCase().replace("|", ",").split(" , ")[1].replaceAll("[^A-Za-z]", "").trim())){
                memberIgnMap.put(member, member.getEffectiveName().replace("|", ",").split(" , ")[1].toLowerCase().replaceAll("[^A-Za-z]", "").trim());
                playerString += ", " + member.getEffectiveName();
            } else {
                if (!member.equals(parser)) {
                    StringSimilar stringSimilar = new StringSimilar(member.getEffectiveName().replace("|", ",").split(" , ")[0].toLowerCase().replaceAll("[^A-Za-z0-9]", ""));
                    boolean found = false;
                    for (String string : playersInRun) {
                        if (stringSimilar.isSimilar(string)) {
                            if (!memberIgnMap.containsKey(member)) {
                                memberIgnMap.put(member, string);
                                found = true;
                                System.out.println("Matched: " + string + " to " + member.getEffectiveName().replace("|", ",").split(" , ")[0].toLowerCase().replaceAll("[^A-Za-z0-9]", ""));
                                break;
                            }
                        }
                    }
                    if (!found) {
                        playersInVcNotInRun += ", " + member.getAsMention();
                        playersInVcNotInRunList.add(member);
                        playerString += ", " + member.getEffectiveName();
                    }
                }
            }
        }
        for (String string : playersInRun) {
            string = string.replace("|", "");
            if (playersInRun.indexOf(string) != 0) {
                if (!memberIgnMap.containsValue(string)) {
                    Member member = MemberSearch.memberSearch(string, parser.getGuild());
                    if (member != null) {
                        if (member.getVoiceState().inVoiceChannel()) {
                            playerChannels += member.getVoiceState().inVoiceChannel() ? ", " + member.getAsMention() +
                                    "[" + member.getVoiceState().getChannel().getName() + "]" : "";
                        } else {
                            playersNotInVc += ", " + member.getAsMention();
                        }
                        kickCommands += " " + member.getEffectiveName().replaceAll("[^A-Za-z]", "");
                        findCommand += " " + member.getEffectiveName().split(" ")[0].replaceAll("[^A-Za-z]", "");
                    } else {
                        playersNotInVc += ", " + StringUtils.capitalize(string);
                        kickCommands += " " + StringUtils.capitalize(string);
                    }
                    if (!string.isEmpty()) {
                        playersInRun.set(0, playersInRun.get(0).replaceAll("(?i)" + string, "**" + StringUtils.capitalize(string) + "**"));
                        allPlayers += ", **" + string + "**";
                    }
                } else {
                    allPlayers += ", " + string;
                }
            }

        }

        if (Database.isOSanc(voiceChannel.getGuild())) {
            List<String> webAppMemberIds = getWebApp(voiceChannel.getId());
            List<String> voiceChannelMemberIds = voiceChannel.getMembers().stream().map(Member::getId).collect(Collectors.toList());

            if (webAppMemberIds != null) {
                waNotInVc = webAppMemberIds.stream().filter(s -> !voiceChannelMemberIds.contains(s) && voiceChannel.getGuild().getMemberById(s) != null)
                        .map(s -> voiceChannel.getGuild().getMemberById(s).getAsMention()).collect(Collectors.joining(", "));
                vcNotInWa = voiceChannelMemberIds.stream().filter(s -> !webAppMemberIds.contains(s) && voiceChannel.getGuild().getMemberById(s) != null)
                        .map(s -> voiceChannel.getGuild().getMemberById(s).getAsMention()).collect(Collectors.joining(", "));
            }
        }

        //new CharacterParse().CharParse(memberIgnMap.entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList()));

        allPlayers = playersInRun.get(0).length() < 2000 ? playersInRun.get(0) : playersInRun.get(0).substring(0, 2000);
        playerChannels = playerChannels.length() < 1000 ? StringUtils.replaceOnce(playerChannels, ", ", "") : StringUtils.replaceOnce(playerChannels, ", ", "").substring(0, 1000);
        playersInVcNotInRun = playersInVcNotInRun.length() < 1000 ? StringUtils.replaceOnce(playersInVcNotInRun, ", ", "") : StringUtils.replaceOnce(playersInVcNotInRun, ", ", "").substring(0, 1000);
        playersNotInVc = playersNotInVc.length() < 1000 ? StringUtils.replaceOnce(playersNotInVc, ", ", "").replaceAll("\n", "") : StringUtils.replaceOnce(playersNotInVc, ", ", "").replaceAll("\n", "").substring(0, 1000);
        playerString = playerString.length() < 1000 ? StringUtils.replaceOnce(playerString, ", ", "") : StringUtils.replaceOnce(playerString, ", ", "").substring(0, 1000);
        findCommand = findCommand.length() < 1000 ? StringUtils.replaceOnce(findCommand, " ", "").replace("  ", " ") : StringUtils.replaceOnce(findCommand, " ", "").substring(0, 1000);
        kickCommands = kickCommands.length() < 1000 ? StringUtils.replaceOnce(kickCommands, " ", "").replace("  ", " ") : StringUtils.replaceOnce(kickCommands, " ", "").substring(0, 1000);

        // Logging
        GoogleSheets.logEvent(parser.getGuild(), GoogleSheets.SheetsLogType.PARSES, parser.getEffectiveName(), parser.getId(), voiceChannel.getName(), (playersNotInVc.isEmpty() ? 0 : playersNotInVc.split(", ").length) + "", voiceChannel.getMembers().size() + "");
        Database.addParseLog(parser, kickCommands.trim().replaceAll(" ", ", "), playerString, allPlayers);

        String potentialLeaks = "";

        EmbedBuilder parseResultEmbed = parseResultEmbed(parser, playerChannels, playersInVcNotInRun, playersNotInVc, playerString, allPlayers, timeStarted, kickCommands, potentialLeaks, nameChanges, findCommand, waNotInVc, vcNotInWa);
        message.editMessage(parseResultEmbed.build()).queue();

        List<CompactPlayerProfile> cpList = new ArrayList<>();
        for (String playerName : kickCommands.split(" ")) {
            try {
                CompactPlayerProfile cp = new CompactPlayerProfile(playerName);
                cpList.add(cp);
                for (Member member : playersInVcNotInRunList) {
                    String nameChange = "";
                    nameChange = NameChange.globalNameChange(member.getUser(), playerName, cp);
                    if (!nameChange.isEmpty()) {
                        nameChanges += nameChange + "\n";
                        playersNotInVc = playersNotInVc.replace(playerName, playerName + "¹");
                        allPlayers = allPlayers.replace("**" + playerName + "**", playerName + "¹");
                        kickCommands = kickCommands.replace(playerName + " ", "");
                        playersInVcNotInRun = playersInVcNotInRun.replace(member.getAsMention(), member.getAsMention() + "¹");
                    }

                }
            } catch (PlayerProfile.PrivateProfileException e) {
                System.out.println("Private Profile for: " + playerName);
            }
        }

        if (nameChanges.isEmpty()) nameChanges = "None";
        parseResultEmbed = parseResultEmbed(parser, playerChannels, playersInVcNotInRun, playersNotInVc, playerString, allPlayers, timeStarted, kickCommands, potentialLeaks, nameChanges, findCommand, waNotInVc, vcNotInWa);
        message.editMessage(parseResultEmbed.build()).queue();

        for (CompactPlayerProfile cp : cpList) {
            try {
                List<String> playersInGuild = BackgroundCheck.getGuildMembers(cp.guild);
                Map<Member, String> guildMemberNames = voiceChannel.getMembers().stream().filter(member1 -> !member1.getRoles().isEmpty()).collect(Collectors.toList())
                        .stream().collect(Collectors.toMap(member1 -> member1, member1 -> member1.getEffectiveName().replaceAll("[^A-z- ]", "").split(" ")[0]));

                //Check guildie
                for (String player : playersInGuild) {
                    if (guildMemberNames.containsValue(player) && !player.equals(cp.username)) {
                        Member guildie = guildMemberNames.entrySet().stream().filter(memberStringEntry -> player.equalsIgnoreCase(memberStringEntry.getValue())).collect(Collectors.toList()).get(0).getKey();
                        if (guildie.getVoiceState().inVoiceChannel() && potentialLeaks.length() < 950) {
                            potentialLeaks += "" + guildie.getEffectiveName() + " ⇒ " + cp.username + " (Relation: Guildie)\n";
                        }
                    }

                }

                String affiliation = Database.getAffiliations(cp.username.toLowerCase().replaceAll("[^A-Za-z]", ""), Arrays.asList(playerString.split(", ")));
                if (!affiliation.isEmpty() && potentialLeaks.length() < 950) {
                    potentialLeaks += affiliation + "\n";
                    System.out.println(affiliation);
                }
            } catch (Exception e) {
                System.out.println("Private Profile for: " + cp.username);
            }
        }

        if (potentialLeaks.isEmpty()) potentialLeaks = "None";

        parseResultEmbed = parseResultEmbed(parser, playerChannels, playersInVcNotInRun, playersNotInVc, playerString, allPlayers, timeStarted, kickCommands, potentialLeaks, nameChanges, findCommand, waNotInVc, vcNotInWa);
        message.editMessage(parseResultEmbed.build()).queue();

    }

    public static EmbedBuilder parseResultEmbed(Member member, String playerChannels, String playersInVcNotInRun, String playersNotInVc, String playerString, String allPlayers, Long TimeStarted,
                                                String kickCommands, String potentialLeaks, String nameChanges, String findCommand, String waNotInVc, String vcNotInWa) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Parse result for " + member.getEffectiveName());
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription("__**Players in *Bold* are crashers:**__\n" + allPlayers);
        embedBuilder.addField("Players In Voice Channel:", playerString.isEmpty() ? "No players are in the voice channel.": playerString,false);
        embedBuilder.addField("Players Crashing:", playersNotInVc.isEmpty() ? "No players are crashing!": playersNotInVc,false);
        if (!playerChannels.isEmpty()) embedBuilder.addField("Players in Other Voice Channels", playerChannels, false);
        if (!playersInVcNotInRun.isEmpty()) embedBuilder.addField("Players In Vc But Not in Run", playersInVcNotInRun,false );
        if (!waNotInVc.isEmpty()) embedBuilder.addField("Players In Web App But Not in Voice Channel", waNotInVc,false );
        if (!vcNotInWa.isEmpty()) embedBuilder.addField("Players In Voice Channel But Not in Web App", vcNotInWa,false );
        if (!kickCommands.isEmpty()) embedBuilder.addField("Kick Commands: ", "```\n/kick " + kickCommands + "\n```", false);
        if (!findCommand.isEmpty()) embedBuilder.addField((Database.isShatters(member.getGuild()) || Database.isPub(member.getGuild()) ? "Find" : "U-Info") + " Command: ", "```\n" + Database.getGuildPrefix(member.getGuild().getId())
                + (Database.isShatters(member.getGuild()) || Database.isPub(member.getGuild()) ? "find " : "ui ") + findCommand + "\n```", false);
        if (nameChanges.isEmpty()) embedBuilder.addField( "Name Changes", "```\nSearching for potential name changes... \n```", false);
        else embedBuilder.addField((Database.isPub(member.getGuild()) ? "Found " : "") + "Name Changes", "```\n" + nameChanges + "\n```", false);
        if (potentialLeaks.isEmpty()) embedBuilder.addField("Potential Leaks", "```\nSearching for potential leaks... \n```", false);
        else embedBuilder.addField("Potential Leaks", "```\n" + potentialLeaks + "\n```", false);
        embedBuilder.setFooter("Time taken: " + Utils.getTimeString((System.currentTimeMillis() - TimeStarted) / 1000));
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    private static List<String> getWebApp(String voiceId) {
        List<String> peopleInWebApp = new ArrayList<>();

        try {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost("https://api.osanc.net/getActiveRaids/");

            // Request parameters and other properties.

            //Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    entity.writeTo(baos);
                    String res = new String(baos.toByteArray());
                    JSONObject json = new JSONObject(res);

                    JSONArray activeRaids = json.getJSONArray("list");
                    JSONArray members = null;

                    for (int i = 0; i < activeRaids.length(); i++) {
                        if (activeRaids.getJSONObject(i).getJSONObject("voice_channel").getString("id").equals(voiceId)) {
                            members = activeRaids.getJSONObject(i).getJSONArray("members");
                        }
                    }

                    if (members == null) return null;

                    members.forEach(o -> {
                        String memberId = ((JSONObject) o).getString("user_id");
                        if (!((JSONObject) o).getBoolean("in_waiting_list")) peopleInWebApp.add(memberId);
                    });

                    return peopleInWebApp;

                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return null;
    }

}
