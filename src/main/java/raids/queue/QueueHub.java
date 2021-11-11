package raids.queue;

import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class QueueHub {

    public ScheduledExecutorService TIMER = new ScheduledThreadPoolExecutor(2);
    public static List<Queue> activeQueues = new ArrayList<>();

    public QueueHub() {
        //Do something
    }

    public static Queue getQueue(TextChannel textChannel) {
        for (Queue q : activeQueues) if (q.rsChannel.equals(textChannel)) return q;
        return null;
    }

}
