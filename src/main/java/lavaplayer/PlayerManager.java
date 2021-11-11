package lavaplayer;

//public class PlayerManager {
//    private static PlayerManager INSTANCE;
//    private final AudioPlayerManager playerManager;
//    private final Map<Long, GuildMusicManager> musicManagers;
//
//    private PlayerManager() {
//        this.musicManagers = new HashMap<>();
//
//        this.playerManager = new DefaultAudioPlayerManager();
//        AudioSourceManagers.registerRemoteSources(playerManager);
//        AudioSourceManagers.registerLocalSource(playerManager);
//    }
//
//    public synchronized GuildMusicManager getGuildMusicManager(Guild guild) {
//        long guildId = guild.getIdLong();
//        GuildMusicManager musicManager = musicManagers.get(guildId);
//
//        if (musicManager == null) {
//            musicManager = new GuildMusicManager(playerManager);
//            musicManagers.put(guildId, musicManager);
//        }
//
//        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
//
//        return musicManager;
//    }
//
//    public void loadAndPlay(TextChannel textChannel, String trackUrl) {
//        GuildMusicManager guildMusicManager = getGuildMusicManager(textChannel.getGuild());
//
//        playerManager.loadItemOrdered(guildMusicManager, trackUrl, new AudioLoadResultHandler() {
//            @Override
//            public void trackLoaded(AudioTrack audioTrack) {
//                play(guildMusicManager, audioTrack);
//            }
//
//            @Override
//            public void playlistLoaded(AudioPlaylist audioPlaylist) {
//                AudioTrack firstTrack = audioPlaylist.getSelectedTrack();
//
//                if (firstTrack == null) {
//                    firstTrack = audioPlaylist.getTracks().get(0);
//                }
//
//                play(guildMusicManager, firstTrack);
//
//            }
//
//            @Override
//            public void noMatches() {
//                //Do nothing
//            }
//
//            @Override
//            public void loadFailed(FriendlyException e) {
//                e.printStackTrace();
//                System.out.println("Could not load track");
//            }
//        });
//    }
//
//    private void play(GuildMusicManager guildMusicManager, AudioTrack track) {
//        guildMusicManager.scheduler.queue(track);
//    }
//
//    public static synchronized PlayerManager getInstance() {
//        if (INSTANCE == null) {
//            INSTANCE = new PlayerManager();
//        }
//
//        return INSTANCE;
//    }
//}
