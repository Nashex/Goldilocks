package utils;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.*;
import java.util.stream.Collectors;

public class MemberSearch {

    public static List<Member> memberSearch (Message msg, String[] args) {

        System.out.println(Arrays.toString(args));
        List<Member> memberList = new ArrayList<>();
        Guild guild = msg.getGuild();
        TextChannel textChannel = msg.getTextChannel();
        if (msg.getMentionedMembers().size() != 0) {
            memberList.addAll(msg.getMentionedMembers());
            return memberList;
        } else if (args.length > 0 && args[0].contains("#")) {
            System.out.println("Searching tags");
            try {
                for (String tag : args) {
                    Member member = guild.getMemberByTag(tag);
                    if (member != null) memberList.add(member);
                }
            } catch (Exception e) { }
            return memberList;
        } else if (!Arrays.asList(args).stream().filter(s -> s.replaceAll("[^0-9]", "").length() > 10).collect(Collectors.toList()).isEmpty()){
            System.out.println("Searching nums");
            try {
                for (String id : args) {
                    net.dv8tion.jda.api.entities.Member member = guild.getMemberById(id);
                    if (member != null) memberList.add(member);
                    String[] expel = Database.getExpel(id, guild);
                    if (expel.length == 3) {
                        textChannel.sendMessage(expelledMemberEmbed(id, expel, guild).build()).queue();
                    }
                }
            } catch (Exception e) { }
            return memberList;
        } else {
            List<Member> guildMembers = guild.getMembers().stream().filter(member -> member.getNickname() != null).distinct().collect(Collectors.toList());
            //List<String> guildMemberNames = guildMembers.stream().map(member -> member.getEffectiveName().replace(",", "").split( " , ")[0].replaceAll("[^A-Za-z]", "")).collect(Collectors.toList());
            Map<Member, String> memberNameHashMap = guildMembers.stream().distinct()
                    .collect(Collectors.toMap(member -> member, member -> member.getEffectiveName().replaceAll("[^A-Za-z]", "").toLowerCase()));
            List<Member> guildAltMembers = guild.getMembers().stream().filter(member -> member.getEffectiveName().split("[|]").length > 1).collect(Collectors.toList());
            //for (Member member : guildAltMembers) System.out.println(member.getEffectiveName());
            Map<Member, String> altNameHashMap = guildAltMembers.stream()
                    .collect(Collectors.toMap(member -> member, member -> member.getEffectiveName().split("[|]")[1].replaceAll("[^A-Za-z]", "").toLowerCase()));
            for (String memberName : args) {
                boolean found = false;
                Member member = memberSearch(memberName, guild);
                if (member != null) {
                    System.out.println("Searching " + memberName);
                    memberList.add(member);
                    found = true;
                } else if (!memberNameHashMap.entrySet().stream().filter(stringMemberEntry -> stringMemberEntry.getValue().toLowerCase()
                        .contains((memberName.length() > 6 ? memberName.toLowerCase().substring(1, 7) : memberName.toLowerCase()))).collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())).isEmpty()) {
                    System.out.println("Searching " + memberName);
                    Map<Member, String> potentialMemberNameHashMap = memberNameHashMap.entrySet()
                            .stream().filter(stringMemberEntry -> stringMemberEntry.getValue().toLowerCase().contains((memberName.length() > 6 ? memberName.toLowerCase().substring(1, 7) : memberName.toLowerCase())))
                            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
                    if (potentialMemberNameHashMap.size() > 20) {
                        Utils.errorMessage("Could not Retrieve User", "Too many users were found for " + memberName, textChannel, 10L);
                        //potentialMemberNameHashMap.forEach((member, s) -> System.out.println(member.getEffectiveName()));
                        //return memberList;
                    }
                    if (potentialMemberNameHashMap.size() == 1) {
                        potentialMemberNameHashMap.forEach((m, s) -> memberList.add(m));
                        found = true;
                        //return memberList;
                    } else {
                        //textChannel.sendMessage(potentialMembersEmbed(potentialMemberNameHashMap).build()).queue();
                        System.out.println(potentialMemberNameHashMap.keySet().toString());
                        Goldilocks.TIMER.execute(() -> couldNotFind(memberName, guild, textChannel, potentialMemberNameHashMap));
                        found = true;
                        //return memberList;
                    }

                } else {
                    Goldilocks.TIMER.execute(() -> couldNotFind(memberName, guild, textChannel, new HashMap<>()));
                }

                String expelName = Database.isExpelled(memberName.toLowerCase(), guild);
                if (!expelName.isEmpty()) {
                    textChannel.sendMessage(new EmbedBuilder().setColor(Goldilocks.RED).setDescription(expelName).build()).queue();
                }

            }
        }

        return memberList;

    }

    private static void couldNotFind(String memberName, Guild guild, TextChannel textChannel, Map<Member, String> potentialMemberNameHashMap) {
        int[] parses = Database.getParses(memberName.toLowerCase().replaceAll("[^A-Za-z]", ""), guild);

        List<Member> globalSearch = new ArrayList<>();
        List<Guild> guilds = Goldilocks.jda.getGuilds().stream().filter(g -> Database.getGuildInfo(guild, "rank").equals("3")).collect(Collectors.toList());
        for (Guild g : guilds) globalSearch.add(memberSearch(memberName, g));
        globalSearch = globalSearch.stream().filter(Objects::nonNull).collect(Collectors.toList());

        StringBuilder potentialMembers = new StringBuilder();
        potentialMemberNameHashMap.forEach((member, s) -> potentialMembers.append("**`" + String.format("%-" +
                        potentialMemberNameHashMap.keySet().stream().map(k -> k.getEffectiveName().length()).max(Integer::compareTo).get() + "s",
                member.getEffectiveName().replaceAll("[^A-Za-z\\|. ]", "")) + ":`**" + member.getAsMention().replace("!", "") + "\n"));

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Goldilocks.YELLOW)
                .setDescription("Unable to match user with the name: `" + memberName + "`");

        if (parses[0] != 0) embedBuilder.addField("Crashes", "This user has crashed a total of `" + parses[0] + "` runs. Exactly `" + parses[1] + "` of those crashes were in " + guild.getName(), false);
        if (!globalSearch.isEmpty()) embedBuilder.addField("Global Search", "I found a similar member matched to ***" + memberName + "***, in the following Servers:\n" +
                globalSearch.stream().map(m -> String.format("`%s` | `%s` | `%s` ", m.getEffectiveName().replaceAll("[^A-Za-z |]", ""), m.getId(), m.getGuild().getName())).collect(Collectors.joining("\n")), false);
        if (!potentialMemberNameHashMap.isEmpty()) embedBuilder.addField("Potential Member Search", potentialMembers.toString(), false);

        textChannel.sendMessage(embedBuilder.build()).queue();
    }

    public static Member memberSearch(String string, Guild guild) {
        for (Member member : guild.getMembers()) {
            if (member.getId().equals(string.replaceAll("[^0-9]", "")))
                return member;
            if (member.getUser().getAsTag().equals(string))
                return member;
            if (member.getNickname() != null) {
                String memberName = member.getEffectiveName().replaceAll("[^A-Za-z|]", "").toLowerCase();
                if (Arrays.asList(memberName.split("[|]")).contains(string.toLowerCase()))
                    return member;
            }
        }
        return null;
    }

    public static List<Member> potentialMemberSearch(String string, Guild guild, boolean caseSensitive) {
        String playerName = caseSensitive ? string.toLowerCase() : string;
        List<Member> members = new ArrayList<>();
        Map<Member, String> memberHashMap = guild.getMembers().stream().distinct().collect(Collectors.toMap(member -> member, member -> caseSensitive ? member.getEffectiveName() : member.getEffectiveName().toLowerCase()));
        for (Map.Entry<Member, String> nameSet : memberHashMap.entrySet()) {
            List<String> names = new ArrayList<>(Arrays.asList(nameSet.getValue().replaceAll("[^A-Za-z|]", "").split("\\|")));
            List<String> additionalNames = new ArrayList<>();
            for (String name : names) additionalNames.addAll(iLCorrection(name));
            names.addAll(additionalNames);
            if (names.stream().anyMatch(s -> s.contains(playerName))) members.add(nameSet.getKey());
        }
        return members;
    }

    public static List<String> iLCorrection(String s) {
        List<String> permutations = new ArrayList<>();
        if (s.contains("i") || s.contains("l")) {
            char[] chars = s.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] == 'i' || chars[i] == 'l') {
                    char temp = chars[i];
                    chars[i] = (chars[i] == 'i' ? 'l' : 'i');
                    permutations.add(String.valueOf(chars));
                    chars[i] = temp;
                }
            }

            for (int i = chars.length - 1; i >= 0; i--) {
                if (chars[i] == 'i' || chars[i] == 'l') {
                    char temp = chars[i];
                    chars[i] = (chars[i] == 'i' ? 'l' : 'i');
                    permutations.add(String.valueOf(chars));
                    chars[i] = temp;
                }
            }
        }
        return permutations;
    }

    public static EmbedBuilder expelledMemberEmbed(String name, String[] expel, Guild guild) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Expulsion found for: " + name)
                .setColor(Goldilocks.RED)
                .setDescription(
                        "**`Mod Info  `: **" + (guild.getMemberById(expel[2]) == null ? "Not in Server" :
                        guild.getMemberById(expel[2]).getAsMention()) + " | " + expel[2] + "\n" +
                        "**`User Info `: **" + (guild.getMemberById(expel[1]) == null ? "Not in Server" :
                                guild.getMemberById(expel[1]).getAsMention()) + " | " + expel[1] + "\n" +
                        "**`Name      `: **" + expel[0] + "\n");
        return embedBuilder;
    }

    public static EmbedBuilder potentialMembersEmbed (Map <Member, String> potentialMemberNameHashMap) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle(potentialMemberNameHashMap.size() + " Potential Members Found");
        StringBuilder description = new StringBuilder();
        embedBuilder.setColor(Goldilocks.BLUE);
        potentialMemberNameHashMap.forEach((member, s) -> description.append("**`" + String.format("%-" +
                potentialMemberNameHashMap.keySet().stream().map(k -> k.getEffectiveName().length()).max(Integer::compareTo).get() + "s",
                member.getEffectiveName().replaceAll("[^A-Za-z\\|. ]", "")) + ":`**" + member.getAsMention().replace("!", "") + "\n"));
        embedBuilder.setDescription(description.toString());
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
