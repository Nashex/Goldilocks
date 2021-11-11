package utils;

import main.Config;
import main.Goldilocks;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EmoteCache {

    public Guild cacheServer;
    public static List<Emote> emoteCache = new ArrayList<>();

    public EmoteCache() {
        //Cache clear cycle
        Goldilocks.TIMER.scheduleWithFixedDelay(() -> {
            if (cacheServer.getEmotes().size() > 0) {
                List<Emote> emotes = cacheServer.getEmotes();
                emotes.sort(new Comparator<Emote>() {
                    @Override
                    public int compare(Emote e1, Emote e2) {
                        return (int) (e1.getTimeCreated().toEpochSecond() - e2.getTimeCreated().toEpochSecond());
                    }
                });
                //emotes.get(0).delete().queue();
            }
        }, 10L, 5L, TimeUnit.SECONDS);
    }

    public void CacheUserEmote(User user, TextChannel textChannel) {
        try {
            File file;
            String string = Goldilocks.cloudinary.url().type("fetch").imageTag(user.getEffectiveAvatarUrl());
            System.out.println(string);

            //cacheServer.createEmote(String.valueOf(cacheServer.getEmotes().size()), icon).queue(emote -> textChannel.sendMessage(emote.getAsMention()).queue());

        } catch (Exception e) {
            e.printStackTrace();
        }
        //Goldilocks.cloudinary.url().transformation(new Transformation().radius(50)).imageTag("emoteCache/" + user.getId());
    }

    public static void cacheEmote(String id) {
        try {
            Guild guild1 = Goldilocks.jda.getGuildById(Config.get("CACHE_SERVER").toString());
            Icon i = Icon.from(new File("data/items/" + id + ".png"));
            if (getEmoteFromCache(id).isEmpty()) {
                guild1.createEmote(id, i).queue(emote -> {

                });
            }
        } catch (Exception e) { }
    }

    public static String tempCacheEmote(String id) {
        try {
            Guild guild1 = Goldilocks.jda.getGuildById(Config.get("CACHE_SERVER").toString());
            Icon i = Icon.from(new File("data/items/" + id + ".png"));
            if (getEmoteFromCache(id).isEmpty()) {
                Emote emote = guild1.createEmote(id, i).complete();
                emoteCache.add(emote);
                emote.delete().queueAfter(15L, TimeUnit.SECONDS, e -> emoteCache.remove(emote), new ErrorHandler().ignore(ErrorResponse.UNKNOWN_EMOJI));
            }
            return getEmoteFromCache(id);
        } catch (Exception e) { }
        return "";
    }

    public static String getEmoteFromCache(String name) {
        return emoteCache.stream().filter(e -> e.getName().equalsIgnoreCase(name)).map(Emote::getAsMention).findFirst().orElse("");
    }

    public static Emote getEmote(String name) {
        for (Emote emote1 : emoteCache) {
            if (emote1.getName().equalsIgnoreCase(name)) return emote1;
        }
        return null;
    }

    public static String useEmote(String name) {
        Emote emote = getEmote(name);
        if (emote != null) {
            return useEmote(emote);
        } else {
            return "";
        }
    }

    public static String useEmote(Emote emote) {
        emoteCache.remove(emote);
        emote.delete().queueAfter(15L, TimeUnit.SECONDS, null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_EMOJI));
        return emote.getAsMention();
    }

}
