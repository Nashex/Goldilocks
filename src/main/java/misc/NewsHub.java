package misc;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import setup.SetupConnector;

import java.awt.*;
import java.sql.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static main.Database.dbUrl;

public class NewsHub {

    final String URL = "http://remaster.realmofthemadgod.com/?page_id=15";
    ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(1);

    public NewsHub() {
        TIMER.scheduleWithFixedDelay(() -> {

            try {
                Document doc = Jsoup.connect("http://remaster.realmofthemadgod.com/?page_id=15").get();
                List<Element> elements = doc.getElementById("wrapper").children();
                for (Element element : elements) {
                    if (element.id().equals("blog-article")) {
                        Element article = element.child(0);

                        String postId = article.id();

                        String postedOn = element.getElementsByClass("entry-date").get(0).text();

                        String header = element.getElementsByClass("entry-title").html();

                        String imageUrl = (element.getElementsByTag("img").isEmpty() ? "" : element.getElementsByTag("img").get(0).attr("src"));

                        String content = element.getElementsByTag("p").stream().map(Element::ownText).collect(Collectors.joining("\n"));

                        String readMore = element.getElementsByClass("read-more").isEmpty() ? "" : element.getElementsByClass("read-more").get(0).attr("href");
                        String readMoreText = element.getElementsByClass("read-more").isEmpty() ? "" : element.getElementsByClass("read-more").get(0).child(0).text();

                        if (!postId.isEmpty() && !hasPosted(postId)) sendNews(postId, postedOn, header, imageUrl, content, readMore, readMoreText);
                        break;

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }, 0L, 15L, TimeUnit.MINUTES);
    }

    public static void sendNews(String postId, String postedOn, String header, String imageUrl, String content, String readMore, String readMoreText) {

        System.out.println("------------\n\n SENT POST WITH POST ID: " + postId + "\n\n------------");

        addPost(postId);
        List<Guild> guilds = Goldilocks.jda.getGuilds().stream()
                .filter(g -> (SetupConnector.getFieldValue(g, "guildInfo", "rank").equals("3") || Database.isPub(g)) &&
                        !SetupConnector.getFieldValue(g, "guildLogs", "newsChannelId").isEmpty())
                .collect(Collectors.toList());

        for (Guild g : guilds) {
            String textchannelId = SetupConnector.getFieldValue(g, "guildLogs", "newsChannelId");
            TextChannel textChannel = Goldilocks.jda.getTextChannelById(textchannelId);
            if (textChannel != null) {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setAuthor(header, (readMore.isEmpty() ? null : readMore))
                        .setColor(new Color(127, 31, 31))
                        .setDescription(content + (readMore.isEmpty() ? "" : "\n\n**[Read More " + readMoreText + "](" + readMore + ")**"))
                        .setFooter("Published on: " + postedOn);
                if (!imageUrl.isEmpty()) embedBuilder.setImage(imageUrl);
                textChannel.sendMessage(embedBuilder.build()).queue();
            }
        }
    }

    public static void addPost(String postId) {
        String sql = "INSERT INTO posts (postId)" +
                " VALUES (?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, postId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasPosted(String postId) {
        String sql = "SELECT * FROM posts WHERE postId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql);) {

            pstmt.setString(1, postId);
            ResultSet resultSet = pstmt.executeQuery();

            return resultSet.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

}
