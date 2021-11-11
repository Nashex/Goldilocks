package quota;

import net.dv8tion.jda.api.entities.Role;

import javax.annotation.Nonnull;

public class QuotaRole {
    public Role role;
    public int runs;
    public int minRunsForAssists;
    public int assists;
    public int parses;

    public QuotaRole(@Nonnull Role role) {
        this.role = role;
        runs = 0;
        minRunsForAssists = 0;
        assists = 0;
        parses = 0;
    }

    public QuotaRole(Role role, int runs, int minRunsForAssists, int assists, int parses) {
        this.role = role;
        this.runs = runs;
        this.minRunsForAssists = minRunsForAssists;
        this.assists = assists;
        this.parses = parses;
    }

}
