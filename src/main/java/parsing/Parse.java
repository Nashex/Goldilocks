package parsing;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import stats.BarConstructor;
import stats.Character;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Parse {

    private static ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(1);
    public static HashMap<Member, String> memberReqsHashMap = new HashMap<>();

    public static void parse(VoiceChannel voiceChannel, TextChannel parseChannel, Member parser, File parseImage, Message message) {
        System.out.println("Started Parse");
        memberReqsHashMap = new HashMap<>();
        TIMER.schedule(() -> {
            Long timeStarted = System.currentTimeMillis();
            List<Member> membersInVc = voiceChannel.getMembers();
            List<String> playersInRun = null;
            try {
                playersInRun = ImageOcr.retrievePlayersFromOcr(parseImage.getAbsolutePath());
                System.out.println(voiceChannel.getGuild().getName() + " | Parsed players ⇒ " + String.join(", ", playersInRun));
            } catch (IOException e) {
                e.printStackTrace();
            }
            HashMap<Member, String> memberIgnMap = new HashMap<>();
            String playerString = "";
            String playersNotInVc = "";
            String playersInVcNotInRun = "";
            String allPlayers = "";
            String playerChannels = "";
            final String[] kickCommands = {""};

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
                            playerString += ", " + member.getEffectiveName();
                        }
                    }
                }
            }
            for (String string : playersInRun) {
                string = string.replace("|", "");
                List<Member> members = parser.getGuild().getMembers();
                if (playersInRun.indexOf(string) != 0) {
                    if (!memberIgnMap.containsValue(string)) {
                        boolean found = false;
                        for (Member member : members) {
                            if (member.getEffectiveName().replaceAll("[^A-Za-z]", "").toLowerCase().equals(string)) {
                                if (member.getVoiceState().inVoiceChannel()) {
                                    playerChannels += member.getVoiceState().inVoiceChannel() ? ", " + member.getAsMention() +
                                            "[" + member.getVoiceState().getChannel().getName() + "]" : "";
                                } else {
                                    playersNotInVc += ", " + member.getAsMention();
                                }
                                found = true;
                                kickCommands[0] += " " + member.getEffectiveName().replaceAll("[^A-Za-z]", "");
                            }
                        }
                        if (!found) {
                            playersNotInVc += ", " + StringUtils.capitalize(string);
                            kickCommands[0] += " " + StringUtils.capitalize(string);
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
            allPlayers = playersInRun.get(0).length() < 2000 ? playersInRun.get(0) : playersInRun.get(0).substring(0, 2000);
            playerChannels = playerChannels.length() < 1000 ? StringUtils.replaceOnce(playerChannels, ", ", "") : StringUtils.replaceOnce(playerChannels, ", ", "").substring(0, 1000);
            playersInVcNotInRun = playersInVcNotInRun.length() < 1000 ? StringUtils.replaceOnce(playersInVcNotInRun, ", ", "") : StringUtils.replaceOnce(playersInVcNotInRun, ", ", "").substring(0, 1000);
            playersNotInVc = playersNotInVc.length() < 1000 ? StringUtils.replaceOnce(playersNotInVc, ", ", "").replaceAll("\n", "") : StringUtils.replaceOnce(playersNotInVc, ", ", "").replaceAll("\n", "").substring(0, 1000);
            playerString = playerString.length() < 1000 ? StringUtils.replaceOnce(playerString, ", ", "") : StringUtils.replaceOnce(playerString, ", ", "").substring(0, 1000);
            kickCommands[0] = kickCommands[0].length() < 1000 ? StringUtils.replaceOnce(kickCommands[0], " ", "") : StringUtils.replaceOnce(kickCommands[0], " ", "").substring(0, 1000);
            System.out.println("Parsing");
            message.editMessage(parseResultEmbed(parser, playerChannels, playersInVcNotInRun, playersNotInVc, playerString, allPlayers, timeStarted, kickCommands[0]).build()).queue();

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(parser.getEffectiveName() + "'s Requirements Parse");
            embedBuilder.setColor(Goldilocks.BLUE);
            embedBuilder.setDescription("```\nParsing Player Requirements\n```");
            embedBuilder.setTimestamp(new Date().toInstant());

            Message playerParseMessage = message.getTextChannel().sendMessage(embedBuilder.build()).complete();

            membersInVc.forEach((member) -> {
                String userName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]","");
                if (member.getEffectiveName().contains("|")) {
                    userName = member.getEffectiveName().replace("|", ",").split(",")[0].replaceAll("[^A-Za-z]", "");
                }

                String playerInfo = "";
                String compactInfo = "";
                try {
                    boolean[] parseArray = ParseMember.parseMember(userName, 1);
                    boolean matched = false;

                    if (parseArray != null) {
                        if (!parseArray[0] || !parseArray[1] || !parseArray[2] || !parseArray[3] || !parseArray[4] || !parseArray[5]) {
                            Character character = stats.RealmPlayer.getCharacter(userName, 1);
                            playerInfo = "**Tag:**" + member.getAsMention() + " | [RealmEye](https://realmeye.com/player/" + userName + ")"
                                    + "\n**Class:** " + Goldilocks.jda.getEmotesByName(character.getName(), true).get(0).getAsMention() + " | **Pots To Max: **";
                            compactInfo = "**Class:** " + Goldilocks.jda.getEmotesByName(character.getName(), true).get(0).getAsMention() + " | **Pots To Max: **";
                            int[] baseStats = BarConstructor.getBaseStats(character.getName());
                            int[] charStats = character.getStatsArray();

                            //Check attack to max
                            if (!((baseStats[2] - charStats[2]) == 0)) {
                                Emote attackPot = Goldilocks.jda.getEmotesByName("attack", true).get(0);
                                playerInfo += attackPot.getAsMention() + "x" + (baseStats[2] - charStats[2]) + " ";
                                compactInfo += attackPot.getAsMention() + "x" + (baseStats[2] - charStats[2]) + " ";
                            }

                            //Check dex to max
                            if (!((baseStats[5] - charStats[7])  == 0)) {
                                Emote dexPot = Goldilocks.jda.getEmotesByName("dexterity", true).get(0);
                                playerInfo +=  dexPot.getAsMention() + "x" + (baseStats[5] - charStats[7]) + " ";
                                compactInfo +=  dexPot.getAsMention() + "x" + (baseStats[5] - charStats[7]) + " ";
                            }

                            String weaponTier = character.getWeapon();
                            weaponTier = weaponTier.contains("UT") ? weaponTier.contains("ST") ? "ST" : "UT" : "T" + weaponTier.replaceAll("[^0-9.]", "");
                            String abilityTier = character.getAbility();
                            abilityTier = abilityTier.contains("UT") ? abilityTier.contains("ST") ? "ST" : "UT" : "T" + abilityTier.replaceAll("[^0-9.]", "");
                            String armorTier = character.getArmor();
                            armorTier = armorTier.contains("UT") ? armorTier.contains("ST") ? "ST" : "UT" : "T" + armorTier.replaceAll("[^0-9.]", "");
                            String ringTier = character.getRing();
                            ringTier = ringTier.contains("UT") ? ringTier.contains("ST") ? "ST" : "UT" : "T" + ringTier.replaceAll("[^0-9.]", "");

                            if (weaponTier.equals("T")) weaponTier = "Empty";
                            if (abilityTier.equals("T")) abilityTier = "Empty";
                            if (armorTier.equals("T")) armorTier = "Empty";
                            if (ringTier.equals("T")) ringTier = "Empty";
                            try {
                                String weaponEmoteName = Utils.toEmoteFormat(character.getWeapon());
                                String abilityEmoteName = Utils.toEmoteFormat(character.getAbility());
                                String armorEmoteName = Utils.toEmoteFormat(character.getArmor());
                                String ringEmoteName = Utils.toEmoteFormat(character.getRing());

                                String weaponEmote = null;
                                String abilityEmote = null;
                                String armorEmote = null;
                                String ringEmote = null;

                                try {
                                    weaponEmote = Goldilocks.jda.getEmotesByName(weaponEmoteName, true).get(0).getAsMention();
                                } catch (Exception e) {
                                    weaponEmote = "⭕";
                                    System.out.println(weaponEmoteName);
                                }
                                try {
                                    abilityEmote = Goldilocks.jda.getEmotesByName(abilityEmoteName, true).get(0).getAsMention();
                                } catch (Exception e) {
                                    abilityEmote = "⭕";
                                    System.out.println(abilityEmoteName);
                                }

                                try {
                                    armorEmote = Goldilocks.jda.getEmotesByName(armorEmoteName, true).get(0).getAsMention();
                                } catch (Exception e) {
                                    armorEmote = "⭕";
                                    System.out.println(armorEmoteName);
                                }

                                try {
                                    ringEmote = Goldilocks.jda.getEmotesByName(ringEmoteName, true).get(0).getAsMention();
                                } catch (Exception e) {
                                    ringEmote = "⭕";
                                    System.out.println(ringEmoteName);
                                }
                                String equips = "\n" + "**Equips: **" + weaponEmote + abilityEmote + armorEmote + ringEmote + " | `" + weaponTier + "` | `" + abilityTier + "` | `" + armorTier + "` | `" + ringTier + "`";
                                playerInfo += equips;
                                compactInfo += equips;

                            } catch (NullPointerException e) {
                                e.printStackTrace();
                                playerInfo = userName + " is not a valid player or their profile is set to private.";
                            }

                        }
                    }

                    if (!playerInfo.isEmpty()) {
                        embedBuilder.addField("__**Player:** " + userName + "__", playerInfo
                                + "\n**Lock:** `/lock " + userName + "`", false);
                        if(!matched && !kickCommands[0].contains(userName)) kickCommands[0] += " " + userName;
                        memberReqsHashMap.put(member, compactInfo);
                        matched = true;
                    }
                } catch (Exception e) {
                    System.out.println("Player " + userName + " has a private profile.");
                    e.printStackTrace();
                    embedBuilder.addField("**Player:** " + userName, "[RealmEye](https://realmeye.com/player/" + userName + ") | " + "Player's profile is set to private."
                            + "\n**Lock:** `/lock " + userName + "`", false);
                }
            });
            kickCommands[0] = kickCommands[0].length() < 1000 ? kickCommands[0] : kickCommands[0].substring(0, 1000);
            if (!kickCommands[0].isEmpty()) embedBuilder.addField("Updated Kick Commands: ", "```\n/kick " + kickCommands[0] + "\n```", false);
            embedBuilder.setFooter("Time taken: " + Utils.getTimeString((System.currentTimeMillis() - timeStarted) / 1000));
            embedBuilder.setTimestamp(new Date().toInstant());

            if (embedBuilder.getFields().size() == 0) embedBuilder.setDescription("All players meet requirements!");
            else embedBuilder.setDescription("");
            playerParseMessage.editMessage(embedBuilder.build()).complete();
            System.out.println("Parse Finished");
        }, 0L, TimeUnit.SECONDS);
    }

    public static EmbedBuilder parseResultEmbed(Member member, String playerChannels, String playersInVcNotInRun, String playersNotInVc, String playerString, String allPlayers, Long TimeStarted, String kickCommands) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle("Parse result for " + member.getEffectiveName());
        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setDescription("__**Players in *Bold* are crashers:**__\n" + allPlayers);
        embedBuilder.addField("Players In Voice Channel:", playerString.isEmpty() ? "No players are crashing!": playerString,false);
        embedBuilder.addField("Players Crashing:", playersNotInVc.isEmpty() ? "No players are crashing!": playersNotInVc,false);
        if (!playerChannels.isEmpty()) embedBuilder.addField("Players in Other Voice Channels", playerChannels, false);
        if (!playersInVcNotInRun.isEmpty()) embedBuilder.addField("Players In Vc But Not in Run", playersInVcNotInRun,false );
        if (!kickCommands.isEmpty()) embedBuilder.addField("Kick Commands: ", "```\n/kick " + kickCommands + "\n```", false);
        embedBuilder.setFooter("Time taken: " + Utils.getTimeString((System.currentTimeMillis() - TimeStarted) / 1000));
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }
}
