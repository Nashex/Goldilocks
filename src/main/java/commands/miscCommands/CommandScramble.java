package commands.miscCommands;

import commands.Command;
import commands.CommandHub;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;

import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static main.Database.dbUrl;

public class CommandScramble extends Command {
    public CommandScramble() {
        setAliases(new String[] {"scramble"});
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
        if (!(oldName = hasName(member)).isEmpty()) {
            resetName(member, msg.getTextChannel(), oldName);
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("How would you like me to scramble your name?")
                .setDescription("By choosing one of the options your name will be changed to a random one within the theme you selected. " +
                        "If you would like to change your name back simple run the command again!")
                .setColor(Goldilocks.WHITE);

        Message message = textChannel.sendMessage(embedBuilder.build()).setActionRow(
                Button.primary("default", "Default Names"),
                Button.primary("dirty", "Dirty Names"),
                Button.primary("scramble", "Scramble My Name"),
                Button.danger("cancel", "Cancel")
        ).complete();

        String[] defaultNames = new String[]{"Utanu", "Gharr", "Yimi", "Idrae", "Odaru", "Scheev", "Zhiar", "Itani", "Serl", "Oeti",
                "Tiar", "Issz", "Oshyu", "Deyst", "Oalei", "Vorv", "Iatho", "Uoro", "Urake", "Eashy", "Queq", "Rayr", "Tal", "Drac",
                "Yangu", "Eango", "Rilr", "Ehoni", "Risrr", "Sek", "Eati", "Laen", "Eendi", "Ril", "Darq", "Sues", "Radph", "Orothi",
                "Vorck", "SayIt", "Iawa", "Iri", "Lauk", "Lorz"};

        String[] dirtyNames = new String[]{"E. Rex Sean", "E. Normous Peter", "Eileen Ulick", "Jenny Tayla", "Phil Accio", "Stella Virgin", "Lou Sirr", "Sal Ami", "Lance Lyde", "Ben Dover", "Mel Keetehts"};

        Random random = new Random();

        Goldilocks.eventWaiter.waitForEvent(ButtonClickEvent.class, e -> {
            return e.getUser().equals(msg.getAuthor()) && Objects.equals(e.getMessage(), message);
        }, e -> {

            e.deferEdit().queue();
            String control = e.getComponentId();

            String prefix = member.getEffectiveName().split(" ")[0].replaceAll("^[A-Za-z]", "");

            if (control.equals("cancel")) {
                message.delete().queue();
                return;
            }

            String newName = "";
            if (control.equals("default")) {
                newName = defaultNames[random.nextInt(defaultNames.length)];
            }

            if (control.equals("dirty")) {
                newName = dirtyNames[random.nextInt(dirtyNames.length)];
            }

            if (control.equals("scramble")) {
                List<Character> characters = new ArrayList<>();
                String memberName = member.getEffectiveName().split(" ")[0].replaceAll("[^A-Za-z]", "");
                for (char c : memberName.toCharArray()) characters.add(c);
                for (int i = 0; i < memberName.length(); i++) {
                    int curIndex = random.nextInt(characters.size());
                    newName += characters.get(curIndex);
                    characters.remove(curIndex);
                }
            }

            changeName(member, newName);
            message.editMessage(new EmbedBuilder().setColor(Goldilocks.LIGHTBLUE).setDescription("Everyone please welcome " + newName + "!").build()).setActionRows().queue();

        }, 10L, TimeUnit.MINUTES, () -> {
            message.delete().queue();
        });

    }

    private void resetName(Member member, TextChannel textChannel, String name) {

        deleteName(member);
        try {
            member.modifyNickname(name).complete();
        } catch (Exception e) {}

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.GREEN)
                .setDescription("Your name has been set back to " + name + "!");

        textChannel.sendMessage(embedBuilder.build()).queue();

    }

    private String hasName(Member member) {
        String sql = "SELECT originalName FROM scrambledNames WHERE guildId = " + member.getGuild().getId() + " AND userId = " + member.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            ResultSet resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) return resultSet.getString("originalName");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void deleteName(Member member) {
        String sql = "DELETE FROM scrambledNames WHERE guildId = " + member.getGuild().getId() + " AND userId = " + member.getId();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();) {

            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void changeName(Member member, String newName) {
        String sql = "INSERT INTO scrambledNames (guildId,userId,originalName)" +
                " VALUES (?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, member.getGuild().getId());
            pstmt.setString(2, member.getId());
            pstmt.setString(3, member.getEffectiveName());

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
        embedBuilder.setTitle("Command: Scramble");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Veteran Raid Leader\n";
        commandDescription += "Syntax: .scramble\n";
        commandDescription +="Aliases:" + aliases + "\n";
        commandDescription += "\nScramble some eggs." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }
}
