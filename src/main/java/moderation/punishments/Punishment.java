package moderation.punishments;

import net.dv8tion.jda.api.entities.Member;

public class Punishment {

    public Member recipient = null, mod;
    public String reason;
    protected long timeIssued;

    public Punishment() { }

}
