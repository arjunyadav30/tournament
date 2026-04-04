package com.example.Tournament.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "innings")
public class Innings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int inningsId;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "batting_team")
    private Team battingTeam;

    @ManyToOne
    @JoinColumn(name = "bowling_team")
    private Team bowlingTeam;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int totalRuns = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int totalWickets = 0;

    @Column(nullable = false, columnDefinition = "FLOAT DEFAULT 0")
    private float oversPlayed = 0f;

    // FIX: these were missing — caused NULL -> primitive int crash
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int extras = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int wides = 0;

    @Column(name = "no_balls", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int noBalls = 0;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int byes = 0;

    // ── Constructors ──────────────────────────────────────────────────────
    public Innings() {}

    public Innings(Match match, Team battingTeam, Team bowlingTeam) {
        this.match       = match;
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int getInningsId() { return inningsId; }
    public void setInningsId(int inningsId) { this.inningsId = inningsId; }

    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }

    public Team getBattingTeam() { return battingTeam; }
    public void setBattingTeam(Team battingTeam) { this.battingTeam = battingTeam; }

    public Team getBowlingTeam() { return bowlingTeam; }
    public void setBowlingTeam(Team bowlingTeam) { this.bowlingTeam = bowlingTeam; }

    public int getTotalRuns() { return totalRuns; }
    public void setTotalRuns(int totalRuns) { this.totalRuns = totalRuns; }

    public int getTotalWickets() { return totalWickets; }
    public void setTotalWickets(int totalWickets) { this.totalWickets = totalWickets; }

    public float getOversPlayed() { return oversPlayed; }
    public void setOversPlayed(float oversPlayed) { this.oversPlayed = oversPlayed; }

    public int getExtras() { return extras; }
    public void setExtras(int extras) { this.extras = extras; }

    public int getWides() { return wides; }
    public void setWides(int wides) { this.wides = wides; }

    public int getNoBalls() { return noBalls; }
    public void setNoBalls(int noBalls) { this.noBalls = noBalls; }

    public int getByes() { return byes; }
    public void setByes(int byes) { this.byes = byes; }
}