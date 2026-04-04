package com.example.Tournament.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int matchId;

    // 🔥 Team 1
    @ManyToOne
    @JoinColumn(name = "team1_id")
    private Team team1;

    // 🔥 Team 2
    @ManyToOne
    @JoinColumn(name = "team2_id")
    private Team team2;

    private LocalDate matchDate;

    private LocalTime matchTime;

    private int overs;
    private int maxOversPerBowler;
    @Column(length = 1000)
    private String team1Squad;
    @Column(length = 1000)
    private String team2Squad;

    private String venue;

    // 🔥 Toss Winner
    @ManyToOne
    @JoinColumn(name = "toss_winner")
    private Team tossWinner;

    // 🔥 Toss Decision (Bat/Bowl)
    @Column(nullable = true)
    private String tossDecision;

    // 🔥 Match Winner
    @ManyToOne
    @JoinColumn(name = "match_winner")
    private Team matchWinner;

    private String status;

    // 🔥 Tournament Reference (Optional for Friendly Matches)
    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    // 🔹 Constructors
    public Match() {}

    // 🔹 Getters & Setters
    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public Team getTeam1() {
        return team1;
    }

    public void setTeam1(Team team1) {
        this.team1 = team1;
    }

    public Team getTeam2() {
        return team2;
    }

    public void setTeam2(Team team2) {
        this.team2 = team2;
    }

    public LocalDate getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDate matchDate) {
        this.matchDate = matchDate;
    }

    public LocalTime getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(LocalTime matchTime) {
        this.matchTime = matchTime;
    }

    public int getOvers() {
        return overs;
    }

    public void setOvers(int overs) {
        this.overs = overs;
    }

    public int getMaxOversPerBowler() {
        return maxOversPerBowler;
    }

    public void setMaxOversPerBowler(int maxOversPerBowler) {
        this.maxOversPerBowler = maxOversPerBowler;
    }

    public String getTeam1Squad() {
        return team1Squad;
    }

    public void setTeam1Squad(String team1Squad) {
        this.team1Squad = team1Squad;
    }

    public String getTeam2Squad() {
        return team2Squad;
    }

    public void setTeam2Squad(String team2Squad) {
        this.team2Squad = team2Squad;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public Team getTossWinner() {
        return tossWinner;
    }

    public void setTossWinner(Team tossWinner) {
        this.tossWinner = tossWinner;
    }

    // 🔥 TOSS DECISION GETTERS & SETTERS
    public String getTossDecision() {
        return tossDecision;
    }

    public void setTossDecision(String tossDecision) {
        this.tossDecision = tossDecision;
    }

    public Team getMatchWinner() {
        return matchWinner;
    }

    public void setMatchWinner(Team matchWinner) {
        this.matchWinner = matchWinner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }
}