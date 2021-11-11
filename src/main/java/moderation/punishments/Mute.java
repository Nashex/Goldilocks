package moderation.punishments;

import main.Database;
import main.Goldilocks;
import moderation.PunishmentConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ocpsoft.prettytime.PrettyTime;
import setup.SetupConnector;
import sheets.GoogleSheets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Mute extends Punishment {

    long duration, timeEnding;
    List<Role> roles = new ArrayList<>();
    String caseId = "", nickName;
    public Message logMessage = null;

    public Mute (String guildId, String recipientId, String modId, String reason, long timeIssued, long timeEnding, String caseId, List<String> roleIds, String logMessageId, String nickname) {
        this(guildId, recipientId, modId, roleIds, logMessageId);
        this.reason = reason;
        this.timeIssued = timeIssued;
        this.timeEnding = timeEnding;
        this.duration = timeEnding - timeIssued;
        this.caseId = caseId;
        this.nickName = nickname;
    }

    public Mute (String guildId, String recipientId, String modId, String reason, long timeIssued, long timeEnding, String caseId) {
        this(guildId, recipientId, modId);
        this.reason = reason;
        this.timeIssued = timeIssued;
        this.timeEnding = timeEnding;
        this.duration = timeEnding - timeIssued;
        this.caseId = caseId;
    }

    public Mute (String guildId, String recipientId, String modId, List<String> roleIds, String logMessageId) {
        this(guildId, recipientId, modId);
        Guild guild = Goldilocks.jda.getGuildById(guildId);
        roles = roleIds.stream().filter(s -> !s.isEmpty()).map(guild::getRoleById).collect(Collectors.toList());

        TextChannel suspensionLogs = null;
        String suspensionLogId = SetupConnector.getFieldValue(guild, "guildLogs","suspensionLogChannelId");
        if (!suspensionLogId.equals("0")) suspensionLogs = Goldilocks.jda.getTextChannelById(suspensionLogId);
        if (suspensionLogs != null && !logMessageId.equals("0")) {
            try {
                logMessage = suspensionLogs.retrieveMessageById(logMessageId).complete();
            } catch (Exception ignored) { }
        }
        System.out.println(guild.getName() + " | Retrieved Mute for: " + recipient);
    }

    public Mute(String guildId, String recipientId, String modId) {
        Guild guild = Goldilocks.jda.getGuildById(guildId);
        if (guild == null) {
            System.out.println("Failed to Retrieve Mute for guild: " + guildId + " | User: " + recipientId);
            return;
        }
        recipient = guild.getMemberById(recipientId);
        mod = guild.getMemberById(modId);
    }

    public Mute (Member recipient, Member mod, String reason, long timeIssued, long duration) {
        this.recipient = recipient;
        this.mod = mod;
        this.reason = reason;
        this.timeIssued = timeIssued;
        this.duration = duration;
        timeEnding = timeIssued + duration;
    }

    public int issueMute() {
        Guild guild = mod.getGuild();
        String mutedRoleId = Database.getGuildInfo(guild, "mutedRole");
        Role mutedRole = guild.getRoleById(mutedRoleId);
        if (mutedRole == null) return -2;
        if (PunishmentManager.getMute(recipient) != null) return -1;

        // Issue the mute
        guild.addRoleToMember(recipient, mutedRole).queue();

        // Log the mute
        TextChannel punishmentLogs = null;
        String punishmentLogId = SetupConnector.getFieldValue(guild, "guildLogs","suspensionLogChannelId");
        if (!punishmentLogId.equals("0")) punishmentLogs = Goldilocks.jda.getTextChannelById(punishmentLogId);
        if (punishmentLogs != null) logMessage = punishmentLogs.sendMessage(logEmbed(null).build()).complete();

        // Tell the user they were muted
        String time = DurationFormatUtils.formatDurationWords(duration , true, true);
        recipient.getUser().openPrivateChannel().complete().sendMessage("You have been muted from **`" + guild.getName() + "`** for `" + time + "` due to the following reason: \n> " + reason +
                "\nIf you would like to appeal this mute please contact: " + mod.getAsMention() + " `" + mod.getEffectiveName() + "` via dms.").queue();

        // Log the mute in the database and put in active suspensions
        PunishmentManager.activePunishments.add(this);
        log();

        return 0;
    }

    public void overrideMute() {
        // End the current mute
        Mute mute;
        if ((mute = PunishmentManager.getMute(recipient)) != null) {
            Database.executeUpdate("UPDATE punishments SET timeEnded = " +
                    "(SELECT DATETIME('" + System.currentTimeMillis() / 1000 + "','unixepoch')) WHERE timeEnded > '" + System.currentTimeMillis() / 1000
                    + "' AND punishmentType = 'mute'");

            if (mute.logMessage != null) {
                if (mute.logMessage.getAuthor().equals(Goldilocks.jda.getSelfUser())) mute.logMessage.editMessage(mute.logEmbed(null).setColor(Goldilocks.YELLOW)
                        .setTitle("Mute for " + recipient.getEffectiveName() + " has been Overridden").build()).queue(null, new ErrorHandler().ignore(ErrorResponse.INVALID_AUTHOR_EDIT));
            }
            PunishmentManager.activePunishments.remove(mute);
        }

        issueMute();
    }

    public void unmute(Member member) {
        Guild guild;
        try {
            guild = recipient.getGuild();
        } catch (Exception e) {
            return;
        }
        String mutedRoleId = Database.getGuildInfo(guild, "mutedRole");
        Role mutedRole = guild.getRoleById(mutedRoleId);
        if (mutedRole == null) return;

        // Make sure we are not updating deleted roles
        roles = roles.stream().filter(Objects::nonNull).collect(Collectors.toList());

        // Remove the muted role from the user
        guild.removeRoleFromMember(recipient, mutedRole).queue();

        // Edit the log message
        if (logMessage != null) logMessage.editMessage(logEmbed(member == null ? guild.getSelfMember() : member).build()).queue();

        // Message the recipient that they are unmuted
        recipient.getUser().openPrivateChannel().queue(p -> p.sendMessage("You have been unmuted from `" + guild.getName()
                + "` please read the rules to avoid further mutes.").queue());

        // Remove this punishment from the arraylist if not auto unsuspended
        if (member != null) PunishmentManager.activePunishments.remove(this);

        // If unsuspended by member log it
        Database.executeUpdate("UPDATE punishments SET timeEnded = " +
                "(SELECT DATETIME('" + System.currentTimeMillis() / 1000 + "','unixepoch')) WHERE caseId = '" + caseId + "'");
        System.out.println(guild.getName() + " | Removed Mute for: " + recipient);
    }

    public String toString() {
        String suspensionInfo = "";
        Date suspendedAt = new Date(timeIssued);
        Date suspendedUntil = new Date(timeEnding);
        SimpleDateFormat timeFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm z");
        timeFormat.setTimeZone(TimeZone.getTimeZone("EST"));

        if (!caseId.isEmpty()) {
            suspensionInfo += "\nCase Id   | " + caseId +
                    "\nFile Size | " +
                    (PunishmentConnector.getCaseEvidence(caseId, mod.getGuild().getId()).isEmpty() ? "Empty" : PunishmentConnector.getCaseEvidence(caseId, mod.getGuild().getId()).size());
        }
        suspensionInfo += "\nPunisher  | " + (mod == null ? "No longer in server." : mod.getEffectiveName());
        suspensionInfo += "\nMuted     | " + (timeIssued == 0 ? "Not Available" : new PrettyTime().format(suspendedAt) +
                "\nEnded At  | " + timeFormat.format(suspendedUntil) +
                "\nDuration  | " + DurationFormatUtils.formatDurationWords(duration, true, true) +
                "\nReason    | " + reason);

        return suspensionInfo;
    }

    public EmbedBuilder logEmbed(Member member) {
        Date suspendedAt = new Date(timeIssued);
        Date suspendedUntil = new Date(timeEnding);
        SimpleDateFormat timeFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm z");
        timeFormat.setTimeZone(TimeZone.getTimeZone("EST"));

        roles = roles.stream().filter(Objects::nonNull).collect(Collectors.toList());

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(recipient.getEffectiveName() + " has been " + (member == null ? "Muted" : "Unmuted"))
                .addField("Reason","```\n" + reason + "\n```", false)
                .addField("Moderator Information", mod.getAsMention() + " | `" + mod.getId() + "`", true)
                .addField("Recipient Information", recipient.getAsMention() + " | `" + recipient.getId() + "`", true);
        if (member != null) embedBuilder.addField("Unmuter Information", member.getAsMention() + " | `" + member.getId() + "`", true);
        embedBuilder.addField("Muted At:", "```\n" + timeFormat.format(suspendedAt) + "\n```", true)
                .addField((member == null ? "Muted Until:" : "Muted At:"), "```\n" + timeFormat.format(suspendedUntil) + "\n```", true)
                .addField("Mute Length", "```\n" + DurationFormatUtils.formatDurationWords(duration, true, true) + "\n```", true)
                .setColor(member == null ? Goldilocks.RED : Goldilocks.GREEN)
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private void log() {
        String recipientId = recipient.getId();
        String memberNamePartition = recipient.getEffectiveName().replaceAll("[^A-Za-z]", "").length() > 3 ?
                recipient.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase().substring(0, 4) : recipient.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase();
        String caseNumber = "'" + memberNamePartition + recipientId.substring(recipientId.length() - 4, recipientId.length() - 1) +
                "' || (SELECT COUNT(*) FROM punishments WHERE userId = " + recipientId + ")" + " || 'm'";

        String sql = "INSERT INTO punishments(guildId,userId,punisherId,reason,timeStarted,timeEnded,punishmentType,caseId,roleIds,logMessageId, nickname) VALUES(" + recipient.getGuild().getId() + ", " + recipientId +
                " ," + mod.getId() + ", '" + StringEscapeUtils.escapeSql(reason) + "',(SELECT DATETIME('" + timeIssued / 1000 + "','unixepoch')),(SELECT DATETIME('" + timeEnding / 1000 + "','unixepoch')),'mute', " +
                caseNumber + ", '" + roles.stream().map(Role::getId).collect(Collectors.joining(" ")) + "' , " + (logMessage == null ? "0" : logMessage.getId()) + ", '" + StringEscapeUtils.escapeSql(recipient.getEffectiveName()) + "')";

        GoogleSheets.logEvent(mod.getGuild(), GoogleSheets.SheetsLogType.PUNISHMENTS, mod.getEffectiveName(), mod.getId(), recipient.getEffectiveName(), recipient.getId(), "MUTE", reason);

        try (Connection conn = DriverManager.getConnection(Database.dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

}
