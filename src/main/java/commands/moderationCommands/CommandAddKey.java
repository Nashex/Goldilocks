package commands.moderationCommands;

import commands.Command;
import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import raids.Raid;
import raids.RaidHub;
import utils.MemberSearch;
import utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static main.Goldilocks.eventWaiter;

public class CommandAddKey extends Command {
    public CommandAddKey() {
        setAliases(new String[] {"pop","addkey", "logkey", "ak", "addkeys"});
        setEligibleRoles(new String[] {"arl","tSec","eo"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.RAID);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {

        int numKeys = 1;
        Member member = msg.getMember();

        if (member.getVoiceState().inVoiceChannel() && (("addkey").equals(alias) || ("ak").equals(alias)) && args.length == 0) {
            Raid raid = RaidHub.getRaid(member.getVoiceState().getChannel());
            if (raid != null) {
                String messageDescription = "Please select the user you would like to add a key for.\n" +
                        "If the user you would like to add a key for is not present please enter their name below.\n";
                List<Member> keyReacts = raid.getKeyReacts();
                for (int i = 1; i <= keyReacts.size(); i++) {
                    messageDescription += "**" + i + ".** " + keyReacts.get(i - 1).getAsMention() + "\n";
                }

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Potential Keys for " + raid.getRaidLeader().getEffectiveName());
                embedBuilder.setColor(raid.getRaidColor());
                embedBuilder.setDescription(messageDescription);
                embedBuilder.setFooter("Please enter close at any time");

                Message controlPanel = msg.getTextChannel().sendMessage(embedBuilder.build()).complete();
                getUser(keyReacts, msg, controlPanel, embedBuilder, numKeys);
            } else {
                String messageDescription = "Please enter the name of the user you would like to add a key for.\n";

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Key Control Panel for " + msg.getMember().getEffectiveName());
                embedBuilder.setColor(Goldilocks.BLUE);
                embedBuilder.setDescription(messageDescription);
                embedBuilder.setFooter("Please enter close at any time");

                Message controlPanel = msg.getTextChannel().sendMessage(embedBuilder.build()).complete();
                List<Member> keyReacts = new ArrayList<>();
                getUser(keyReacts, msg, controlPanel, embedBuilder, numKeys);
            }
        } else if (args.length > 1) {
            if (!Utils.isNumeric(args[0]) && !Utils.isNumeric(args[1])) {
                msg.delete().queue();
                Utils.errorMessage("Could not add keys", "User did not enter the number of keys to add.\n" +
                        "Please enter either: addkey <Name/Id/@> <#Keys> or addkey <#Keys>", msg.getTextChannel(), 10L);
                return;
            } else {
                if (!Utils.isNumeric(args[1])) {
                    msg.delete().queue();
                    Utils.errorMessage("Could not add keys", "User did not enter the number of keys to add.\n" +
                            "Please enter either: addkey <Name/Id/@> <#Keys> or addkey <#Keys>", msg.getTextChannel(), 10L);
                    return;
                }
                numKeys = Integer.parseInt(args[1]);
                String[] argsList = {args[0]};
                List<Member> memberList = MemberSearch.memberSearch(msg, argsList);
                if (!memberList.isEmpty()) {
                    member = memberList.get(0);

                    Raid raid = RaidHub.getRaid(member.getVoiceState().getChannel());
                    if (raid != null) {
                        raid.keyReacts.add(member);
                    }

                    if (numKeys != 0) {
                        Database.addKeys(member, numKeys);
                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        embedBuilder.clear().setTitle("Keys Added by " + msg.getMember().getEffectiveName())
                                .setTimestamp(new Date().toInstant())
                                .setColor(Goldilocks.BLUE);
                        embedBuilder.setDescription("You have added " + numKeys + " key" + (numKeys == 1 ? "" : "s") + " to " + member.getAsMention() + " who now has a total of `" +
                                Database.getKeysPopped(member.getId(), member.getGuild().getId()) + " keys`");

                        //Add Key Roles
                        List<Role> eligibleRoles = Database.eligibleKeyRoles(Database.getKeysPopped(member.getId(), member.getGuild().getId()), member.getGuild().getId());
                        if (!eligibleRoles.isEmpty()) {
                            for (Role role : eligibleRoles) {
                                if (!member.getRoles().contains(role)) {
                                    member.getGuild().addRoleToMember(member, role).queue();
                                    if (!Database.getKeyRoleMessage(role).isEmpty()) {
                                        Utils.sendPM(member.getUser(), Database.getKeyRoleMessage(role));
                                    }
                                }
                            }
                        }


                        msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
                        msg.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                        return;
                    }
                } else {
                    //Utils.errorMessage("Could not find user", "Could not find user with the name " + args[0], msg.getTextChannel(), 10L);
                    msg.delete().queue();
                }
            }
        } else if (args.length > 0) {
            if (!Utils.isNumeric(args[0])) {
                msg.delete().queue();
                Utils.errorMessage("Could not add keys", "User did not enter the number of keys to add.\n" +
                        "Please enter either: addkey <Name/Id/@> <#Keys> or addkey <#Keys>", msg.getTextChannel(), 10L);
                return;
            } else {
                numKeys = Integer.parseInt(args[0]);
                String[] argsList = {args[0]};
                List<Member> memberList = MemberSearch.memberSearch(msg, argsList);
                if (!memberList.isEmpty()) {
                    member = memberList.get(0);

                    Raid raid = RaidHub.getRaid(member.getVoiceState().getChannel());
                    if (raid != null) {
                        raid.keyReacts.add(member);
                    }

                    if (numKeys != 0) {
                        Database.addKeys(member, numKeys);
                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        embedBuilder.clear().setTitle("Keys Added by " + msg.getMember().getEffectiveName())
                                .setTimestamp(new Date().toInstant())
                                .setColor(Goldilocks.BLUE);
                        embedBuilder.setDescription("You have added " + numKeys + " key" + (numKeys == 1 ? "" : "s") + " to " + member.getAsMention() + " who now has a total of `" +
                                Database.getKeysPopped(member.getId(), member.getGuild().getId()) + " keys`");

                        //Add Key Roles
                        List<Role> eligibleRoles = Database.eligibleKeyRoles(Database.getKeysPopped(member.getId(), member.getGuild().getId()), member.getGuild().getId());
                        if (!eligibleRoles.isEmpty()) {
                            for (Role role : eligibleRoles) {
                                if (!member.getRoles().contains(role)) {
                                    member.getGuild().addRoleToMember(member, role).queue();
                                    if (!Database.getKeyRoleMessage(role).isEmpty()) {
                                        Utils.sendPM(member.getUser(), Database.getKeyRoleMessage(role));
                                    }
                                }
                            }
                        }

                        msg.getTextChannel().sendMessage(embedBuilder.build()).queue();
                        msg.delete().queue();
                        return;
                    }
                } else {
                    //Utils.errorMessage("Could not find user", "Could not find user with the name " + args[0], msg.getTextChannel(), 10L);
                    msg.delete().queue();
                    return;
                }
            }
        } else {
            msg.delete().queue();
            Utils.errorMessage("Could not add keys", "User did not enter the number of keys to add.\n" +
                    "Please enter either: addkey <Name/Id/@> <#Keys> or addkey <#Keys>", msg.getTextChannel(), 10L);
            return;
        }

    }

    public static void getUser(List<Member> keyReacts, Message message, Message controlPanel, EmbedBuilder embedBuilder, int numKeys) {
        final Member[] member = {null};
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class, e -> e.getAuthor().equals(message.getAuthor()), e -> {

            if (e.getMessage().getContentRaw().equalsIgnoreCase("close")) {
                controlPanel.delete().queue();
                return;
            }

            if (Utils.isNumeric(e.getMessage().getContentRaw())) {
                int choice = Integer.parseInt(e.getMessage().getContentRaw()) - 1;
                if (choice < keyReacts.size() && choice >= 0) {
                    member[0] = keyReacts.get(choice);
                    if (numKeys != 0) {
                        Database.addKeys(member[0], numKeys);
                        embedBuilder.clear().setTitle("Keys Added by " + e.getMember().getEffectiveName())
                                .setTimestamp(new Date().toInstant())
                                .setColor(Goldilocks.BLUE);
                        embedBuilder.setDescription("You have added " + numKeys + " key" + (numKeys == 1 ? "" : "s") + " to " + member[0].getAsMention() + " who now has a total of `" +
                                Database.getKeysPopped(member[0].getId(), member[0].getGuild().getId()) + " keys`");

                        //Add Key Roles
                        List<Role> eligibleRoles = Database.eligibleKeyRoles(Database.getKeysPopped(member[0].getId(), member[0].getGuild().getId()), member[0].getGuild().getId());
                        if (!eligibleRoles.isEmpty()) {
                            for (Role role : eligibleRoles) {
                                if (!member[0].getRoles().contains(role)) {
                                    member[0].getGuild().addRoleToMember(member[0], role).queue();
                                    if (!Database.getKeyRoleMessage(role).isEmpty()) {
                                        Utils.sendPM(member[0].getUser(), Database.getKeyRoleMessage(role));
                                    }
                                }
                            }
                        }

                        controlPanel.editMessage(embedBuilder.build()).queue();
                        message.delete().queue();
                        e.getMessage().delete().queue();
                    }
                    return;
                }
            }

            List<Member> memberList = MemberSearch.memberSearch(e.getMessage(), e.getMessage().getContentRaw().toLowerCase().split(" "));
            if (!memberList.isEmpty()) {
                member[0] = memberList.get(0);
                if (numKeys != 0) {
                    Database.addKeys(member[0], numKeys);
                    embedBuilder.clear().setTitle("Keys Added by " + e.getMember().getEffectiveName())
                            .setTimestamp(new Date().toInstant())
                            .setColor(Goldilocks.BLUE);
                    embedBuilder.setDescription("You have added " + numKeys + " key" + (numKeys == 1 ? "" : "s") + " to " + member[0].getAsMention() + " who now has a total of `" +
                            Database.getKeysPopped(member[0].getId(), member[0].getGuild().getId()) + " keys`");

                    //Add Key Roles
                    List<Role> eligibleRoles = Database.eligibleKeyRoles(Database.getKeysPopped(member[0].getId(), member[0].getGuild().getId()), member[0].getGuild().getId());
                    if (!eligibleRoles.isEmpty()) {
                        for (Role role : eligibleRoles) {
                            if (!member[0].getRoles().contains(role)) {
                                member[0].getGuild().addRoleToMember(member[0], role).queue();
                                if (!Database.getKeyRoleMessage(role).isEmpty()) {
                                    Utils.sendPM(member[0].getUser(), Database.getKeyRoleMessage(role));
                                }
                            }
                        }
                    }


                    controlPanel.editMessage(embedBuilder.build()).queue();
                    e.getMessage().delete().queue();
                    message.delete().queue();
                }
            } else {
                Utils.errorMessage("Could not find user", "Could not find user with the name " + e.getMessage().getContentRaw(), controlPanel, 10L);
            }
        }, 2L, TimeUnit.MINUTES, () -> {controlPanel.delete().queue();});
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Add key");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Trial Security | Almost Raid Leader\n";
        commandDescription += "Syntax: ;alias\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nAdds a key to the recipient." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
