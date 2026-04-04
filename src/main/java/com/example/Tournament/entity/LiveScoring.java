package com.example.Tournament.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ball_by_ball")
public class LiveScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ballId;

    // 🔥 Match FK
    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    private int innings;

    private int overNumber;

    private int ballNumber;

    // 🔥 Batting Team
    @ManyToOne
    @JoinColumn(name = "batting_team_id")
    private Team battingTeam;

    // 🔥 Bowling Team
    @ManyToOne
    @JoinColumn(name = "bowling_team_id")
    private Team bowlingTeam;

    // 🔥 Players
    @ManyToOne
    @JoinColumn(name = "striker_id")
    private Player striker;

    @ManyToOne
    @JoinColumn(name = "non_striker_id")
    private Player nonStriker;

    @ManyToOne
    @JoinColumn(name = "bowler_id")
    private Player bowler;

    private int runsScored;

    private int extraRuns;

    private String extraType;

    private boolean wicket;

    private String wicketType;

    // 🔥 Out Player
    @ManyToOne
    @JoinColumn(name = "out_player_id")
    private Player outPlayer;

    @Column(columnDefinition = "TEXT")
    private String commentary;

    // 🔹 Constructors
    public LiveScoring() {}

    // 🔹 Getters & Setters

    public int getBallId() {
        return ballId;
    }

    public void setBallId(int ballId) {
        this.ballId = ballId;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public int getInnings() {
        return innings;
    }

    public void setInnings(int innings) {
        this.innings = innings;
    }

    public int getOverNumber() {
        return overNumber;
    }

    public void setOverNumber(int overNumber) {
        this.overNumber = overNumber;
    }

    public int getBallNumber() {
        return ballNumber;
    }

    public void setBallNumber(int ballNumber) {
        this.ballNumber = ballNumber;
    }

    public Team getBattingTeam() {
        return battingTeam;
    }

    public void setBattingTeam(Team battingTeam) {
        this.battingTeam = battingTeam;
    }

    public Team getBowlingTeam() {
        return bowlingTeam;
    }

    public void setBowlingTeam(Team bowlingTeam) {
        this.bowlingTeam = bowlingTeam;
    }

    public Player getStriker() {
        return striker;
    }

    public void setStriker(Player striker) {
        this.striker = striker;
    }

    public Player getNonStriker() {
        return nonStriker;
    }

    public void setNonStriker(Player nonStriker) {
        this.nonStriker = nonStriker;
    }

    public Player getBowler() {
        return bowler;
    }

    public void setBowler(Player bowler) {
        this.bowler = bowler;
    }

    public int getRunsScored() {
        return runsScored;
    }

    public void setRunsScored(int runsScored) {
        this.runsScored = runsScored;
    }

    public int getExtraRuns() {
        return extraRuns;
    }

    public void setExtraRuns(int extraRuns) {
        this.extraRuns = extraRuns;
    }

    public String getExtraType() {
        return extraType;
    }

    public void setExtraType(String extraType) {
        this.extraType = extraType;
    }

    public boolean isWicket() {
        return wicket;
    }

    public void setWicket(boolean wicket) {
        this.wicket = wicket;
    }

    public String getWicketType() {
        return wicketType;
    }

    public void setWicketType(String wicketType) {
        this.wicketType = wicketType;
    }

    public Player getOutPlayer() {
        return outPlayer;
    }

    public void setOutPlayer(Player outPlayer) {
        this.outPlayer = outPlayer;
    }

    public String getCommentary() {
        return commentary;
    }

    public void setCommentary(String commentary) {
        this.commentary = commentary;
    }
}