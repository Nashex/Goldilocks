package slashCommands.slashcommands;

import commands.CommandHub;
import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.StringUtils;
import shatters.SqlConnector;
import slashCommands.SlashCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class SlashCommandChangeLog extends SlashCommand {
    public SlashCommandChangeLog() {
        setName("changelog");
        setEligibleRoles(new String[] {"officer", "hrl"});
        setGuildRank(3);
        setNameSpace(CommandHub.CommandNameSpace.MOD);

        this.commandData = new CommandData("changelog", "Modifies the values in the database for a corresponding column and user.")
                .addOptions(
                        new OptionData(OptionType.USER, "user", "The user you are modifying the data for.").setRequired(true),
                        new OptionData(OptionType.STRING, "field", "The field you would like to modify.")
                                .addChoices(
                                        new Command.Choice("Runs", "runs"),
                                        new Command.Choice("Assists", "assists"),
                                        new Command.Choice("Parses", "parses"),
                                        new Command.Choice("Key Pops", "keys")
                                ).setRequired(true),
                        new OptionData(OptionType.INTEGER, "amount", "The amount you would like to add. Make this negative to remove.").setRequired(true)
                );
    }

    @Override
    public SlashCommand enable(Guild guild) {
        if (Database.isPub(guild)) {
            guild.upsertCommand(new CommandData("changelog", "Modifies the values in the database for a corresponding column and user.")
                    .addOptions(
                            new OptionData(OptionType.USER, "user", "The user you are modifying the data for.").setRequired(true),
                            new OptionData(OptionType.STRING, "field", "The field you would like to modify.")
                                    .addChoices(
                                            new Command.Choice("Voids", "void"),
                                            new Command.Choice("Cults", "cult"),
                                            new Command.Choice("Events", "event"),
                                            new Command.Choice("Assists", "assists"),
                                            new Command.Choice("Parses", "parses"),
                                            new Command.Choice("Keys", "keypops"),
                                            new Command.Choice("Event Keys", "eventpops")
                                    ).setRequired(true),
                            new OptionData(OptionType.INTEGER, "amount", "The amount you would like to add. Make this negative to remove.").setRequired(true)
                    )).queue();
            return this;
        } else if (Database.isShatters(guild)) {
            guild.upsertCommand(new CommandData("changelog", "Modifies the values in the database for a corresponding column and user.")
                    .addOptions(
                            new OptionData(OptionType.USER, "user", "The user you are modifying the data for.").setRequired(true),
                            new OptionData(OptionType.STRING, "field", "The field you would like to modify.")
                                    .addChoices(
                                            new Command.Choice("Success Runs", "currentweek:successruns"),
                                            new Command.Choice("Failed Runs", "currentweekfailed:failruns"),
                                            new Command.Choice("Event Runs", "currentweekEvents:eventsLead"),
                                            new Command.Choice("Assists", "currentweekAssists:assists"),
                                            new Command.Choice("Shatters Pops", "shatterspops"),
                                            new Command.Choice("Event Pops", "eventpops"),
                                            new Command.Choice("Points", "points")
                                    ).setRequired(true),
                            new OptionData(OptionType.INTEGER, "amount", "The amount you would like to add. Make this negative to remove.").setRequired(true)
                    )).queue();
            return this;
        } else if (guild.getId().equals("708026927721480254")) { // OSanc
            guild.upsertCommand(new CommandData("changelog", "Modifies the values in the database for a corresponding column and user.")
                    .addOptions(
                            new OptionData(OptionType.USER, "user", "The user you are modifying the data for.").setRequired(true),
                            new OptionData(OptionType.STRING, "field", "The field you would like to modify.")
                                    .addChoices(
                                            new Command.Choice("Success Runs", "currentweek:successruns"),
                                            new Command.Choice("Failed Runs", "currentweekfailed:failruns"),
                                            new Command.Choice("Event Runs", "currentweekEvents:eventsLead"),
                                            new Command.Choice("Assists", "currentweekAssists:assists"),
                                            new Command.Choice("Shatters Pops", "shatterspops"),
                                            new Command.Choice("Event Pops", "eventpops"),
                                            new Command.Choice("Points", "points")
                                    ).setRequired(true),
                            new OptionData(OptionType.INTEGER, "amount", "The amount you would like to add. Make this negative to remove.").setRequired(true)
                    )).queue();
            return this;
        }
        else return super.enable(guild);
    }

    @Override
    public void execute(SlashCommandEvent event) {
        Member member = event.getOptionsByName("user").get(0).getAsMember();
        String field = event.getOptionsByName("field").get(0).getAsString();
        long value = event.getOptionsByName("amount").get(0).getAsLong();

        Guild guild = event.getGuild();

        if (guild == null || member == null) return;

        if (!Database.isPub(guild)) {

            if (Database.isShatters(guild)) {
                if (field.contains(":")) {
                    SqlConnector.logFieldForMember(member, Arrays.asList(field.split(":")[0],
                            field.split(":")[1]), (int) value, "shatters");
                    event.reply("Successfully " + (value < 0 ? "removed" : "added") +" " + (value < 0 ? value * -1 : value) + " " +
                            StringUtils.capitalize(field.split(":")[1]) + (value < 0 ? " from " : " to ") + member.getEffectiveName()).queue();
                    return;
                } else {
                    SqlConnector.logFieldForMember(member, Collections.singletonList(field), (int) value, "shatters");
                    event.reply("Successfully " + (value < 0 ? "removed" : "added") +" " + (value < 0 ? value * -1 : value) + " " +
                            StringUtils.capitalize(field) + (value < 0 ? " from " : " to ") + member.getEffectiveName()).queue();
                    return;
                }
            } else {
                Database.executeUpdate("UPDATE rlStats SET quota" + field + " = quota" + field + " + " + value + ", total" + (field.equals("runs") ? "runsLed" : field)
                        + " = total" + (field.equals("runs") ? "runsLed" : field) + " + " + value + " WHERE userId = " + member.getId() + " AND guildId = " + member.getGuild().getId());
            }

        } else {
            if (!field.contains("pops")) {
                SqlConnector.logFieldForMember(member, Arrays.asList((Arrays.asList("cult", "void", "event").contains(field) ? field + "sLead" : field),
                        "currentweek" + field + (field.equals("event") ? "s" : "")), (int) value, "halls");
            } else {
                SqlConnector.logFieldForMember(member, Collections.singletonList(field), (int) value, "halls");
            }

        }

        event.reply("Successfully " + (value < 0 ? "removed" : "added") +" " + (value < 0 ? value * -1 : value) + " " + (Arrays.asList("cult", "void", "event").contains(field) ? StringUtils.capitalize(field) + "s" :
                StringUtils.capitalize(field).replace("pops", " Pops")) + (value < 0 ? " from " : " to ") + member.getEffectiveName()).queue();

    }

    @Override
    public EmbedBuilder getInfo() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Command: Change Log");
        embedBuilder.setColor(Goldilocks.BLUE);

        String commandDescription = "```\n";
        commandDescription += "Required rank: Head Raid Leader or Mod\n";
        commandDescription += "\nModifies the database for a given field." + "\n```";
        embedBuilder.setDescription(commandDescription);
        embedBuilder.setTimestamp(new Date().toInstant());
        return embedBuilder;
    }

}
