package moderation.punishments.controlpanels;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ControlPanelUtils {

    public static String[] times = {"w", "d", "mo", "h", "mi", "s"};

    public static long getTime(int multiplier, String string) {
        long length = multiplier;
        String prefix = Arrays.stream(times).filter(string::startsWith).collect(Collectors.toList()).get(0);
        switch (prefix) {
            case "w":
                length *= 604800;
                break;
            case "mo":
                length *= 2628000;
                break;
            case "h":
                length *= 3600;
                break;
            case "d":
                length *= 86400;
                break;
            case "mi":
                length *= 60;
                break;
            default: //Seconds
                break;
        }
        return length * 1000; // Convert to millis
    }

    public static void addEmojis(Message message, String... emojis) {
        for (String emoji : emojis) message.addReaction(emoji).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
    }

}
