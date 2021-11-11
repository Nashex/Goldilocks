package listeners;

import commands.moderationCommands.CommandDrag;
import commands.moderationCommands.CommandLockdown;
import commands.raidCommands.CommandChangeRaidType;
import commands.raidCommands.CommandTransferRaid;
import main.Config;
import main.Database;
import main.Goldilocks;
import main.Permissions;
import modmail.ModmailHub;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import setup.SetupConnector;
import sheets.GoogleSheets;
import utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class CommandListener extends ListenerAdapter {

    public static ExecutorService COMMAND_POOL = Executors.newFixedThreadPool(4);

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

        if (event.getAuthor().isBot()) // if a bot ignore.
            return;

        Guild guild = event.getGuild();
        String guildId;
        int guildRank;
        String guildInfo = SetupConnector.getFieldValue(event.getGuild(), "guildInfo","rank");
        guildRank = guildInfo.isEmpty() ? 1 : Integer.parseInt(guildInfo);

        guildId = event.getGuild().getId();
        String COMMAND_PREFIX = Database.getGuildPrefix(guildId);
        if (Goldilocks.jda.getSelfUser().getId().equals("770776162677817384")) {
            COMMAND_PREFIX = "!";
        }


        String[] split = event.getMessage().getContentRaw().split(" ");
        String alias = split[0].toLowerCase().replace(COMMAND_PREFIX, "");
        String[] args = Arrays.copyOfRange(split, 1, split.length);

        if (!event.getMessage().getContentRaw().startsWith(COMMAND_PREFIX))
            return;

        //Hard coded to prevent mistakes :sunglas:
//        if (Database.isPub(guild)) {
//            if (Goldilocks.commands.getCommand(alias) instanceof CommandChart || Goldilocks.commands.getCommand(alias) instanceof CommandCSV || Goldilocks.commands.getCommand(alias) instanceof CommandSetup
//                    || Goldilocks.commands.getCommand(alias) instanceof CommandParseVc || Goldilocks.commands.getCommand(alias) instanceof CommandPing || Goldilocks.commands.getCommand(alias) instanceof CommandFind
//                    || Goldilocks.commands.getCommand(alias) instanceof CommandHelp || Goldilocks.commands.getCommand(alias) instanceof CommandPlayerHistory || Goldilocks.commands.getCommand(alias) instanceof CommandGoogleSheets) {
//                if (Permissions.hasPermission(event.getMessage().getMember(), Goldilocks.commands.getCommand(alias).getEligibleRoles())) {
//                    COMMAND_POOL.execute(() -> Goldilocks.commands.getCommand(alias).execute(event.getMessage(), alias, args));
//                    Database.logEvent(event.getMember(), Database.EventType.COMMAND, System.currentTimeMillis() / 1000, event.getChannel(), alias);
//                    GoogleSheets.logEvent(guild, GoogleSheets.SheetsLogType.COMMANDS, event.getMember().getEffectiveName(), event.getChannel().getId(),
//                            event.getMember().getId(), alias, event.getMessage().getContentRaw());
//                }
//            }
//            return;
//        }

        List<TextChannel> commandChannels = SetupConnector.getChannels(guild, "commandChannels");
        if (!commandChannels.isEmpty() && !commandChannels.contains(event.getChannel())
                && !event.getMember().getId().equals(Config.get("INSTANCE_OWNER")) && !Database.getGuildRaidCommandChannels(guildId).contains((event.getChannel().getId())) && !alias.equals("cleanup")
                && !(Goldilocks.commands.getCommand(alias) instanceof CommandDrag) && !(Goldilocks.commands.getCommand(alias) instanceof CommandTransferRaid)
                && !(Goldilocks.commands.getCommand(alias) instanceof CommandChangeRaidType) && !(Goldilocks.commands.getCommand(alias) instanceof CommandLockdown)
                && !(Arrays.asList(new String[]{"reply","close","blacklist", "2048"}).contains(alias))) {
            return;
        }

        if (Goldilocks.commands.getCommand(alias) != null) {
            if (guildRank < Goldilocks.commands.getCommand(alias).getGuildRank()) {
                event.getChannel().sendMessage("Unfortunately this command is not available in this server, if you believe this to be an error join the support server.")
                        .setActionRow(Button.link("https://discord.gg/jq7WuZRWqe", "Click Here to Join the Support Server!")).queue();
                return;
            }

            if (!Permissions.hasPermission(event.getMessage().getMember(), Goldilocks.commands.getCommand(alias).getEligibleRoles())) {
                if (!(Goldilocks.commands.getCommand(alias) instanceof CommandDrag || Goldilocks.commands.getCommand(alias) instanceof CommandTransferRaid || Goldilocks.commands.getCommand(alias) instanceof CommandChangeRaidType)) {
                    //event.getMessage().delete().submitAfter(5L, TimeUnit.SECONDS);
                    Utils.sendMessage(event.getChannel(), "You do not have permission to use `" + alias + "`.").delete().submitAfter(5L, TimeUnit.SECONDS);
                }
                return;
            }

            try {
                if (guildRank >= 3) {
                    if (SetupConnector.commandEnabled(guild, Goldilocks.commands.getCommand(alias).getClass())) COMMAND_POOL.execute(() -> Goldilocks.commands.getCommand(alias).execute(event.getMessage(), alias, args));
                    else event.getChannel().sendMessage("Unfortunately this command is not available in this server, if you believe this to be an error join the support server.")
                            .setActionRow(Button.link("https://discord.gg/jq7WuZRWqe", "Click Here to Join the Support Server!")).queue();
                } else {
                    COMMAND_POOL.execute(() -> Goldilocks.commands.getCommand(alias).execute(event.getMessage(), alias, args));
                }
            } catch (Exception e) {e.printStackTrace(); Goldilocks.proxyHelper.nextProxy();}
            Database.logEvent(event.getMember(), Database.EventType.COMMAND, System.currentTimeMillis() / 1000, event.getChannel(), alias);
            GoogleSheets.logEvent(guild, GoogleSheets.SheetsLogType.COMMANDS, event.getMember().getEffectiveName(), event.getChannel().getId(),
                    event.getMember().getId(), alias, event.getMessage().getContentRaw());
        } else {

            if (("reply").equals(alias)) {
                if (Database.getModmailCategory(event.getGuild()) != null
                        && Database.getModmailCategory(event.getGuild()).getTextChannels().contains(event.getChannel())
                        && ModmailHub.isModmailChannel(event.getChannel())) {

                    ModmailHub.modmailReply(event.getMessage(), event.getChannel());
                    Database.logEvent(event.getMember(), Database.EventType.COMMAND, System.currentTimeMillis() / 1000, event.getChannel(), alias);
                    GoogleSheets.logEvent(guild, GoogleSheets.SheetsLogType.COMMANDS, event.getMember().getEffectiveName(), event.getChannel().getId(),
                            event.getMember().getId(), alias, event.getMessage().getContentRaw());
                    Database.incrementField(event.getMember(), "quotaModMail", "totalModMail");
                    Database.logEvent(event.getMember(), Database.EventType.MODMAIL, System.currentTimeMillis() / 1000, event.getChannel(), "reply");

                }
            } else if (("close").equals(alias) || ("blacklist").equals(alias)) {
                if (Database.getModmailCategory(event.getGuild()) != null
                        && Database.getModmailCategory(event.getGuild()).getTextChannels().contains(event.getChannel())
                        && ModmailHub.isModmailChannel(event.getChannel())) {

                    try {
                        if (("close").equals(alias)) ModmailHub.closeModmail(event.getChannel(), event.getMember(), args.length > 0 ? event.getMessage().getContentRaw().split(COMMAND_PREFIX + alias + " ")[1] : "No reason was provided.", event.getMessage());
                        else {
                            ModmailHub.blackListMember(event.getChannel(), event.getMember(), args.length > 0 ? event.getMessage().getContentRaw().split(COMMAND_PREFIX + alias + " ", 2)[1] : "You have been blacklisted from sending mod-mail.", event.getMessage());
                        }
                        Database.logEvent(event.getMember(), Database.EventType.COMMAND, System.currentTimeMillis() / 1000, event.getChannel(), alias);
                        GoogleSheets.logEvent(guild, GoogleSheets.SheetsLogType.COMMANDS, event.getMember().getEffectiveName(), event.getChannel().getId(),
                                event.getMember().getId(), alias, event.getMessage().getContentRaw());

                    } catch (Exception e) {}
                }
            } else {
                //Utils.sendPM(event.getAuthor(), "Invalid command!");
                //event.getMessage().delete().queue();
            }
        }
    }
}
