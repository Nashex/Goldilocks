package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.*;
import java.util.Date;

import static main.Database.dbUrl;

public class CommandRemovePrefix extends Command {
    public CommandRemovePrefix() {
        setAliases(new String[] {"prefix"});
        setEligibleRoles(new String[] {"vetRl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);
    }

    @Override
    public void execute(Message msg, String alias, String[] args) {
        TextChannel textChannel = msg.getTextChannel();

        Member member = msg.getMember();
        if (member == null) return;

        String oldName = "";
        if (!(oldName = hasPrefix(member)).isEmpty()) {
            reAddPrefix(member, msg.getTextChannel(), oldName);
            return;
        }

        //removePrefix(member, newName);

    }

    private void reAddPrefix(Member member, TextChannel textChannel, String name) {

        deletePrefix(member);
        try {
            member.modifyNickname(name + member.getEffectiveName()).complete();
        } catch (Exception e) {}

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.GREEN)
                .setDescription("Your prefix has been added back!");

        textChannel.sendMessage(embedBuilder.build()).queue();

    }

    private String hasPrefix(Member member) {
        String sql = "SELECT prefix FROM removedPrefixes WHERE guildId = " + member.getGuild().getId() + " AND userId = " + member.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) return resultSet.getString("originalName");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void deletePrefix(Member member) {
        String sql = "DELETE FROM removedPrefixes WHERE guildId = " + member.getGuild().getId() + " AND userId = " + member.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removePrefix(Member member, String newName) {
        String sql = "INSERT INTO removedPrefixes (guildId,userId,prefix) VALUES (?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, member.getGuild().getId());
            pstmt.setString(2, member.getId());
            pstmt.setString(3, member.getEffectiveName().split(" ")[0].replaceAll("^[A-Za-z]", ""));

            pstmt.executeUpdate();

            try {
                member.modifyNickname(newName).complete();
            } catch (Exception e) {}

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public EmbedBuilder getInfo() {
        String aliases = "";
        for (String alias : getAliases())
            aliases = aliases + " " + alias;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Remove Prefix");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Veteran Raid Leader\n";
        commandDescription += "Syntax: .rprefix\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nRemoves your prefix." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
