package main;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import commands.CommandHub;
import commands.adminCommands.*;
import commands.debugCommands.CommandHelp;
import commands.lobbyCommands.CommandCreateLobbyManager;
import commands.lobbyCommands.CommandDeleteLobbyManager;
import commands.lobbyCommands.CommandLobbyManagerCP;
import commands.miscCommands.*;
import commands.moderationCommands.*;
import commands.parseCommands.CommandOcrTest;
import commands.parseCommands.CommandParse;
import commands.parseCommands.CommandParseVc;
import commands.raidCommands.*;
import commands.setupCommands.*;
import listeners.*;
import lobbies.LobbyManagerHub;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.LoggerFactory;
import raids.RaidHub;
import slashCommands.SlashCommandHub;
import slashCommands.SlashCommandListener;
import slashCommands.slashcommands.SlashCommandChangeLog;
import utils.EmoteCache;
import utils.ProxyHelper;
import verification.VerificationHub;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Getter
public class Goldilocks {

    public static Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "nashex",
            "api_key", "334294162247817",
            "api_secret", Config.get("API_SECRET")));
    public static ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(8);

    public static final Color BLUE = new Color(14, 144, 255);
    public static final Color GOLD = new Color(158, 119, 0);
    public static final Color WHITE = new Color(212, 212, 212);
    public static final Color LIGHTBLUE = new Color(99, 184, 255);
    public static final Color GREEN = new Color(140, 255, 112);
    public static final Color DARKGREEN = new Color(40, 137, 0);
    public static final Color RED = new Color(255, 112, 140);
    public static final Color YELLOW = new Color(255, 219, 112);

    public static String[] numEmotes = {"0️⃣","1️⃣","2️⃣","3️⃣","4️⃣","5️⃣","6️⃣","7️⃣","8️⃣","9️⃣","🔟"};

    public static CommandHub commands = new CommandHub();
    public static SlashCommandHub slashCommands = new SlashCommandHub();
    //public static LobbyHub lobbys = new LobbyHub();
    public static EventWaiter eventWaiter;
    public static ProxyHelper proxyHelper;
    public static EmoteCache emoteCache;
    public static JDA jda;
    public static long timeStarted;

    public static HashMap<Guild, Message> activeGames = new HashMap<>();
    public static void main(String[] args) {

        final org.slf4j.Logger log = LoggerFactory.getLogger(Goldilocks.class);

        timeStarted = System.currentTimeMillis();
        try {
            new Config();
            //NzYyODk0NzU0NjM5MzgwNTUw.X3vzGw.diDW7BWfK_ErgY1SccdHpGnQWLs

            JDABuilder builder = JDABuilder.create((String) Config.get("TOKEN"),
                    GatewayIntent.GUILD_BANS,
                    GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                    GatewayIntent.GUILD_EMOJIS,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.DIRECT_MESSAGE_TYPING,
                    GatewayIntent.GUILD_VOICE_STATES,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_VOICE_STATES,
                    GatewayIntent.GUILD_MESSAGE_TYPING,
                    GatewayIntent.GUILD_PRESENCES);

            builder.setAutoReconnect(true);

            builder.setStatus(OnlineStatus.ONLINE);



            eventWaiter = new EventWaiter();
            TIMER.execute(() -> proxyHelper = new ProxyHelper());

            registerCommands();
            registerListener(builder, eventWaiter);

            try {
                jda = builder.build();
            } catch ( LoginException e) {
                e.printStackTrace();
            }
            emoteCache = new EmoteCache();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void registerListener(JDABuilder builder, EventWaiter eventWaiter){
        builder.addEventListeners(new CommandListener());
        builder.addEventListeners(new PrivateCommandListener());
        builder.addEventListeners(new GeneralListener());
        builder.addEventListeners(new ReadyListener());
        builder.addEventListeners(new LobbyManagerHub());
        builder.addEventListeners(new RaidHub());
        builder.addEventListeners(new VerificationHub());
        builder.addEventListeners(new ReactionListener(eventWaiter));
        builder.addEventListeners(new DisconnectListener());
        builder.addEventListeners(new TypingListener());
        builder.addEventListeners(new SlashCommandListener());
        builder.addEventListeners(eventWaiter);

    }

    private static void registerCommands() {
        commands.add(new CommandHelp());
        //commands.add(new CommandReboot());
        //commands.add(new CommandTerminate());
        commands.add(new CommandTest());
        commands.add(new CommandPing());
        commands.add(new CommandSetup());
        commands.add(new CommandRaidSetup());
        commands.add(new CommandVerificationSetup());
        commands.add(new CommandVerifyEmbed());
        commands.add(new CommandDungeonVerifyEmbed());
        commands.add(new CommandLobbyManagerCP());
        commands.add(new CommandCreateLobbyManager());
        commands.add(new CommandDeleteLobbyManager());
        commands.add(new CommandStats());
        commands.add(new CommandCreateRaid());
        commands.add(new CommandStartRaid());
        commands.add(new CommandStartHeadCount());
        commands.add(new CommandOcrTest());
        commands.add(new CommandParseVc());
        commands.add(new CommandSuspend());
        commands.add(new CommandParse());
        commands.add(new CommandQuotaEmbed());
        commands.add(new CommandRaidLeaderPrefs());
        commands.add(new CommandRuns());
        commands.add(new CommandTopRuns());
        commands.add(new CommandUserInfo());
        commands.add(new CommandAddKey());
        commands.add(new CommandRunsVerificationEmbed());
        commands.add(new CommandCleanup());
        commands.add(new CommandUnsuspend());
        commands.add(new CommandEndRaid());
        commands.add(new CommandNoNick());
        commands.add(new CommandWarn());
        commands.add(new CommandCase());
        commands.add(new CommandCreateCase());
        commands.add(new CommandCreateEventRaid());
        commands.add(new CommandKeySetup());
        commands.add(new CommandCreateStickyRole());
        commands.add(new CommandPoints());
        commands.add(new CommandCreateEventHeadCount());
        commands.add(new CommandManualVerify());
        //commands.add(new CommandTransferRaid());
        commands.add(new CommandDrag());
        commands.add(new CommandChangeRaidType());
        commands.add(new CommandRemovePunishment());
        commands.add(new CommandAddRole());
        commands.add(new CommandPlayerHistory());
        commands.add(new CommandMusic());
        commands.add(new CommandAddUserData());
        commands.add(new CommandRoleInfo());
        commands.add(new Command2048());
        commands.add(new CommandTestVault());
        commands.add(new CommandAddAlt());
        commands.add(new CommandTestExalts());
        commands.add(new CommandGiveaway());
        commands.add(new CommandPoll());
        commands.add(new CommandRate());
        commands.add(new CommandClearVc());
        //commands.add(new CommandTrlArlVote());
        commands.add(new CommandLog());
        commands.add(new CommandTutorial());
        commands.add(new CommandChangeName());
        commands.add(new CommandFind());
        commands.add(new CommandChart());
        commands.add(new CommandCSV());
//        commands.add(new CommandExpelAdd());
        commands.add(new CommandExpelRemove());
        commands.add(new CommandCheck());
        commands.add(new CommandKick());
        commands.add(new CommandStatus());
        commands.add(new CommandGhostPing());
        commands.add(new CommandFeedback());
        commands.add(new CommandWord());
        commands.add(new CommandLockdown());
        commands.add(new CommandGraveyard());
        commands.add(new CommandMute());
        commands.add(new CommandUnmute());
        commands.add(new CommandNote());
        commands.add(new CommandMock());
        commands.add(new CommandBackgroundCheck());
        commands.add(new CommandGoogleSheets());
        commands.add(new CommandResetQuota());
        commands.add(new CommandAssist());
        commands.add(new CommandUnlock());
        commands.add(new CommandDuplicates());
        commands.add(new CommandCrashes());
        commands.add(new CommandScramble());
        commands.add(new CommandExpel());
    }

    public static void registerSlashCommands() {
        Guild guild = Goldilocks.jda.getGuildById("733482900841824318");
        slashCommands.add(new SlashCommandChangeLog().enable(guild));
    }

    public static String getUptime(long timeStarted) {
        return new PrettyTime().format(new Date(timeStarted));
    }
}
