package pointSystem;

import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static main.Goldilocks.eventWaiter;

public class PointGui {

    private Member member;
    private Member operator;
    private Guild guild;
    private TextChannel textChannel;
    private PointProfile pointProfile;

    private int pageIndex;

    private Message controlPanel;

    public PointGui (Member member, Member operator, TextChannel textChannel, PointProfile pointProfile) {
        this.member = member;
        this.guild = member.getGuild();
        this.operator = operator;
        this.textChannel = textChannel;
        this.pointProfile = pointProfile;
        pageIndex = 0;

        createGui();
    }

    private void createGui() {
        controlPanel = textChannel.sendMessage(profilePanel().build()).complete();
        controlPanel.addReaction("◀").queue();
        controlPanel.addReaction("▶").queue();
        controlPanel.addReaction("❌").queue();
        reactionHandler();

    }

    public void reactionHandler() {
        eventWaiter.waitForEvent(MessageReactionAddEvent.class, e -> {
            return e.getMessageId().equals(controlPanel.getId()) && e.getUser().equals(operator.getUser()) && ("◀▶❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {
            String reactionEmote = e.getReactionEmote().getEmoji();
            if (reactionEmote.equals("◀") && pageIndex > 0) {

                pageIndex--;
                controlPanel.editMessage(getPage(pageIndex).build()).queue();

            } else if (reactionEmote.equals("▶") && pageIndex < 2) {
                pageIndex++;
                controlPanel.editMessage(getPage(pageIndex).build()).queue();

            } else if (reactionEmote.equals("❌")) {
                controlPanel.delete().queue();
                return;
            }

            e.getReaction().removeReaction(e.getUser()).queue();
            reactionHandler();

        }, 2L, TimeUnit.MINUTES, () -> controlPanel.delete().queue());
    }

    private EmbedBuilder getPage(int pageIndex) {
        if (pageIndex == 0) {
            return profilePanel();
        } else if (pageIndex == 1) {
            return shopPanel();
        } else {
            return infoPanel();
        }
    }

    private EmbedBuilder profilePanel() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setAuthor(member.getEffectiveName() + "'s Point Profile", member.getUser().getAvatarUrl(), member.getUser().getAvatarUrl());
        embedBuilder.setColor(Goldilocks.BLUE);

        Emote coin = Goldilocks.jda.getEmoteById("791034027330240532");

        Emote nest = Goldilocks.jda.getEmoteById("723001215407095899");
        Emote nestChain = Goldilocks.jda.getEmoteById("791041417745006602");
        Emote nestKey = Goldilocks.jda.getEmoteById("767819092467253289");

        String description = "**__Point Balance__** " + coin.getAsMention() +
                "```\n" + pointProfile.getTotalPoints() + " Points" + "```" +
                "\n**__Current Streaks__** 🔥" +
                "\n" +
                "\n" + "**Daily Run**" +
                "```\n" + pointProfile.getDailyRunStreak() + " Day" + (pointProfile.getDailyRunStreak() == 1 ? " " : "s") + " | Completed:" + (pointProfile.isRunStreakMet() ? " 🟢 " : " ⚫ " ) + "```" +
                "\n**Quest Runs (x5 Runs)**" +
                "```\n" + pointProfile.getQuestRunStreak() + " Day" + (pointProfile.getQuestRunStreak() == 1 ? " " : "s") + " | Completed:" + (pointProfile.isQuestStreakMet() ? " 🟢 " : " ⚫ " ) + "```" +
                "\n**Key Pops**" +
                "```\n" + pointProfile.getKeyPopStreak() + " Day" + (pointProfile.getKeyPopStreak() == 1 ? " " : "s") + " | Completed:" + (pointProfile.isKeyStreakMet() ? " 🟢 " : " 🔴 " ) + "```" +
                "\n**__Your Multipliers__** 🌟" +
                "```\nDaily Run:  " + pointProfile.getDailyRunMultiplier() + "x \nQuest Runs: " + pointProfile.getQuestRunMultiplier() + "x \nKey Pops:   " + pointProfile.getKeyRunMultiplier() + "x" + "\n```";

        embedBuilder.setDescription(description);


        embedBuilder.setFooter(member.getEffectiveName() + "'s points in " + guild.getName(), guild.getIconUrl());
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    private EmbedBuilder shopPanel() {

        ShopProfile shop = new ShopProfile();

        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setThumbnail(guild.getIconUrl());

        embedBuilder.setTitle("Point Shop for " + guild.getName());

        Emote coin = Goldilocks.jda.getEmoteById("791034027330240532");

        String description = "**__Point Balance__** " + coin.getAsMention() +
                "```\n" + pointProfile.getTotalPoints() + " Points" + "```" +
                "\n**__Shop__**" +
                "\nHere you can view the prices of various items within the shop.\n";

        AtomicReference<String> locationString = new AtomicReference<>("");
        AtomicReference<String> vanityRoles = new AtomicReference<>("");
        AtomicReference<String> boostString = new AtomicReference<>("");


        shop.shopItems.forEach((shopItem, itemType) -> {
            if (itemType.equals(ShopProfile.ItemType.LOCATION)) {
                String tempString = locationString.get();
                tempString += "\n**" + shopItem.getItemName() + "**" +
                        "```\n" +
                        "\n" + shopItem.getPointCost() + " Points";
                locationString.set(tempString);
            }
            if (itemType.equals(ShopProfile.ItemType.ROLE)) {
                String tempString = vanityRoles.get();
                tempString += "\n**" + shopItem.getItemName() + "**" +
                        "```\n" +
                        "\n" + shopItem.getPointCost() + " Points";
                vanityRoles.set(tempString);
            }
            if (itemType.equals(ShopProfile.ItemType.BOOST)) {
                String tempString = boostString.get();
                tempString += "\n**" + shopItem.getItemName() + "**" +
                        "```\n" +
                        "\n" + shopItem.getPointCost() + " Points";
                boostString.set(tempString);
            }
        });

        if (!locationString.toString().isEmpty()) description += locationString.toString() + "```";
        if (!vanityRoles.toString().isEmpty()) description += vanityRoles.toString() + "```";
        if (!boostString.toString().isEmpty()) description += boostString.toString() + "```";

        //embedBuilder.setDescription(description);

        embedBuilder.setDescription("```\n" +
                "\n                              " +
                "\n ---------------------------- " +
                "\n      Under Construction      " +
                "\n ---------------------------- " +
                "\n                              " +
                "\n```");

        embedBuilder.setFooter(guild.getName() + "'s Shop", guild.getIconUrl());
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

    private EmbedBuilder infoPanel() {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setColor(Goldilocks.GOLD);
        embedBuilder.setThumbnail(guild.getIconUrl());

        embedBuilder.setTitle("What are points?");

        embedBuilder.setFooter(guild.getName() + "'s Shop", guild.getIconUrl());
        embedBuilder.setTimestamp(new Date().toInstant());

        return embedBuilder;
    }

}
