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
import com.example.Tournament.repository.BallRepository;
import com.example.Tournament.repository.InningsRepository;
import com.example.Tournament.repository.MatchRepository;
import com.example.Tournament.repository.PlayerRepository;
import com.example.Tournament.repository.ScorecardRepository;
import com.example.Tournament.repository.TeamRepository;
import com.example.Tournament.service.LiveScoreBroadcaster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class ScoringController {

    // Per-match last-updated timestamp (epoch ms)
    public static final Map<Integer, Long> lastBallTime = new ConcurrentHashMap<>();

    @Autowired private BallRepository ballRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private ScorecardRepository scorecardRepository;
    @Autowired private InningsRepository inningsRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private LiveScoreBroadcaster liveScoreBroadcaster;

    @GetMapping("/admin/scoring")
    public String showLiveScoring(Model model) {
        model.addAttribute("matches", matchRepository.findAll());
        return "admin/live-scoring";
    }

    // ── UNDO LAST BALL ────────────────────────────────────────────────────
    @PostMapping("/api/score/undo/{matchId}")
    @ResponseBody
    @Transactional
    public Map<String, Object> undoLastBall(@PathVariable int matchId) {
        Map<String, Object> res = new HashMap<>();
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null) {
            res.put("success", false);
            res.put("message", "Match not found");
            return res;
        }

        Ball lastBall = ballRepository.findTopByMatchMatchIdOrderByIdDesc(matchId).orElse(null);
        if (lastBall == null) {
            res.put("success", false);
            res.put("message", "No ball to undo");
            return res;
        }

        ballRepository.delete(lastBall);
        int totalBalls = ballRepository.findByMatchMatchId(matchId).size();
        lastBallTime.put(matchId, System.currentTimeMillis());
        liveScoreBroadcaster.broadcastBallUpdate(matchId, totalBalls);

        res.put("success", true);
        res.put("message", "Last ball undone");
        res.put("totalBalls", totalBalls);
        return res;
    }

    // ── RESET MATCH ───────────────────────────────────────────────────────
    @PostMapping("/api/score/reset/{matchId}")
    @ResponseBody
    @Transactional
    public Map<String, Object> resetMatchScore(@PathVariable int matchId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Match match = matchRepository.findById(matchId).orElse(null);
            if (match == null) {
                res.put("success", false);
                res.put("message", "Match not found");
                return res;
            }

            scorecardRepository.deleteAll(scorecardRepository.findByMatch_MatchId(matchId));
            inningsRepository.deleteAll(inningsRepository.findByMatchMatchId(matchId));
            ballRepository.deleteAll(ballRepository.findByMatchMatchId(matchId));

            match.setMatchWinner(null);
            if (match.getTossWinner() != null && match.getTossDecision() != null) {
                match.setStatus("Live");
            } else {
                match.setStatus("Upcoming");
            }
            matchRepository.save(match);

            lastBallTime.put(matchId, System.currentTimeMillis());
            liveScoreBroadcaster.broadcastMatchUpdate(matchId, match.getStatus());
            liveScoreBroadcaster.broadcastBallUpdate(matchId, 0);

            res.put("success", true);
            res.put("message", "Match score reset");
            res.put("status", match.getStatus());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Reset failed: " + e.getMessage());
        }
        return res;
    }

    // ── ADD BALL ──────────────────────────────────────────────────────────
    @PostMapping("/api/score/add")
    @ResponseBody
    public String addBall(@RequestBody BallRequest request) {
        Match match = matchRepository.findById(request.getMatchId()).orElse(null);
        if (match == null) return "Match not found";

        Player bowlerPlayer = null;
        if (request.getBowlerId() > 0) {
            bowlerPlayer = playerRepository.findById(request.getBowlerId()).orElse(null);
        }

        // Bowler over-limit check
        int maxOversPerBowler = match.getMaxOversPerBowler();
        if (maxOversPerBowler > 0 && bowlerPlayer != null && isLegalBallType(request.getType())) {
            int legalBallsByBowler = 0;
            for (Ball b : ballRepository.findByMatchMatchId(request.getMatchId())) {
                if (b.getBowler() == null) continue;
                if (!b.getBowler().equals(bowlerPlayer.getPlayerName())) continue;
                if (isLegalBallType(b.getType())) legalBallsByBowler++;
            }
            if (legalBallsByBowler >= (maxOversPerBowler * 6)) {
                return "Bowler over limit reached";
            }
        }

        Ball ball = new Ball();
        ball.setMatch(match);
        ball.setRuns(request.getRuns());
        ball.setType(request.getType());
        ball.setWicket(request.isWicket());
        ball.setHowOut(request.getHowOut());
        ball.setOverNumber(request.getOverNumber());
        ball.setBallNumber(request.getBallNumber());

        if (request.getBatsmanId() > 0) {
            Player b = playerRepository.findById(request.getBatsmanId()).orElse(null);
            ball.setBatsman(b != null ? b.getPlayerName() : "");
        }
        if (request.getBowlerId() > 0) {
            Player b = playerRepository.findById(request.getBowlerId()).orElse(null);
            ball.setBowler(b != null ? b.getPlayerName() : "");
        }

        ballRepository.save(ball);

        lastBallTime.put(request.getMatchId(), System.currentTimeMillis());
        int totalBalls = ballRepository.findByMatchMatchId(request.getMatchId()).size();
        liveScoreBroadcaster.broadcastBallUpdate(request.getMatchId(), totalBalls);
        return "Saved";
    }

    private boolean isLegalBallType(String type) {
        if (type == null) return true;
        String t = type.trim().toLowerCase();
        return !t.equals("wd") && !t.equals("wide")
            && !t.equals("nb") && !t.equals("noball") && !t.equals("no ball");
    }

    // ── END INNING ────────────────────────────────────────────────────────
    @PostMapping("/api/score/end-inning")
    @ResponseBody
    public Map<String, Object> endInning(@RequestBody InningRequest request) {
        Map<String, Object> response = new HashMap<>();

        Match match = matchRepository.findById(request.getMatchId()).orElse(null);
        if (match == null) {
            response.put("success", false);
            response.put("message", "Match not found");
            return response;
        }

        Team battingTeam = teamRepository.findById(request.getBattingTeamId()).orElse(null);
        Team bowlingTeam = (battingTeam != null
                && battingTeam.getTeamId() == match.getTeam1().getTeamId())
                ? match.getTeam2() : match.getTeam1();

        // ── Save Innings to DB ─────────────────────────────────────────
        Innings innings = new Innings();
        innings.setMatch(match);
        innings.setBattingTeam(battingTeam);
        innings.setBowlingTeam(bowlingTeam);
        innings.setTotalRuns(request.getTotalRuns());
        innings.setTotalWickets(request.getTotalWickets());
        innings.setOversPlayed(request.getOversPlayed());
        // FIX: extras were not being saved before
        innings.setExtras(request.getExtras());
        innings.setWides(request.getWides());
        innings.setNoBalls(request.getNoBalls());
        innings.setByes(request.getByes());
        inningsRepository.save(innings);

        // ── Save Batters Scorecard ─────────────────────────────────────
        // BUG FIX: track updated player IDs to prevent double matchesPlayed increment
        java.util.Set<Integer> careerUpdatedBatters = new java.util.HashSet<>();

        for (BatterData b : request.getBatters()) {
            if (b.getPlayerId() <= 0) continue;
            Player player = playerRepository.findById(b.getPlayerId()).orElse(null);
            if (player == null) continue;

            Scorecard sc = new Scorecard();
            sc.setMatch(match);
            sc.setPlayer(player);
            sc.setTeam(player.getTeam());
            sc.setInnings(request.getInning());

            // Batting stats
            sc.setRuns(b.getRuns());
            sc.setBalls(b.getBalls());
            sc.setFours(b.getFours());
            sc.setSixes(b.getSixes());
            sc.setStrikeRate(b.getBalls() > 0
                    ? (float) (b.getRuns() * 100.0 / b.getBalls()) : 0f);
            sc.setOut(b.isOut());
            sc.setHowOut(b.getHowOut());

            scorecardRepository.save(sc);

            // Update player career stats — only increment matchesPlayed once per player
            if (!careerUpdatedBatters.contains(b.getPlayerId())) {
                careerUpdatedBatters.add(b.getPlayerId());
                player.setMatchesPlayed(player.getMatchesPlayed() + 1);
            }
            player.setTotalRuns(player.getTotalRuns() + b.getRuns());
            player.setBallsPlayed(player.getBallsPlayed() + b.getBalls());
            int tb = player.getBallsPlayed();
            int tr = player.getTotalRuns();
            player.setStrikeRate(tb > 0 ? (float) (tr * 100.0 / tb) : 0f);
            if (b.getRuns() >= 100)      player.setHundreds(player.getHundreds() + 1);
            else if (b.getRuns() >= 50)  player.setFifties(player.getFifties() + 1);
            playerRepository.save(player);
        }

        // ── Save Bowlers Scorecard ─────────────────────────────────────
        for (BowlerData bw : request.getBowlers()) {
            if (bw.getPlayerId() <= 0) continue;
            Player player = playerRepository.findById(bw.getPlayerId()).orElse(null);
            if (player == null) continue;

            // Reuse existing scorecard row if player also batted THIS innings
            // BUG FIX: was using findByMatch_MatchIdAndPlayer_PlayerId (non-innings-aware)
            // which caused Inning 1 batting stats to be overwritten when player bowls in Inning 2
            Scorecard sc = scorecardRepository
                    .findByMatch_MatchIdAndPlayer_PlayerIdAndInnings(
                            match.getMatchId(), player.getPlayerId(), request.getInning())
                    .orElse(new Scorecard());

            sc.setMatch(match);
            sc.setPlayer(player);
            sc.setTeam(player.getTeam());
            // FIX: innings number
            sc.setInnings(request.getInning());

            // Bowling stats
            sc.setWickets(bw.getWickets());
            // oversBowled stored as e.g. 3.4 (3 overs 4 balls)
            sc.setOversBowled((float) (bw.getOvers() + bw.getBalls() / 10.0));
            sc.setRunsGiven(bw.getRuns());
            // FIX: maidens, noBalls, wides were missing
            sc.setMaidens(bw.getMaidens());
            sc.setNoBalls(bw.getNb());
            sc.setWides(bw.getWd());

            float totalOversFloat = bw.getOvers() + bw.getBalls() / 6.0f;
            float eco = totalOversFloat > 0 ? bw.getRuns() / totalOversFloat : 0f;
            sc.setEconomy(eco);

            scorecardRepository.save(sc);

            // Update player career bowling stats
            player.setTotalWickets(player.getTotalWickets() + bw.getWickets());
            player.setEconomy(totalOversFloat > 0 ? bw.getRuns() / totalOversFloat : 0f);
            playerRepository.save(player);
        }

        // ── Inning 1 complete → start inning 2 ────────────────────────
        if (request.getInning() == 1) {
            match.setStatus("Live");
            matchRepository.save(match);
            liveScoreBroadcaster.broadcastMatchUpdate(request.getMatchId(), "Live");

            int target = request.getTotalRuns() + 1;
            response.put("success", true);
            response.put("inning", 1);
            response.put("message", "1st Inning Saved!");
            response.put("target", target);
            response.put("nextInning", 2);
            response.put("nextBattingTeamId",
                    bowlingTeam != null ? bowlingTeam.getTeamId() : 0);
            response.put("nextBattingTeamName",
                    bowlingTeam != null ? bowlingTeam.getTeamName() : "");
            response.put("nextBowlingTeamName",
                    battingTeam != null ? battingTeam.getTeamName() : "");

        // ── Inning 2 complete → decide winner ─────────────────────────
        } else {
            List<Innings> allInnings = inningsRepository.findByMatchMatchId(request.getMatchId());
            int team1Runs = 0, team2Runs = 0;
            // BUG FIX: track which team batted first and second from DB innings order,
            // so "won by wickets" is credited to the correct chasing team regardless of
            // toss outcome. Old code always said team2 won by wickets when team2Runs > team1Runs,
            // but team1 could be the chasing team if they batted second.
            Team firstBattingTeam  = null;
            Team secondBattingTeam = null;
            for (Innings inn : allInnings) {
                if (inn.getBattingTeam() == null) continue;
                if (inn.getBattingTeam().getTeamId() == match.getTeam1().getTeamId())
                    team1Runs = inn.getTotalRuns();
                else
                    team2Runs = inn.getTotalRuns();
                // First innings saved = first batter; second = chasing team
                if (firstBattingTeam == null)  firstBattingTeam  = inn.getBattingTeam();
                else                           secondBattingTeam = inn.getBattingTeam();
            }
            // Fallback if innings data incomplete
            if (secondBattingTeam == null) secondBattingTeam = battingTeam;

            Team winner;
            String resultMsg;
            if (team1Runs > team2Runs) {
                winner    = match.getTeam1();
                // team1 batted first and won → they won by runs
                if (firstBattingTeam != null && firstBattingTeam.getTeamId() == match.getTeam1().getTeamId()) {
                    resultMsg = match.getTeam1().getTeamName()
                            + " won by " + (team1Runs - team2Runs) + " runs!";
                } else {
                    // team1 chased and won
                    resultMsg = match.getTeam1().getTeamName()
                            + " won by " + (10 - request.getTotalWickets()) + " wickets!";
                }
            } else if (team2Runs > team1Runs) {
                winner    = match.getTeam2();
                if (firstBattingTeam != null && firstBattingTeam.getTeamId() == match.getTeam2().getTeamId()) {
                    resultMsg = match.getTeam2().getTeamName()
                            + " won by " + (team2Runs - team1Runs) + " runs!";
                } else {
                    // team2 chased and won
                    resultMsg = match.getTeam2().getTeamName()
                            + " won by " + (10 - request.getTotalWickets()) + " wickets!";
                }
            } else {
                winner    = null;
                resultMsg = "Match Tied!";
            }

            match.setMatchWinner(winner);
            match.setStatus("Completed");
            matchRepository.save(match);
            liveScoreBroadcaster.broadcastMatchUpdate(request.getMatchId(), "Completed");

            // Update team win/loss/match stats
            Team team1 = match.getTeam1();
            Team team2 = match.getTeam2();
            if (team1 != null && team2 != null) {
                team1.setTotalMatches(team1.getTotalMatches() + 1);
                team2.setTotalMatches(team2.getTotalMatches() + 1);
                if (winner != null) {
                    if (winner.getTeamId() == team1.getTeamId()) {
                        team1.setTotalWins(team1.getTotalWins() + 1);
                        team2.setTotalLosses(team2.getTotalLosses() + 1);
                    } else {
                        team2.setTotalWins(team2.getTotalWins() + 1);
                        team1.setTotalLosses(team1.getTotalLosses() + 1);
                    }
                }
                teamRepository.save(team1);
                teamRepository.save(team2);
            }

            response.put("success", true);
            response.put("inning", 2);
            response.put("message", resultMsg);
            response.put("completed", true);
        }

        return response;
    }

    // ── MATCH STATE API (for scoring page refresh recovery) ───────────────
    @GetMapping("/api/score/state/{matchId}")
    @ResponseBody
    public Map<String, Object> getMatchState(@PathVariable int matchId) {
        Map<String, Object> response = new HashMap<>();
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null) {
            response.put("found", false);
            return response;
        }

        List<Innings> allInnings = inningsRepository.findByMatchMatchId(matchId);
        response.put("found", true);
        response.put("matchId", matchId);
        response.put("status", match.getStatus());
        response.put("tossWinnerId",
                match.getTossWinner() != null ? match.getTossWinner().getTeamId() : 0);
        response.put("tossDecision", match.getTossDecision());
        response.put("team1Id", match.getTeam1().getTeamId());
        response.put("team2Id", match.getTeam2().getTeamId());
        response.put("team1Name", match.getTeam1().getTeamName());
        response.put("team2Name", match.getTeam2().getTeamName());
        response.put("overs", match.getOvers());
        response.put("inningsCount", allInnings.size());

        if (!allInnings.isEmpty()) {
            Innings inn1 = allInnings.get(0);
            Map<String, Object> inn1Data = new HashMap<>();
            inn1Data.put("battingTeamId",
                    inn1.getBattingTeam() != null ? inn1.getBattingTeam().getTeamId() : 0);
            inn1Data.put("battingTeamName",
                    inn1.getBattingTeam() != null ? inn1.getBattingTeam().getTeamName() : "");
            inn1Data.put("totalRuns", inn1.getTotalRuns());
            inn1Data.put("totalWickets", inn1.getTotalWickets());
            inn1Data.put("oversPlayed", inn1.getOversPlayed());
            response.put("innings1", inn1Data);
            response.put("target", inn1.getTotalRuns() + 1);
        }

        return response;
    }

    // ======= REQUEST / DTO CLASSES ========================================

    public static class BallRequest {
        private int matchId, runs, batsmanId, bowlerId, overNumber, ballNumber, outBatterSlot;
        private String type, howOut;
        private boolean wicket;

        public int getMatchId()       { return matchId; }
        public void setMatchId(int v) { matchId = v; }

        public int getRuns()       { return runs; }
        public void setRuns(int v) { runs = v; }

        public String getType()          { return type; }
        public void setType(String v)    { type = v; }

        public boolean isWicket()        { return wicket; }
        public void setWicket(boolean v) { wicket = v; }

        public String getHowOut()        { return howOut; }
        public void setHowOut(String v)  { howOut = v; }

        public int getOutBatterSlot()       { return outBatterSlot; }
        public void setOutBatterSlot(int v) { outBatterSlot = v; }

        public int getBatsmanId()       { return batsmanId; }
        public void setBatsmanId(int v) { batsmanId = v; }

        public int getBowlerId()       { return bowlerId; }
        public void setBowlerId(int v) { bowlerId = v; }

        public int getOverNumber()       { return overNumber; }
        public void setOverNumber(int v) { overNumber = v; }

        public int getBallNumber()       { return ballNumber; }
        public void setBallNumber(int v) { ballNumber = v; }
    }

    public static class InningRequest {
        private int matchId, inning, battingTeamId, totalRuns, totalWickets;
        private int extras, wides, noBalls, byes;
        private float oversPlayed;
        private List<BatterData> batters;
        private List<BowlerData> bowlers;

        public int getMatchId()           { return matchId; }
        public void setMatchId(int v)     { matchId = v; }

        public int getInning()            { return inning; }
        public void setInning(int v)      { inning = v; }

        public int getBattingTeamId()        { return battingTeamId; }
        public void setBattingTeamId(int v)  { battingTeamId = v; }

        public int getTotalRuns()         { return totalRuns; }
        public void setTotalRuns(int v)   { totalRuns = v; }

        public int getTotalWickets()      { return totalWickets; }
        public void setTotalWickets(int v){ totalWickets = v; }

        public float getOversPlayed()        { return oversPlayed; }
        public void setOversPlayed(float v)  { oversPlayed = v; }

        public int getExtras()       { return extras; }
        public void setExtras(int v) { extras = v; }

        public int getWides()       { return wides; }
        public void setWides(int v) { wides = v; }

        public int getNoBalls()       { return noBalls; }
        public void setNoBalls(int v) { noBalls = v; }

        public int getByes()       { return byes; }
        public void setByes(int v) { byes = v; }

        public List<BatterData> getBatters()             { return batters; }
        public void setBatters(List<BatterData> v)       { batters = v; }

        public List<BowlerData> getBowlers()             { return bowlers; }
        public void setBowlers(List<BowlerData> v)       { bowlers = v; }
    }

    public static class BatterData {
        private int playerId, runs, balls, fours, sixes;
        private boolean out;
        private String howOut;

        public int getPlayerId()       { return playerId; }
        public void setPlayerId(int v) { playerId = v; }

        public int getRuns()       { return runs; }
        public void setRuns(int v) { runs = v; }

        public int getBalls()       { return balls; }
        public void setBalls(int v) { balls = v; }

        public int getFours()       { return fours; }
        public void setFours(int v) { fours = v; }

        public int getSixes()       { return sixes; }
        public void setSixes(int v) { sixes = v; }

        public boolean isOut()        { return out; }
        public void setOut(boolean v) { out = v; }

        public String getHowOut()       { return howOut; }
        public void setHowOut(String v) { howOut = v; }
    }

    public static class BowlerData {
        private int playerId, overs, balls, runs, wickets, nb, wd, maidens;

        public int getPlayerId()       { return playerId; }
        public void setPlayerId(int v) { playerId = v; }

        public int getOvers()       { return overs; }
        public void setOvers(int v) { overs = v; }

        public int getBalls()       { return balls; }
        public void setBalls(int v) { balls = v; }

        public int getRuns()       { return runs; }
        public void setRuns(int v) { runs = v; }

        public int getWickets()       { return wickets; }
        public void setWickets(int v) { wickets = v; }

        public int getNb()       { return nb; }
        public void setNb(int v) { nb = v; }

        public int getWd()       { return wd; }
        public void setWd(int v) { wd = v; }

        public int getMaidens()       { return maidens; }
        public void setMaidens(int v) { maidens = v; }
    }
}