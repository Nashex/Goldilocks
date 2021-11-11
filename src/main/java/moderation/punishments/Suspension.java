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
import utils.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


public class Suspension extends Punishment {

    long duration, timeEnding;
    List<Role> roles = new ArrayList<>();
    String caseId = "", nickName;
    public Message logMessage = null;

    public Suspension(String guildId, String recipientId, String modId, String reason, long timeIssued, long timeEnding, String caseId, List<String> roleIds, String logMessageId, String nickname) {
        this(guildId, recipientId, modId, roleIds, logMessageId);
        this.reason = reason;
        this.timeIssued = timeIssued;
        this.timeEnding = timeEnding;
        this.duration = timeEnding - timeIssued;
        this.caseId = caseId;
        this.nickName = nickname;
    }

    public Suspension(String guildId, String recipientId, String modId, String reason, long timeIssued, long timeEnding, String caseId) {
        this(guildId, recipientId, modId);
        this.reason = reason;
        this.timeIssued = timeIssued;
        this.timeEnding = timeEnding;
        this.duration = timeEnding - timeIssued;
        this.caseId = caseId;
    }

    public Suspension (String guildId, String recipientId, String modId, List<String> roleIds, String logMessageId) {
        this(guildId, recipientId, modId);
        Guild guild = Goldilocks.jda.getGuildById(guildId);
        roles = roleIds.stream().filter(s -> !s.isEmpty()).map(guild::getRoleById).collect(Collectors.toList());

        TextChannel suspensionLogs = null;
        String suspensionLogId = SetupConnector.getFieldValue(guild, "guildLogs","suspensionLogChannelId");
        if (!suspensionLogId.equals("0")) suspensionLogs = Goldilocks.jda.getTextChannelById(suspensionLogId);
        if (suspensionLogs != null && !logMessageId.equals("0")) logMessage = suspensionLogs.retrieveMessageById(logMessageId).complete();
        System.out.println(guild.getName() + " | Retrieved Suspension for: " + recipient);
    }

    public Suspension (String guildId, String recipientId, String modId) {
        Guild guild = Goldilocks.jda.getGuildById(guildId);
        if (guild == null) {
            System.out.println("Failed to Retrieve Suspension for guild: " + guildId + " | User: " + recipientId);
            return;
        }
        recipient = guild.getMemberById(recipientId);
        mod = guild.getMemberById(modId);
    }

    public Suspension(Member recipient, Member mod, String reason, long timeIssued, long duration) {
        this.recipient = recipient;
        this.mod = mod;
        this.reason = reason;
        this.timeIssued = timeIssued;
        this.duration = duration;
        timeEnding = timeIssued + duration;
    }

    public int issueSuspension() {
        Guild guild = mod.getGuild();
        String suspendedRoleId = Database.getGuildInfo(guild, "SuspendedRole");
        Role suspendedRole = guild.getRoleById(suspendedRoleId);
        if (suspendedRole == null) return -2;
        if (PunishmentManager.getSuspension(recipient) != null) return -1;
        if (Utils.getUnHoistedHighestRole(mod).getPosition() <= Utils.getUnHoistedHighestRole(recipient).getPosition()) return -3;

        // Issue the suspension
        // First apply the suspended role then remove the roles below the suspended role
        roles = recipient.getRoles().stream().filter(role -> role.getPosition() < suspendedRole.getPosition() && !role.equals(guild.getBoostRole())).collect(Collectors.toList());
        if (Database.isShatters(guild)) roles = recipient.getRoles();
        guild.addRoleToMember(recipient, suspendedRole).queue();
        roles.forEach(role -> {
            guild.removeRoleFromMember(recipient, role).queue(null, new ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS));
        });

        // Log the suspension
        TextChannel suspensionLogs = null;
        String suspensionLogId = SetupConnector.getFieldValue(guild, "guildLogs","suspensionLogChannelId");
        if (!suspensionLogId.equals("0")) suspensionLogs = Goldilocks.jda.getTextChannelById(suspensionLogId);
        if (suspensionLogs != null) logMessage = suspensionLogs.sendMessage(logEmbed(null).build()).complete();

        // Tell the user they were suspended
        String time = DurationFormatUtils.formatDurationWords(duration , true, true);
        recipient.getUser().openPrivateChannel().complete().sendMessage("You have been suspended from **`" + guild.getName() + "`** for `" + time + "` due to the following reason: \n> " + reason +
                "\nIf you would like to appeal this suspension please contact: " + mod.getAsMention() + " `" + mod.getEffectiveName() + "`").queue(null, new ErrorHandler()
                .ignore(ErrorResponse.CANNOT_SEND_TO_USER));
        // If they are in a voice channel disconnect them
        if (Objects.requireNonNull(recipient.getVoiceState()).inVoiceChannel() && !Database.isShatters(guild)) guild.kickVoiceMember(recipient).queue(null, new ErrorHandler().ignore(ErrorResponse.MISSING_PERMISSIONS));

        // Log the suspension in the database and put in active suspensions
        PunishmentManager.activePunishments.add(this);
        log();
        caseId = getCaseId();
        return 0;
    }

    public String getCaseId() {
        return Database.query("caseId", "punishments", "punisherId = " + mod.getId() + " AND (timeStarted = (SELECT DATETIME('" + (timeIssued / 1000) + "','unixepoch')))");
    }

    public void overrideSuspension() {
        // End the current suspension
        Suspension suspension;
        if ((suspension = PunishmentManager.getSuspension(recipient)) != null) {
            Database.executeUpdate("UPDATE punishments SET timeEnded = " +
                    "(SELECT DATETIME('" + System.currentTimeMillis() / 1000 + "','unixepoch')) WHERE timeEnded > '" + System.currentTimeMillis() / 1000
                    + "' AND punishmentType = 'suspension'");

            if (suspension.logMessage != null) {
                suspension.logMessage.editMessage(suspension.logEmbed(null).setColor(Goldilocks.YELLOW)
                        .setTitle("Suspension for " + recipient.getEffectiveName() + " has been Overridden").build()).queue();
            }
            this.roles = suspension.roles;
            PunishmentManager.activePunishments.remove(suspension);
        }
        issueSuspension();
    }

    public void unsuspend(Member member) {
        if (recipient == null) {
            Database.executeUpdate("UPDATE punishments SET timeEnded = " +
                    "(SELECT DATETIME('" + System.currentTimeMillis() / 1000 + "','unixepoch')) WHERE caseId = '" + caseId + "'");
            return;
        }

        Guild guild = recipient.getGuild();
        String suspendedRoleId = Database.getGuildInfo(guild, "SuspendedRole");
        Role suspendedRole = guild.getRoleById(suspendedRoleId);
        if (suspendedRole == null) return;

        // Make sure we are not re applying deleted roles
        roles = roles.stream().filter(Objects::nonNull).collect(Collectors.toList());

        guild.removeRoleFromMember(recipient, suspendedRole).queue();
        roles.forEach(role -> guild.addRoleToMember(recipient, role).queue());

        // Edit the log message
        if (logMessage != null) logMessage.editMessage(logEmbed(member == null ? guild.getSelfMember() : member).build()).queue();

        // Message the recipient that they are unsuspended
        recipient.getUser().openPrivateChannel().queue(p -> p.sendMessage("You have been unsuspended from `" + guild.getName()
                + "` please read the raiding rules to avoid further suspensions.").queue());

        // Remove this punishment from the arraylist if not auto unsuspended
        if (member != null) PunishmentManager.activePunishments.remove(this);

        // If unsuspended by member log it
        Database.executeUpdate("UPDATE punishments SET timeEnded = " +
                "(SELECT DATETIME('" + System.currentTimeMillis() / 1000 + "','unixepoch')) WHERE caseId = '" + caseId + "'");
        System.out.println(guild.getName() + " | Removed Suspension for: " + recipient);
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
        suspensionInfo +=
                "\nSuspended | " + (timeIssued == 0 ? "Not Available" : new PrettyTime().format(suspendedAt) +
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
        embedBuilder.setTitle(recipient.getEffectiveName() + " has been " + (member == null ? "Suspended" : "Unsuspended"))
                .addField("Reason","```\n" + reason + "\n```", false)
                .addField("Moderator Information", mod.getAsMention() + " | `" + mod.getId() + "`", true)
                .addField("Recipient Information", recipient.getAsMention() + " | `" + recipient.getId() + "`", true);
        if (member != null) embedBuilder.addField("Unsuspender Information", member.getAsMention() + " | `" + member.getId() + "`", true);
        embedBuilder.addField("Recipient Roles", roles.stream().map(Role::getAsMention).collect(Collectors.joining(", ")), false)
                .addField("Suspended At:", "```\n" + timeFormat.format(suspendedAt) + "\n```", true)
                .addField((member == null ? "Suspended Until:" : "Unsuspended At:"), "```\n" + timeFormat.format(suspendedUntil) + "\n```", true)
                .addField("Suspension Length", "```\n" + DurationFormatUtils.formatDurationWords(duration, true, true) + "\n```", true)
                .setColor(member == null ? Goldilocks.RED : Goldilocks.GREEN)
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    private void log() {
        String recipientId = recipient.getId();
        String memberNamePartition = recipient.getEffectiveName().replaceAll("[^A-Za-z]", "").length() > 3 ?
                recipient.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase().substring(0, 4) : recipient.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase();
        String caseNumber = "'" + memberNamePartition + recipientId.substring(recipientId.length() - 4, recipientId.length() - 1) +
                "' || (SELECT COUNT(*) FROM punishments WHERE userId = " + recipientId + ") || 's'";

        String sql = "INSERT INTO punishments(guildId,userId,punisherId,reason,timeStarted,timeEnded,punishmentType,caseId,roleIds,logMessageId, nickname) VALUES(" + recipient.getGuild().getId() + ", " + recipientId +
                " ," + mod.getId() + ", '" + StringEscapeUtils.escapeSql(reason) + "',(SELECT DATETIME('" + timeIssued / 1000 + "','unixepoch')),(SELECT DATETIME('" + timeEnding / 1000 + "','unixepoch')),'suspension', " +
                caseNumber + ", '" + roles.stream().map(Role::getId).collect(Collectors.joining(" ")) + "' , " + (logMessage == null ? "0" : logMessage.getId()) + ", '" + StringEscapeUtils.escapeSql(recipient.getEffectiveName()) + "')";

        GoogleSheets.logEvent(mod.getGuild(), GoogleSheets.SheetsLogType.PUNISHMENTS, mod.getEffectiveName(), mod.getId(), recipient.getEffectiveName(), recipient.getId(), "SUSPENSION", reason);

        try (Connection conn = DriverManager.getConnection(Database.dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

}
