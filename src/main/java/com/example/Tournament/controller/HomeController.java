package com.example.Tournament.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.Tournament.entity.Ball;
import com.example.Tournament.entity.Innings;
import com.example.Tournament.entity.Match;
import com.example.Tournament.entity.Player;
import com.example.Tournament.entity.Scorecard;
import com.example.Tournament.entity.Team;
import com.example.Tournament.entity.Tournament;
import com.example.Tournament.repository.InningsRepository;
import com.example.Tournament.repository.MatchRepository;
import com.example.Tournament.repository.ScorecardRepository;
import com.example.Tournament.repository.TeamRepository;
import com.example.Tournament.repository.PlayerRepository;
import com.example.Tournament.repository.BallRepository;
import com.example.Tournament.repository.TournamentRepository;
import com.example.Tournament.service.LiveScoreBroadcaster;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class HomeController {

    @Autowired private MatchRepository matchRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private InningsRepository inningsRepository;
    @Autowired private ScorecardRepository scorecardRepository;
    @Autowired private BallRepository ballRepository;
    @Autowired private TournamentRepository tournamentRepository;
    @Autowired private LiveScoreBroadcaster liveScoreBroadcaster;

    private static class PlayerAggregate {
        private final Player player;
        private int totalRuns;
        private int totalWickets;
        private int highestScore;
        private int totalOuts;
        private int fifties;
        private int hundreds;
        private int totalFours;
        private int totalSixes;
        private int totalRunsGiven;
        private float totalOversBowled;
        private int bestWickets;
        private int bestRunsGiven = Integer.MAX_VALUE;

        public PlayerAggregate(Player player) {
            this.player = player;
        }

        public void update(Scorecard scorecard) {
            totalRuns += scorecard.getRuns();
            totalFours += scorecard.getFours();
            totalSixes += scorecard.getSixes();
            highestScore = Math.max(highestScore, scorecard.getRuns());

            if (scorecard.isOut()) {
                totalOuts++;
            }
            if (scorecard.getRuns() >= 50 && scorecard.getRuns() < 100) {
                fifties++;
            }
            if (scorecard.getRuns() >= 100) {
                hundreds++;
            }

            totalWickets += scorecard.getWickets();
            totalRunsGiven += scorecard.getRunsGiven();
            totalOversBowled += scorecard.getOversBowled();

            if (scorecard.getWickets() > bestWickets
                    || (scorecard.getWickets() == bestWickets && scorecard.getRunsGiven() < bestRunsGiven)) {
                bestWickets = scorecard.getWickets();
                bestRunsGiven = scorecard.getRunsGiven();
            }
        }

        public Player getPlayer() { return player; }
        public int getTotalRuns() { return totalRuns; }
        public int getTotalWickets() { return totalWickets; }
        public int getHighestScore() { return highestScore; }
        public int getTotalOuts() { return totalOuts; }
        public int getFifties() { return fifties; }
        public int getHundreds() { return hundreds; }
        public int getTotalFours() { return totalFours; }
        public int getTotalSixes() { return totalSixes; }
        public int getTotalRunsGiven() { return totalRunsGiven; }
        public float getTotalOversBowled() { return totalOversBowled; }
        public int getBestWickets() { return bestWickets; }
        public int getBestRunsGiven() { return bestRunsGiven == Integer.MAX_VALUE ? 0 : bestRunsGiven; }

        public String getTeamName() {
            return player.getTeam() != null ? player.getTeam().getTeamName() : "Unknown";
        }

        public double getBattingAverage() {
            return totalOuts > 0 ? Math.round((totalRuns / (double) totalOuts) * 100.0) / 100.0 : totalRuns;
        }

        public double getBowlingAverage() {
            return totalWickets > 0 ? Math.round((totalRunsGiven / (double) totalWickets) * 100.0) / 100.0 : Double.MAX_VALUE;
        }

        public double getEconomyRate() {
            return totalOversBowled > 0
                    ? Math.round((totalRunsGiven / totalOversBowled) * 100.0) / 100.0
                    : Double.MAX_VALUE;
        }

        public int getBoundaryCount() {
            return totalFours + totalSixes;
        }

        public String getBestBowlingFigure() {
            return bestWickets > 0 ? bestWickets + "/" + getBestRunsGiven() : "0/0";
        }
    }

    private static class StatSpot {
        private final String playerName;
        private final String teamName;
        private final String value;

        public StatSpot(String playerName, String teamName, String value) {
            this.playerName = playerName;
            this.teamName = teamName;
            this.value = value;
        }

        public String getPlayerName() { return playerName; }
        public String getTeamName() { return teamName; }
        public String getValue() { return value; }
    }

    private List<PlayerAggregate> buildPlayerAggregates(List<Scorecard> scorecards) {
        Map<Integer, PlayerAggregate> aggMap = new LinkedHashMap<>();
        for (Scorecard scorecard : scorecards) {
            Player player = scorecard.getPlayer();
            if (player == null) continue;
            aggMap.computeIfAbsent(player.getPlayerId(), id -> new PlayerAggregate(player)).update(scorecard);
        }
        return new ArrayList<>(aggMap.values());
    }

    private List<StatSpot> buildTopStats(List<PlayerAggregate> aggregates,
                                         Comparator<PlayerAggregate> comparator,
                                         Function<PlayerAggregate, String> extractor) {
        return aggregates.stream()
                .sorted(comparator)
                .limit(5)
                .map(a -> new StatSpot(a.getPlayer().getPlayerName(), a.getTeamName(), extractor.apply(a)))
                .collect(Collectors.toList());
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("matches", matchRepository.findAll());
        model.addAttribute("teamCount", teamRepository.count());
        model.addAttribute("playerCount", playerRepository.count());
        model.addAttribute("liveCount", matchRepository.findByStatus("Live").size());
        return "home";
    }

    @GetMapping("/teams")
    public String teamsPublic(Model model) {
        var teams = teamRepository.findAll();
        Map<Integer, List<Player>> teamPlayersMap = new HashMap<>();
        for (var team : teams) {
            teamPlayersMap.put(team.getTeamId(), playerRepository.findByTeam_TeamId(team.getTeamId()));
        }
        model.addAttribute("teams", teams);
        model.addAttribute("teamPlayersMap", teamPlayersMap);
        return "public/teams";
    }

    @GetMapping("/players")
    public String playersPublic(Model model) {
        model.addAttribute("players", playerRepository.findAll());
        return "public/players";
    }

    @GetMapping("/matches")
    public String matchesPublic(Model model) {
        model.addAttribute("matches", matchRepository.findAll());
        return "public/matches";
    }

    // Public Tournaments List
    @GetMapping("/tournaments")
    public String tournamentsPublic(Model model) {
        model.addAttribute("tournaments", tournamentRepository.findAll());
        return "public/tournaments";
    }

    // Public Tournament Detail Page
    @GetMapping("/tournament/{id}")
    public String tournamentDetail(@PathVariable int id, Model model) {
        Tournament tournament = tournamentRepository.findById(id).orElse(null);
        if (tournament == null) return "redirect:/tournaments";

        model.addAttribute("tournament", tournament);

        List<Match> tournamentMatches = matchRepository.findByTournament_TournamentId(id);
        model.addAttribute("matches", tournamentMatches);

        Map<Integer, Team> tournamentTeamMap = new LinkedHashMap<>();
        for (Match m : tournamentMatches) {
            if (m.getTeam1() != null) tournamentTeamMap.put(m.getTeam1().getTeamId(), m.getTeam1());
            if (m.getTeam2() != null) tournamentTeamMap.put(m.getTeam2().getTeamId(), m.getTeam2());
        }
        for (Team team : teamRepository.findAll()) {
            if (team.getTournament() != null && team.getTournament().getTournamentId() == id) {
                tournamentTeamMap.put(team.getTeamId(), team);
            }
        }
        model.addAttribute("teams", new ArrayList<>(tournamentTeamMap.values()));

        List<Integer> matchIds = tournamentMatches.stream()
                .map(Match::getMatchId)
                .collect(Collectors.toList());
        List<Scorecard> tournamentScorecards = matchIds.isEmpty()
                ? List.of()
                : scorecardRepository.findByMatch_MatchIdIn(matchIds);

        List<PlayerAggregate> aggregates = buildPlayerAggregates(tournamentScorecards);

        model.addAttribute("statsMostRuns", buildTopStats(
                aggregates,
                Comparator.comparingInt(PlayerAggregate::getTotalRuns).reversed(),
                agg -> String.valueOf(agg.getTotalRuns())
        ));

        model.addAttribute("statsMostWickets", buildTopStats(
                aggregates.stream().filter(a -> a.getTotalWickets() > 0).collect(Collectors.toList()),
                Comparator.comparingInt(PlayerAggregate::getTotalWickets).reversed(),
                agg -> String.valueOf(agg.getTotalWickets())
        ));

        model.addAttribute("statsHighestScore", buildTopStats(
                aggregates,
                Comparator.comparingInt(PlayerAggregate::getHighestScore).reversed(),
                agg -> String.valueOf(agg.getHighestScore())
        ));

        model.addAttribute("statsBestBowling", buildTopStats(
                aggregates.stream().filter(a -> a.getBestWickets() > 0).collect(Collectors.toList()),
                Comparator.comparingInt(PlayerAggregate::getBestWickets).reversed()
                        .thenComparingInt(PlayerAggregate::getBestRunsGiven),
                PlayerAggregate::getBestBowlingFigure
        ));

        model.addAttribute("statsBattingAverage", buildTopStats(
                aggregates.stream().filter(a -> a.getTotalRuns() > 0).collect(Collectors.toList()),
                Comparator.comparingDouble(PlayerAggregate::getBattingAverage).reversed(),
                agg -> String.format(Locale.US, "%.2f", agg.getBattingAverage())
        ));

        model.addAttribute("statsBowlingAverage", buildTopStats(
                aggregates.stream().filter(a -> a.getTotalWickets() > 0).collect(Collectors.toList()),
                Comparator.comparingDouble(PlayerAggregate::getBowlingAverage),
                agg -> String.format(Locale.US, "%.2f", agg.getBowlingAverage())
        ));

        model.addAttribute("statsMostFifties", buildTopStats(
                aggregates.stream().filter(a -> a.getFifties() > 0).collect(Collectors.toList()),
                Comparator.comparingInt(PlayerAggregate::getFifties).reversed(),
                agg -> String.valueOf(agg.getFifties())
        ));

        List<StatSpot> mostHundreds = buildTopStats(
                aggregates.stream().filter(a -> a.getHundreds() > 0).collect(Collectors.toList()),
                Comparator.comparingInt(PlayerAggregate::getHundreds).reversed(),
                agg -> String.valueOf(agg.getHundreds())
        );
        model.addAttribute("statsMostHundreds", mostHundreds);
        model.addAttribute("showHundredsSection", !mostHundreds.isEmpty());

        model.addAttribute("statsEconomy", buildTopStats(
                aggregates.stream().filter(a -> a.getTotalOversBowled() > 0).collect(Collectors.toList()),
                Comparator.comparingDouble(PlayerAggregate::getEconomyRate),
                agg -> String.format(Locale.US, "%.2f", agg.getEconomyRate())
        ));

        model.addAttribute("statsMostSixes", buildTopStats(
                aggregates.stream().filter(a -> a.getTotalSixes() > 0).collect(Collectors.toList()),
                Comparator.comparingInt(PlayerAggregate::getTotalSixes).reversed(),
                agg -> String.valueOf(agg.getTotalSixes())
        ));

        model.addAttribute("statsMostFours", buildTopStats(
                aggregates.stream().filter(a -> a.getTotalFours() > 0).collect(Collectors.toList()),
                Comparator.comparingInt(PlayerAggregate::getTotalFours).reversed(),
                agg -> String.valueOf(agg.getTotalFours())
        ));

        model.addAttribute("statsMostBoundaries", buildTopStats(
                aggregates.stream().filter(a -> a.getBoundaryCount() > 0).collect(Collectors.toList()),
                Comparator.comparingInt(PlayerAggregate::getBoundaryCount).reversed(),
                agg -> String.valueOf(agg.getBoundaryCount())
        ));

        model.addAttribute("showHundredsSection", !((List<?>) model.getAttribute("statsMostHundreds")).isEmpty());
        return "public/tournament-detail";
    }

    @GetMapping("/match/{id}")
    public String matchDetail(@PathVariable int id, Model model) {
        Match match = matchRepository.findById(id).orElse(null);
        if (match == null) return "redirect:/matches";
        model.addAttribute("team1SquadPlayers", resolveSquadPlayers(match.getTeam1Squad()));
        model.addAttribute("team2SquadPlayers", resolveSquadPlayers(match.getTeam2Squad()));
        model.addAttribute("match", match);
        model.addAttribute("innings", inningsRepository.findByMatchMatchId(id));
        model.addAttribute("scorecards", scorecardRepository.findByMatch_MatchId(id));
        return "public/match-detail";
    }

    @GetMapping("/commentry/{id}")
    public String commentry(@PathVariable int id, Model model) {
        Match match = matchRepository.findById(id).orElse(null);
        if (match == null) return "redirect:/matches";
        model.addAttribute("match", match);
        model.addAttribute("innings", inningsRepository.findByMatchMatchId(id));
        model.addAttribute("scorecards", scorecardRepository.findByMatch_MatchId(id));
        return "public/commentry";
    }

    private List<Player> resolveSquadPlayers(String squadCsv) {
        if (squadCsv == null || squadCsv.isBlank()) return List.of();
        List<Integer> ids = new ArrayList<>();
        for (String part : squadCsv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.matches("\\d+")) ids.add(Integer.parseInt(trimmed));
        }
        if (ids.isEmpty()) return List.of();
        return playerRepository.findAllById(ids);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC LIVE SCORE API
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/api/public/live/{matchId}")
    @ResponseBody
    public Map<String, Object> publicLiveScore(@PathVariable int matchId) {
        Map<String, Object> res = new HashMap<>();

        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null) { res.put("found", false); return res; }

        res.put("found",        true);
        res.put("status",       match.getStatus());
        res.put("team1Name",    match.getTeam1() != null ? match.getTeam1().getTeamName() : "");
        res.put("team2Name",    match.getTeam2() != null ? match.getTeam2().getTeamName() : "");
        res.put("team1Id",      match.getTeam1() != null ? match.getTeam1().getTeamId()   : 0);
        res.put("team2Id",      match.getTeam2() != null ? match.getTeam2().getTeamId()   : 0);
        res.put("overs",        match.getOvers());
        res.put("venue",        match.getVenue()        != null ? match.getVenue()        : "");
        res.put("matchDate",    match.getMatchDate()    != null ? match.getMatchDate().toString() : "");
        res.put("tossWinner",   match.getTossWinner()   != null ? match.getTossWinner().getTeamName()  : "");
        res.put("tossDecision", match.getTossDecision() != null ? match.getTossDecision() : "");
        res.put("matchWinner",  match.getMatchWinner()  != null ? match.getMatchWinner().getTeamName() : "");

        // ── All balls sorted by ID (oldest first = lowest ID first) ─────
        // IMPORTANT: Sort by Ball ID only — overNumber resets to 0 in 2nd innings
        // so sorting by overNumber would mix both innings balls incorrectly.
        List<Ball> allBalls = ballRepository.findByMatchMatchId(matchId);
        allBalls.sort(Comparator.comparingInt(Ball::getId));   // oldest → newest

        res.put("totalBalls", allBalls.size());

        // ── Completed innings from DB (sorted by inningsId) ─────────────
        List<Innings> dbInnings = inningsRepository.findByMatchMatchId(matchId);
        dbInnings.sort(Comparator.comparingInt(Innings::getInningsId));

        // ── Build innings list ───────────────────────────────────────────
        List<Map<String, Object>> inningsList = new ArrayList<>();

        for (Innings inn : dbInnings) {
            Map<String, Object> i = new HashMap<>();
            i.put("battingTeam",  inn.getBattingTeam() != null ? inn.getBattingTeam().getTeamName() : "");
            i.put("bowlingTeam",  inn.getBowlingTeam() != null ? inn.getBowlingTeam().getTeamName() : "");
            i.put("totalRuns",    inn.getTotalRuns());
            i.put("totalWickets", inn.getTotalWickets());
            i.put("oversPlayed",  inn.getOversPlayed());
            i.put("extras",       inn.getExtras());
            i.put("wides",        inn.getWides());
            i.put("noBalls",      inn.getNoBalls());
            i.put("byes",         inn.getByes());
            i.put("live",         false);
            inningsList.add(i);
        }

        // ── Compute LIVE ongoing innings from balls ───────────────────────
        if ("Live".equals(match.getStatus()) && !allBalls.isEmpty()) {

            int currentInningNum = dbInnings.size() + 1;

            int team1Id = match.getTeam1() != null ? match.getTeam1().getTeamId() : 0;
            int team2Id = match.getTeam2() != null ? match.getTeam2().getTeamId() : 0;

            // Determine WHO is batting live
            // If Inn1 is already saved in DB → live team = team that did NOT bat in Inn1
            // If nothing saved yet → use toss decision
            String liveBattingTeam = "";
            String liveBowlingTeam = "";

            if (!dbInnings.isEmpty()) {
                Team inn1BatTeam = dbInnings.get(0).getBattingTeam();
                int inn1BatId = inn1BatTeam != null ? inn1BatTeam.getTeamId() : 0;
                int liveBatId = (inn1BatId == team1Id) ? team2Id : team1Id;
                liveBattingTeam = (liveBatId == team1Id)
                        ? (match.getTeam1() != null ? match.getTeam1().getTeamName() : "")
                        : (match.getTeam2() != null ? match.getTeam2().getTeamName() : "");
                liveBowlingTeam = (liveBatId == team1Id)
                        ? (match.getTeam2() != null ? match.getTeam2().getTeamName() : "")
                        : (match.getTeam1() != null ? match.getTeam1().getTeamName() : "");
            } else if (match.getTossWinner() != null && match.getTossDecision() != null) {
                int tossId = match.getTossWinner().getTeamId();
                int firstBatId = "Bat".equalsIgnoreCase(match.getTossDecision())
                        ? tossId : (tossId == team1Id ? team2Id : team1Id);
                liveBattingTeam = (firstBatId == team1Id)
                        ? (match.getTeam1() != null ? match.getTeam1().getTeamName() : "")
                        : (match.getTeam2() != null ? match.getTeam2().getTeamName() : "");
                liveBowlingTeam = (firstBatId == team1Id)
                        ? (match.getTeam2() != null ? match.getTeam2().getTeamName() : "")
                        : (match.getTeam1() != null ? match.getTeam1().getTeamName() : "");
            }

            // ── How many balls belong to completed Inn1? ─────────────────
            // Walk allBalls (sorted oldest→newest by ID) and count until
            // we have seen exactly completedLegalBalls legal deliveries.
            // All those balls (legal + any extras among them) = Inn1 balls.
            int completedLegalBalls = 0;
            for (Innings inn : dbInnings) {
                float op = inn.getOversPlayed();
                int fo = (int) op;
                // oversPlayed stored as e.g. 5.3 = 5 overs 3 balls (decimal digit × 1, NOT ×6)
                int eb = Math.round((op - fo) * 10.0f);
                if (eb > 5) eb = 5; // safety clamp
                completedLegalBalls += fo * 6 + eb;
            }

            // Two-pass: first find total ball count of Inn1 (legal + extras together)
            int inn1TotalBallCount = 0;
            if (completedLegalBalls > 0) {
                int legalSeen = 0;
                for (Ball b : allBalls) { // allBalls is oldest-first now
                    String t = b.getType() != null ? b.getType().trim().toLowerCase() : "";
                    boolean isExt = t.equals("wd") || t.equals("wide")
                                 || t.equals("nb") || t.equals("noball") || t.equals("no ball");
                    inn1TotalBallCount++;
                    if (!isExt) {
                        legalSeen++;
                        if (legalSeen >= completedLegalBalls) break;
                    }
                }
            }

            // ── Process only Inn2 (live) balls ───────────────────────────
            int liveRuns = 0, liveWickets = 0, liveLegalBalls = 0;
            Map<String, Map<String, Object>> batsmanMap = new LinkedHashMap<>();
            Map<String, Map<String, Object>> bowlerMap  = new LinkedHashMap<>();
            List<Map<String, Object>> liveBallEvents    = new ArrayList<>();

            for (int bi = inn1TotalBallCount; bi < allBalls.size(); bi++) {
                Ball b = allBalls.get(bi);

                String t       = b.getType() != null ? b.getType().trim().toLowerCase() : "";
                boolean isWide   = t.equals("wd")   || t.equals("wide");
                boolean isNoBall = t.equals("nb")   || t.equals("noball") || t.equals("no ball");
                boolean isBye    = t.equals("bye");
                boolean isExtra  = isWide || isNoBall;

                liveRuns += b.getRuns();
                if (b.isWicket()) liveWickets++;
                if (!isExtra)     liveLegalBalls++;

                Map<String, Object> ev = new HashMap<>();
                ev.put("display", formatBallDisplay(b));
                ev.put("legal",   !isExtra);
                liveBallEvents.add(ev);

                // ── Batsman stats ────────────────────────────────────────
                String bat = b.getBatsman();
                if (bat != null && !bat.isEmpty()) {
                    batsmanMap.computeIfAbsent(bat, k -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name", k); m.put("runs", 0); m.put("balls", 0);
                        m.put("fours", 0); m.put("sixes", 0); m.put("out", false); m.put("howOut", "");
                        return m;
                    });
                    Map<String, Object> bm = batsmanMap.get(bat);
                    if (!isWide && !isNoBall && !isBye) bm.put("balls", (int) bm.get("balls") + 1);
                    int batRuns = isNoBall ? Math.max(0, b.getRuns() - 1) : (!isExtra ? b.getRuns() : 0);
                    if (batRuns > 0) {
                        bm.put("runs",  (int) bm.get("runs")  + batRuns);
                        if (batRuns == 4) bm.put("fours", (int) bm.get("fours") + 1);
                        if (batRuns == 6) bm.put("sixes", (int) bm.get("sixes") + 1);
                    }
                    if (b.isWicket()) {
                        bm.put("out", true);
                        bm.put("howOut", b.getHowOut() != null ? b.getHowOut() : "out");
                    }
                }

                // ── Bowler stats ─────────────────────────────────────────
                String bow = b.getBowler();
                if (bow != null && !bow.isEmpty()) {
                    bowlerMap.computeIfAbsent(bow, k -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name", k); m.put("runs", 0); m.put("legalBalls", 0);
                        m.put("wickets", 0); m.put("wides", 0); m.put("noBalls", 0); m.put("maidens", 0);
                        return m;
                    });
                    Map<String, Object> bw = bowlerMap.get(bow);
                    int bowlerRuns = isBye ? 0 : b.getRuns();
                    bw.put("runs",       (int) bw.get("runs")       + bowlerRuns);
                    if (b.isWicket()) bw.put("wickets",  (int) bw.get("wickets")  + 1);
                    if (isWide)       bw.put("wides",    (int) bw.get("wides")    + 1);
                    if (isNoBall)     bw.put("noBalls",  (int) bw.get("noBalls")  + 1);
                    if (!isExtra)     bw.put("legalBalls",(int) bw.get("legalBalls") + 1);
                }
            }

            // Overs display e.g. 4.3
            int fullOvers  = liveLegalBalls / 6;
            int extraBalls = liveLegalBalls % 6;
            float oversPlayed = Float.parseFloat(String.format("%.1f", fullOvers + extraBalls / 10.0));

            Map<String, Object> liveInn = new HashMap<>();
            liveInn.put("battingTeam",  liveBattingTeam);
            liveInn.put("bowlingTeam",  liveBowlingTeam);
            liveInn.put("totalRuns",    liveRuns);
            liveInn.put("totalWickets", liveWickets);
            liveInn.put("oversPlayed",  oversPlayed);
            liveInn.put("extras",       0);
            liveInn.put("wides",        0);
            liveInn.put("noBalls",      0);
            liveInn.put("byes",         0);
            liveInn.put("live",         true);
            inningsList.add(liveInn);

            // Target banner for 2nd innings
            if (currentInningNum == 2 && !dbInnings.isEmpty()) {
                int target = dbInnings.get(0).getTotalRuns() + 1;
                res.put("target",     target);
                res.put("runsNeeded", Math.max(0, target - liveRuns));
            }

            // Live batters with SR
            List<Map<String, Object>> liveBatters = new ArrayList<>();
            for (Map<String, Object> bm : batsmanMap.values()) {
                int r = (int) bm.get("runs"), bl = (int) bm.get("balls");
                bm.put("strikeRate", bl > 0 ? Math.round(r * 1000.0 / bl) / 10.0 : 0.0);
                liveBatters.add(bm);
            }
            res.put("liveBatters", liveBatters);

            // Live bowlers with overs + economy
            List<Map<String, Object>> liveBowlers = new ArrayList<>();
            for (Map<String, Object> bw : bowlerMap.values()) {
                int lb = (int) bw.get("legalBalls");
                double ov = lb / 6 + (lb % 6) / 10.0;
                bw.put("overs",   Math.round(ov * 10.0) / 10.0);
                int r = (int) bw.get("runs");
                bw.put("economy", ov > 0 ? Math.round(r / ov * 100.0) / 100.0 : 0.0);
                liveBowlers.add(bw);
            }
            res.put("liveBowlers", liveBowlers);

            // Recent overs blocks
            List<List<String>> completedOvers = new ArrayList<>();
            List<String> currentOver = new ArrayList<>();
            int legalInCurrent = 0;
            for (Map<String, Object> ev : liveBallEvents) {
                currentOver.add(String.valueOf(ev.get("display")));
                boolean legal = Boolean.TRUE.equals(ev.get("legal"));
                if (legal) legalInCurrent++;
                if (legalInCurrent == 6) {
                    completedOvers.add(new ArrayList<>(currentOver));
                    currentOver.clear();
                    legalInCurrent = 0;
                }
            }
            List<Map<String, Object>> overBlocks = new ArrayList<>();
            int totalCompleted = completedOvers.size();
            int from = Math.max(0, totalCompleted - 2);
            for (int i = from; i < totalCompleted; i++) {
                Map<String, Object> block = new HashMap<>();
                block.put("label", "Ov " + (i + 1));
                block.put("balls", completedOvers.get(i));
                block.put("current", false);
                overBlocks.add(block);
            }
            Map<String, Object> cur = new HashMap<>();
            cur.put("label", "Current Ov " + (totalCompleted + 1));
            cur.put("balls", currentOver);
            cur.put("current", true);
            overBlocks.add(cur);
            res.put("recentOverBlocks", overBlocks);
        }

        res.put("innings", inningsList);

        // ── Completed innings scorecard from DB ──────────────────────────
        List<Scorecard> scs = scorecardRepository.findByMatch_MatchId(matchId);
        List<Map<String, Object>> scList = new ArrayList<>();
        for (Scorecard sc : scs) {
            Map<String, Object> s = new HashMap<>();
            s.put("playerName",  sc.getPlayer() != null ? sc.getPlayer().getPlayerName() : "");
            s.put("teamName",    sc.getTeam()   != null ? sc.getTeam().getTeamName()     : "");
            s.put("innings",     sc.getInnings());
            s.put("runs",        sc.getRuns());
            s.put("balls",       sc.getBalls());
            s.put("fours",       sc.getFours());
            s.put("sixes",       sc.getSixes());
            s.put("strikeRate",  sc.getStrikeRate());
            s.put("out",         sc.isOut());
            s.put("howOut",      sc.getHowOut() != null ? sc.getHowOut() : "");
            s.put("wickets",     sc.getWickets());
            s.put("oversBowled", sc.getOversBowled());
            s.put("runsGiven",   sc.getRunsGiven());
            s.put("economy",     sc.getEconomy());
            s.put("maidens",     sc.getMaidens());
            s.put("noBalls",     sc.getNoBalls());
            s.put("wides",       sc.getWides());
            scList.add(s);
        }
        res.put("scorecards", scList);

        // ── Recent balls (last 12, newest first for display) ────────────
        List<Ball> newestFirst = new ArrayList<>(allBalls);
        Collections.reverse(newestFirst);
        List<Map<String, Object>> ballList = new ArrayList<>();
        newestFirst.stream().limit(12).forEach(b -> {
            Map<String, Object> bm = new HashMap<>();
            bm.put("over",    b.getOverNumber());
            bm.put("ball",    b.getBallNumber());
            bm.put("runs",    b.getRuns());
            bm.put("type",    b.getType() != null ? b.getType() : "normal");
            bm.put("display", formatBallDisplay(b));
            bm.put("wicket",  b.isWicket());
            bm.put("batsman", b.getBatsman() != null ? b.getBatsman() : "");
            bm.put("bowler",  b.getBowler()  != null ? b.getBowler()  : "");
            ballList.add(bm);
        });
        res.put("recentBalls", ballList);

        // ── Current batsman & bowler ─────────────────────────────────────
        if (!newestFirst.isEmpty()) {
            Ball latest = newestFirst.get(0);
            res.put("currentBatsman", latest.getBatsman() != null ? latest.getBatsman() : "");
            res.put("currentBowler",  latest.getBowler()  != null ? latest.getBowler()  : "");
        }

        return res;
    }

    @GetMapping(value = "/api/public/live/stream/{matchId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter liveScoreStream(@PathVariable int matchId) {
        return liveScoreBroadcaster.subscribe(matchId);
    }

    private String formatBallDisplay(Ball b) {
        if (b == null) return "";
        if (b.isWicket()) return "W";

        String t = b.getType() == null ? "" : b.getType().trim().toLowerCase();
        int runs = Math.max(0, b.getRuns());

        if (t.equals("wd") || t.equals("wide"))
            return runs > 1 ? "Wd+" + (runs - 1) : "Wd";
        if (t.equals("nb") || t.equals("noball") || t.equals("no ball"))
            return runs > 1 ? "Nb+" + (runs - 1) : "Nb";
        if (t.equals("bye") || t.equals("b"))
            return "b" + runs;
        if (t.equals("legbye") || t.equals("leg bye") || t.equals("lb"))
            return "lb" + runs;

        return String.valueOf(runs);
    }
}