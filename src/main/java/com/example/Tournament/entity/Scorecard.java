package com.example.Tournament.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "scorecard")
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int scorecardId;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    // FIX: default = 0 — prevents NULL -> primitive int NPE crash
    @Column(name = "innings", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int innings = 0;

    // ── BATTING ───────────────────────────────────────────────────────────
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int runs = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int balls = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int fours = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int sixes = 0;

    @Column(nullable = false, columnDefinition = "FLOAT DEFAULT 0")
    private float strikeRate = 0f;

    // FIX: was missing — Thymeleaf sc.out caused 500 error
    @Column(name = "is_out", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean out = false;

    @Column(name = "how_out")
    private String howOut;

    // ── BOWLING ───────────────────────────────────────────────────────────
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int wickets = 0;

    @Column(nullable = false, columnDefinition = "FLOAT DEFAULT 0")
    private float oversBowled = 0f;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int runsGiven = 0;

    @Column(nullable = false, columnDefinition = "FLOAT DEFAULT 0")
    private float economy = 0f;

    // FIX: was missing — needed for bowling display
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int maidens = 0;

    @Column(name = "no_balls", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int noBalls = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int wides = 0;

    // ── FIELDING ──────────────────────────────────────────────────────────
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int catches = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int runOuts = 0;

    // ── Constructors ──────────────────────────────────────────────────────
    public Scorecard() {}

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int getScorecardId() { return scorecardId; }
    public void setScorecardId(int scorecardId) { this.scorecardId = scorecardId; }

    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public int getInnings() { return innings; }
    public void setInnings(int innings) { this.innings = innings; }

    public int getRuns() { return runs; }
    public void setRuns(int runs) { this.runs = runs; }

    public int getBalls() { return balls; }
    public void setBalls(int balls) { this.balls = balls; }

    public int getFours() { return fours; }
    public void setFours(int fours) { this.fours = fours; }

    public int getSixes() { return sixes; }
    public void setSixes(int sixes) { this.sixes = sixes; }

    public float getStrikeRate() { return strikeRate; }
    public void setStrikeRate(float strikeRate) { this.strikeRate = strikeRate; }

    public boolean isOut() { return out; }
    public void setOut(boolean out) { this.out = out; }

    public String getHowOut() { return howOut; }
    public void setHowOut(String howOut) { this.howOut = howOut; }

    public int getWickets() { return wickets; }
    public void setWickets(int wickets) { this.wickets = wickets; }

    public float getOversBowled() { return oversBowled; }
    public void setOversBowled(float oversBowled) { this.oversBowled = oversBowled; }

    public int getRunsGiven() { return runsGiven; }
    public void setRunsGiven(int runsGiven) { this.runsGiven = runsGiven; }

    public float getEconomy() { return economy; }
    public void setEconomy(float economy) { this.economy = economy; }

    public int getMaidens() { return maidens; }
    public void setMaidens(int maidens) { this.maidens = maidens; }

    public int getNoBalls() { return noBalls; }
    public void setNoBalls(int noBalls) { this.noBalls = noBalls; }

    public int getWides() { return wides; }
    public void setWides(int wides) { this.wides = wides; }

    public int getCatches() { return catches; }
    public void setCatches(int catches) { this.catches = catches; }

    public int getRunOuts() { return runOuts; }
    public void setRunOuts(int runOuts) { this.runOuts = runOuts; }
}