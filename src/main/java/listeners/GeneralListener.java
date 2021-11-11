package listeners;

import commands.miscCommands.CommandMock;
import main.Config;
import main.Database;
import main.Goldilocks;
import main.Permissions;
import modmail.ModmailHub;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang3.StringUtils;
import raids.Raid;
import raids.RaidHub;
import raids.caching.RaidCaching;
import setup.SetupConnector;
import utils.Fun;
import utils.Logging;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class GeneralListener extends ListenerAdapter {

    private static String lastPingerId = "";
    private static int numTimes = 0;
    private static long lastChange = 0;

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        if (event.isFromType(ChannelType.TEXT) && !event.getChannel().getName().contains("💬‍")
                && Database.getModmailCategory(event.getGuild()) != null
                && Database.getModmailCategory(event.getGuild()).getTextChannels().contains(event.getTextChannel())) {
            if (ModmailHub.isModmailChannel(event.getTextChannel())) {
                String channelName = event.getTextChannel().getName();
                //event.getTextChannel().getManager().setName(channelName.replace("👀", "💬")).queue();
            }
        }
        if (event.getMessage().isFromGuild() && !event.getMessage().getAuthor().isBot())
            Database.logMessage(event.getMessage());

        if (event.getMessage().getContentRaw().contains(Goldilocks.jda.getSelfUser().getId()) && event.isFromGuild()) {
            String guildInfo = SetupConnector.getFieldValue(event.getGuild(), "guildInfo","rank");
            int guildRank = guildInfo.isEmpty() ? 1 : Integer.parseInt(guildInfo);
            if (guildRank < 2 || Database.isPub(event.getGuild())) return;
            if (!lastPingerId.equalsIgnoreCase(event.getAuthor().getId())) {
                Guild guild = event.getGuild();
                String prefix = Database.getGuildPrefix(guild.getId());
                event.getChannel().sendMessage("My prefix is: `" + prefix + "`").queue(null, new ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS));
                numTimes = 0;
            } else {
                if (numTimes == 0) event.getChannel().sendMessage("Ping me again and see what happens").queue(null, new ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS));
                else if (numTimes == 1) {
                    event.getChannel().sendMessage("You asked for it...").queue(null, new ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS));
                    Goldilocks.jda.getPresence().setPresence(Activity.playing("with " + event.getMember().getEffectiveName().split("[|]")[0].replaceAll("[^A-Za-z]","") + "'s mom"), false);
                    Goldilocks.TIMER.schedule(() -> Goldilocks.jda.getPresence().setPresence(Activity.competing("Sunglas"), false), 60L, TimeUnit.SECONDS);
                    lastChange = System.currentTimeMillis();
                }
                numTimes++;
            }
            lastPingerId = event.getAuthor().getId();
        }
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        if (event.getMember() == null) return;
        if (CommandMock.mockedMembers.contains(event.getMember())) {
            String content = event.getMessage().getContentRaw();
            event.getMessage().delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
            event.getChannel().sendMessage("**" + event.getMember().getEffectiveName() + " says: **" + Fun.randomCaps(content)).queue();
        }
    }

    @Override
    public void onGuildMemberRoleAdd(@Nonnull GuildMemberRoleAddEvent event) {
        String teamRoleId;
        if (!(teamRoleId = SetupConnector.getFieldValue(event.getGuild(), "guildInfo","teamRole")).equals("0")) {
            // Check if they were given a staff role
            if (Permissions.hasPermission(event.getMember(), new String[] {"trl", "eo", "tSec"})) {
                // Check if the team role is valid
                Role teamRole = Goldilocks.jda.getRoleById(teamRoleId);

                // If it is apply the role to the member
                if (teamRole != null) {
                    Guild guild = event.getGuild();
                    guild.addRoleToMember(event.getMember(), teamRole).queue(a -> System.out.println(guild.getName() +  " | Applied Team Role to " + event.getMember().getEffectiveName()));
                }
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@Nonnull GuildMemberRoleRemoveEvent event) {
        String teamRoleId;
        if (!(teamRoleId = SetupConnector.getFieldValue(event.getGuild(), "guildInfo","teamRole")).equals("0")) {
            // Check if they were given a staff role
            if (!Permissions.hasPermission(event.getMember(), new String[] {"trl", "eo", "tSec"})) {
                // Check if the team role is valid
                Role teamRole = Goldilocks.jda.getRoleById(teamRoleId);

                // If it is remove the role from the member
                if (teamRole != null) {
                    Guild guild = event.getGuild();
                    guild.removeRoleFromMember(event.getMember(), teamRole).queue(a -> System.out.println(guild.getName() +  " | Removed Team Role from " + event.getMember().getEffectiveName()));
                }
            }
        }
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {

        // Admin Server Join Monitor
        if (event.getGuild().getId().equals(Config.get("ADMIN_SERVER"))) {
            Guild guild = event.getGuild();
            User user = event.getUser();
            List<Role> roleList = guild.getRoles();
            List<Role> guildRoles = roleList.stream().filter(role -> user.getMutualGuilds().stream().map(Guild::getName).anyMatch(s -> s.equalsIgnoreCase(role.getName()) && !s.equals("Goldilocks"))).collect(Collectors.toList());
            List<Guild> mutualGuilds = user.getMutualGuilds().stream().filter(guild1 -> guildRoles.stream().map(Role::getName).anyMatch(s -> s.equalsIgnoreCase(guild1.getName()))).collect(Collectors.toList());

            if (!guildRoles.isEmpty()) {
                // Set their nickname
                String nickName = mutualGuilds.get(0).getMember(user).getEffectiveName().split("\\|")[0].replaceAll("[^A-Za-z]", "");
                nickName = nickName.equals(user.getName()) ? nickName.toLowerCase().equals(user.getName()) ? StringUtils.capitalize(nickName) : nickName.toLowerCase() : nickName;
                guild.modifyNickname(event.getMember(), nickName).queue();

                //guildRoles.forEach(role -> guild.addRoleToMember(event.getMember(), role).queue());
                List<Role> rolesAdded = new ArrayList<>();

                // Check each guild
                for (Guild g : mutualGuilds) {
                    List<Role> memberRoles = g.getMember(user).getRoles();
                    // Add corresponding roles in the admin server
                    for (Role role : memberRoles) {
                        // Admin role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","adminRole"))) {
                            Role admin = Goldilocks.jda.getRoleById("842586385302814740");
                            guild.addRoleToMember(event.getMember(), admin).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(admin);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Developer role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","developerRole"))) {
                            Role developer = Goldilocks.jda.getRoleById("842593060357144597");
                            guild.addRoleToMember(event.getMember(), developer).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(developer);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Head Raid Leader
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","headRlRole"))) {
                            Role hrl = Goldilocks.jda.getRoleById("842595474694995969");
                            guild.addRoleToMember(event.getMember(), hrl).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(hrl);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Mod Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","modRole"))) {
                            Role mod = Goldilocks.jda.getRoleById("842547072015401001");
                            guild.addRoleToMember(event.getMember(), mod).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(mod);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Officer Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","officerRole"))) {
                            Role officer = Goldilocks.jda.getRoleById("842586721912487957");
                            guild.addRoleToMember(event.getMember(), officer).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(officer);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Head EO Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","headEoRole"))) {
                            Role heo = Goldilocks.jda.getRoleById("842595355446870016");
                            guild.addRoleToMember(event.getMember(), heo).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(heo);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Security Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","securityRole"))) {
                            Role security = Goldilocks.jda.getRoleById("842586769471832074");
                            guild.addRoleToMember(event.getMember(), security).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(security);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }

                        // Helper Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","tSecRole"))) {
                            Role helper = Goldilocks.jda.getRoleById("847241255242956801");
                            guild.addRoleToMember(event.getMember(), helper).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(helper);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }

                        // Vet Raid Leader Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","vetRlRole"))) {
                            Role vrl =  Goldilocks.jda.getRoleById("842586375542538240");
                            guild.addRoleToMember(event.getMember(), vrl).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(vrl);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Raid Leader Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","rlRole"))) {
                            Role rl = Goldilocks.jda.getRoleById("842586666870505512");
                            guild.addRoleToMember(event.getMember(), rl).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(rl);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Almost Raid Leader Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","arlRole"))) {
                            Role arl = Goldilocks.jda.getRoleById("842587079297073182");
                            guild.addRoleToMember(event.getMember(), arl).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(arl);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Event Organizer Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","eoRole"))) {
                            Role eo = Goldilocks.jda.getRoleById("842595197857562654");
                            guild.addRoleToMember(event.getMember(), eo).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(eo);
                            Role guildRole = guildRoles.stream().filter(r -> r.getName().equals(g.getName())).collect(Collectors.toList()).get(0);
                            if (!rolesAdded.contains(guildRole)) {
                                guild.addRoleToMember(event.getMember(), guildRole).queue();
                                rolesAdded.add(guildRole);
                            }
                        }


                        // Raider Role
                        if (role.getId().equals(SetupConnector.getFieldValue(g, "guildInfo","verifiedRole"))) {
                            Role raider = Goldilocks.jda.getRoleById("843498718757388349");
                            guild.addRoleToMember(event.getMember(), raider).queue();
                            if (!rolesAdded.contains(role)) rolesAdded.add(raider);
                        }

                    }
                }

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle(user.getAsTag() + " has Joined the Server")
                        .setColor(Goldilocks.LIGHTBLUE)
                        .addField("**User Info**:", user.getAsMention() + " `" + user.getId() + "`", false)
                        .addField("**Mutual Guilds:** ", mutualGuilds.stream().map(g -> "`" + g.getName() + "`").collect(Collectors.joining(", ")), false)
                        .addField("Nickname: ", "`" + nickName + "` from `" + mutualGuilds.get(0).getName() + "`", false)
                        .addField("Roles Applied: ", rolesAdded.stream().distinct().map(Role::getAsMention).collect(Collectors.joining(",")), false)
                        .setTimestamp(new Date().toInstant());

                guild.getTextChannelById("843500517539577856").sendMessage(embedBuilder.build()).queue();

            }
        }
    }

    @Override
    public void onGuildMessageUpdate(@Nonnull GuildMessageUpdateEvent event) {
        if (!SetupConnector.getChannels(event.getGuild(), "ignoredChannels").contains(event.getChannel())) return;
        if (!event.getAuthor().isBot()) Logging.logMessageEdit(event.getMessage());
    }

    public static List<String> lastPurge = new ArrayList<>();

    @Override
    public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) {
        if (!SetupConnector.getChannels(event.getGuild(), "ignoredChannels").contains(event.getChannel())) return;
        if (!Database.getMessageContent(event.getMessageId()).isEmpty() && event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS) && !lastPurge.contains(event.getMessageId())) {
            Logging.logMessageDelete(event.getGuild(), event.getMessageId());
        }
    }

    @Override
    public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
        if (ModmailHub.activeModmails.containsKey(event.getUser())) {
            ModmailHub.activeModmails.get(event.getUser()).sendMessage(event.getUser().getAsMention() + " has left the server.").queue();
        }
    }

    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent event) {
        System.out.println("Guild #" + event.getJDA().getGuilds().size() + " Joined! " + event.getGuild() + " Member count: " + event.getGuild().getMembers().size());
        if ((event.getGuild().getMembers().size() < 5 || event.getJDA().getGuilds().size() > 95) && event.getGuild().getMember(event.getJDA().getUserById(Config.get("INSTANCE_OWNER").toString())) == null) {
            if (!event.getGuild().getId().equals("343704644712923138")) {
                System.out.println("Under Guild Threshold, leaving guild." + new Date().toString());
                event.getGuild().leave().queue();
            } else {
                System.out.println("Joined pub halls.");
            }
        }
    }

//    @Override
//    public void onGuildMemberRoleRemove(@Nonnull GuildMemberRoleRemoveEvent event) {
//        Member member = event.getMember();
//        List<Role> roles = event.getRoles();
//        Role verified = Goldilocks.jda.getRoleById(Database.getGuildInfo(member.getGuild(),"verifiedRole"));
//        if (roles.contains(verified)) {
//            System.out.println("Verified removed from " + member);
//            if (member.getRoles().stream().filter(r -> r.getName().toLowerCase().contains("suspended")).count() == 0) {
//                if (verified != null) member.getGuild().addRoleToMember(member, verified).queue(aVoid -> System.out.println("Re-Added role to " + member));
//            }
//        }
//
//    }

    @Override
    public void onVoiceChannelDelete(@Nonnull VoiceChannelDeleteEvent event) {
        VoiceChannel voiceChannel = event.getChannel();
        Raid raid = RaidHub.getRaid(voiceChannel);
        if (raid != null && !raid.isCreatedVc() && raid.getRaidLeader().getVoiceState().inVoiceChannel()) {
            raid.setVoiceChannel(raid.getRaidLeader().getVoiceState().getChannel());
            RaidCaching.updateVoiceChannel(raid.getRaidLeader().getVoiceState().getChannel().getIdLong(), raid.getRaidLeader().getIdLong());
        }
    }

//    @Override
//    public void onSlashCommand(@Nonnull SlashCommandEvent event) {
//        if (event.getName().equals("chart")) {
//            event.acknowledge(false).queue();
//            List<List<LogField>> fields = new ArrayList<>();
//            List<LogField> field = DataCollector.getData(event.getGuild(), event.getOptions().get(0).getAsString(), 0);
//            fields.add(field);
//            CommandHook commandHook = event.getHook();
//            commandHook.setEphemeral(false);
//            commandHook.sendMessage(new EmbedBuilder().setColor(Goldilocks.BLUE).setImage(utils.Charts.createChart(fields, false, false)).build()).queue();
//        }
//    }

}
