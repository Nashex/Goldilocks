package moderation.punishments;

import main.Database;
import main.Goldilocks;
import moderation.PunishmentConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.ocpsoft.prettytime.PrettyTime;
import setup.SetupConnector;
import sheets.GoogleSheets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Warning extends Punishment {
    private String caseId;
    boolean strict;
    private Message logMessage = null;

    public Warning(Member recipient, Member mod, long timePunished, String reason, String caseId, boolean strict) {
        this.recipient = recipient;
        this.mod = mod;
        this.timeIssued = timePunished;
        this.reason = reason;
        this.caseId = caseId;
        this.strict = strict;
    }

    public Warning(Member recipient, Member mod, String reason, boolean strict) {
        this.recipient = recipient;
        this.mod = mod;
        this.reason = reason;
        this.strict = strict;
        this.timeIssued = System.currentTimeMillis();
    }

    public Warning issue(TextChannel textChannel) {
        recipient.getUser().openPrivateChannel().complete().sendMessage("You have been issued a " + (strict ? "**STRICT**" : "**NORMAL**") + " warning for the following reason:\n" +
                "> " + reason +
                "\nPlease make sure to follow raiding rules in the future. If you believe this to be a mistake contact: " + mod.getAsMention() + " | `" + mod.getEffectiveName() + "`")
                .queue(null, new ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER, e -> {
                    textChannel.sendMessage("Unable to send message to " + recipient.getAsMention() + ". The warning was still applied.").queue();
                }));

        // Log the punishment
        TextChannel punishmentLogs = null;
        String suspensionLogId = SetupConnector.getFieldValue(mod.getGuild(), "guildLogs","suspensionLogChannelId");
        if (!suspensionLogId.equals("0")) punishmentLogs = Goldilocks.jda.getTextChannelById(suspensionLogId);
        if (punishmentLogs != null) logMessage = punishmentLogs.sendMessage(logEmbed().build()).complete();
        log();
        caseId = getCaseId();

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setDescription("You have successfully warned " + recipient.getAsMention() + (logMessage == null ? "" : " see the log [here](" + logMessage.getJumpUrl() + ")"))
                .setColor(Goldilocks.GREEN);
        textChannel.sendMessage(embedBuilder.build()).queue();
        return this;
    }

    public String getCaseId() {
        return Database.query("caseId", "punishments", "punisherId = " + mod.getId() + " AND (timeStarted = (SELECT DATETIME('" + (timeIssued / 1000) + "','unixepoch')))");
    }

    public void log() {
        String recipientId = recipient.getId();
        String memberNamePartition = recipient.getEffectiveName().replaceAll("[^A-Za-z]", "").length() > 3 ?
                recipient.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase().substring(0, 4) : recipient.getEffectiveName().replaceAll("[^A-Za-z]", "").toUpperCase();
        String caseNumber = "'" + memberNamePartition + recipientId.substring(recipientId.length() - 4, recipientId.length() - 1) +
                "' || (SELECT COUNT(*) FROM punishments WHERE userId = " + recipientId + ") || " + (strict ? "'sw'" : "'w'");

        String sql = "INSERT INTO punishments(guildId,userId,punisherId,reason,timeStarted,timeEnded,punishmentType,caseId,roleIds,logMessageId, nickname) VALUES(" + recipient.getGuild().getId() + ", " + recipientId +
                " ," + mod.getId() + ", '" + StringEscapeUtils.escapeSql(reason) + "',(SELECT DATETIME('" + timeIssued / 1000 + "','unixepoch')), 0, '" + (strict ? "strict " : "") + "warning', " +
                caseNumber + ", '0' , " + (logMessage == null ? "0" : logMessage.getId()) + ", '" + StringEscapeUtils.escapeSql(recipient.getEffectiveName()) + "')";

        GoogleSheets.logEvent(mod.getGuild(), GoogleSheets.SheetsLogType.PUNISHMENTS, mod.getEffectiveName(), mod.getId(), recipient.getEffectiveName(), recipient.getId(), "WARNING", reason);

        try (Connection conn = DriverManager.getConnection(Database.dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public EmbedBuilder logEmbed() {
        Date suspendedAt = new Date(timeIssued);
        SimpleDateFormat timeFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm z");
        timeFormat.setTimeZone(TimeZone.getTimeZone("EST"));

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(recipient.getEffectiveName() + " has Recieved a" + (strict ? " Strict" : "") + " Warning")
                .addField("Reason","```\n" + reason + "\n```", false)
                .addField("Moderator Information", mod.getAsMention() + " | `" + mod.getId() + "`", true)
                .addField("Recipient Information", recipient.getAsMention() + " | `" + recipient.getId() + "`", true)
                .addField("Warned At:", "```\n" + timeFormat.format(suspendedAt) + "\n```", false)
                .setColor(Goldilocks.YELLOW)
                .setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

    public String toString() {
        String warningInfo = "";
        Date warnedAt = new Date(timeIssued);
        SimpleDateFormat timeFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm z");
        timeFormat.setTimeZone(TimeZone.getTimeZone("EST"));

        if (!caseId.isEmpty()) {
            warningInfo += "\nCase Id   | " + caseId +
                    "\nFile Size | " +
                    (PunishmentConnector.getCaseEvidence(caseId, mod.getGuild().getId()).isEmpty() ? "Empty" : PunishmentConnector.getCaseEvidence(caseId, mod.getGuild().getId()).size());
        }
        warningInfo += "\nPunisher  | " + (mod == null ? "No longer in server." : mod.getEffectiveName()) +
                "\nIntensity | " + (strict ? "STRICT" : "NORMAL") +
                "\nWarned At | " + (timeIssued == 0 ? "Not Available" : new PrettyTime().format(warnedAt) + " (" + (timeFormat.format(warnedAt)) + ")" +
                "\nReason    | " + reason);

        return warningInfo;
    }
}
