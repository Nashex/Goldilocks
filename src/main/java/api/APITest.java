package api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import sheets.GoogleSheets;

@SpringBootApplication
@RestController
public class APITest {

    private RaidRepository raidRepository;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(APITest.class);
        app.run(args);
    }

    @GetMapping("/")
    public @ResponseBody
    String logRaid () {
        // @ResponseBody means the returned String is the response, not a view name
        // @RequestParam means it is a parameter from the GET or POST request
        return "Pong";
    }

    @PostMapping("/lograid")
    public @ResponseBody
    String logRaid (@RequestBody LogRaid logRaid) {
        // @ResponseBody means the returned String is the response, not a view name
        // @RequestParam means it is a parameter from the GET or POST request

        //GoogleSheets.logEvent(raidGuild, GoogleSheets.SheetsLogType.RAIDS, raidLeader.getEffectiveName(), raidLeader.getId(), dungeonName, raidType + "");
        //LogRaid logRaid = new LogRaid(guildId, raidLeaderName, raidLeaderId, dungeonName);
        return GoogleSheets.logEvent(logRaid.getGuildId(), GoogleSheets.SheetsLogType.RAIDS, logRaid.getRaidLeaderName(), logRaid.getRaidLeaderId(), logRaid.getDungeonName(), "OTHER BOT");
    }


}
