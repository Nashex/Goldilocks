package earlylocation;

import main.Goldilocks;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.sql.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

public class EarlyLocationConnector {
    private static ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(1);
    private static String dbUrl = "jdbc:sqlite:database.db";

    public static boolean userExists(User user) {
        String sql = "SELECT * FROM earlyLocationUsers WHERE userID = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);

             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, user.getId());

            ResultSet resultSet = pstmt.executeQuery();

            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void createUser(User user) {
        String sql = "INSERT INTO earlyLocationUsers (userId,lastUse,numUses) VALUES(?,0,0)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setString(1, user.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void useLocation(User user) {
        if (!userExists(user)) createUser(user);
        String sql = "UPDATE earlyLocationUsers SET lastUse = ?, numUses = numUses + 1";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setTime(1, new Time(System.currentTimeMillis()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isEligible(User user) {
        if (!userExists(user)) createUser(user);
        String sql = "SELECT lastUse FROM earlyLocationUsers WHERE userID = ?";

        if (user.getMutualGuilds().stream().filter(guild -> !guild.getId().equalsIgnoreCase("514788290809954305")
                && !guild.getId().equalsIgnoreCase("806075806001397820")).collect(Collectors.toList()).isEmpty()) {
            try {
                if (Goldilocks.jda.getGuilds().size() < 100) user.openPrivateChannel().complete().sendMessage("Looks like you are eligible to receive early location! " +
                        "If you would like to invite me, Goldilocks, to your server with this link <http://goldi.tech> to receive early loc every 2 hours!").queue();
            } catch (Exception e) {} //Private dms
            return false;
        }

        boolean isEligible = false;
        long MIN_USE = 7200L;
        long lastUse = 0L;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, user.getId());
            ResultSet resultSet = pstmt.executeQuery();
            Time time = resultSet.getTime(1);
            lastUse = (System.currentTimeMillis() - time.getTime()) / 1000; // Seconds
            System.out.println(lastUse);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (MIN_USE < lastUse) {
            isEligible = true;
            useLocation(user);
            System.out.println("Early loc used by: " + user.getId());
        } else {
            user.openPrivateChannel().complete().sendMessage("Looks like you have `" +
                    DurationFormatUtils.formatDuration((MIN_USE - lastUse) * 1000, "H' hours 'm' minutes 's' seconds'", true)
                    + "` before you can use early location again.").queue();
            System.out.println("Early loc attempted use by: " + user.getId());
        }
        return isEligible;
    }

}
