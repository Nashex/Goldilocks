package moderation.punishments;

import main.Database;
import main.Goldilocks;
import moderation.PunishmentConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang.StringEscapeUtils;
import org.ocpsoft.prettytime.PrettyTime;
import sheets.GoogleSheets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Note extends Punishment {
    String caseId;

    public Note(Member recipient, Member mod, long timePunished, String reason, String caseId) {
        this.recipient = recipient;
        this.mod = mod;
        this.timeIssued = timePunished;
        this.reason = reason;
        this.caseId = caseId;
    }

    public Note(Member recipient, Member mod, String reason) {
        this.recipient = recipient;
        this.mod = mod;
        this.reason = reason;
        this.timeIssued = System.currentTimeMillis();
    }

    public Note issue(TextChannel textChannel) {
        // Log the note
        log();
        caseId = getCaseId();

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setDescription("Successfully added note to " + recipient.getAsMention())
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
                "' || (SELECT COUNT(*) FROM punishments WHERE userId = " + recipientId + ") || 'n'";

        String sql = "INSERT INTO punishments(guildId,userId,punisherId,reason,timeStarted,timeEnded,punishmentType,caseId,roleIds,logMessageId, nickname) VALUES(" + recipient.getGuild().getId() + ", " + recipientId +
                " ," + mod.getId() + ", '" + StringEscapeUtils.escapeSql(reason) + "',(SELECT DATETIME('" + timeIssued / 1000 + "','unixepoch')), 0, 'note', " +
                caseNumber + ", '0' , 0 , '" + StringEscapeUtils.escapeSql(recipient.getEffectiveName()) + "')";

        GoogleSheets.logEvent(mod.getGuild(), GoogleSheets.SheetsLogType.PUNISHMENTS, mod.getEffectiveName(), mod.getId(), recipient.getEffectiveName(), recipient.getId(), "NOTE", reason);

        try (Connection conn = DriverManager.getConnection(Database.dbUrl);
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public String toString() {
        String noteInfo = "";
        Date notedAt = new Date(timeIssued);
        SimpleDateFormat timeFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm z");
        timeFormat.setTimeZone(TimeZone.getTimeZone("EST"));

        if (!caseId.isEmpty()) {
            noteInfo += "Case Id   | " + caseId + "\n";
            if (mod != null) noteInfo += "File Size | " + (PunishmentConnector.getCaseEvidence(caseId, mod.getGuild().getId()).isEmpty() ? "Empty" : PunishmentConnector.getCaseEvidence(caseId, mod.getGuild().getId()).size()) + "\n";
        }

        noteInfo += "Note By   | " + (mod == null ? "No longer in server." : mod.getEffectiveName()) + "\n" +
                "Noted     | " +  (timeIssued == 0 ? "Not Available" : new PrettyTime().format(notedAt) + " (" + (timeFormat.format(notedAt))) + ")" + "\n" +
                "Content   | " + reason;

        return noteInfo;
    }
}
