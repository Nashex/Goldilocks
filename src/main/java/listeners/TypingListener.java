package listeners;

import main.Database;
import modmail.ModmailHub;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public class TypingListener extends ListenerAdapter {
    @Override
    public void onUserTyping(@Nonnull UserTypingEvent event) {
        if (event.isFromType(ChannelType.TEXT) && !event.getChannel().getName().contains("👀‍")
                && Database.getModmailCategory(event.getGuild()) != null && !event.getUser().isBot()
                && Database.getModmailCategory(event.getGuild()).getTextChannels().contains(event.getTextChannel())) {
            if (ModmailHub.isModmailChannel(event.getTextChannel())) {
                String channelName = event.getTextChannel().getName();
                event.getTextChannel().getManager().setName(channelName.replace("📨", "‍👀")).queue();
            }
        }
    }
}
