package com.example.Tournament.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int teamId;

    @Column(nullable = false)
    private String teamName;

    private String village;

    private String captainName;

    private int totalMatches;

    private int totalWins;

    private int totalLosses;

    private String status;

    // 🔥 Tournament Reference (Optional - teams can play in multiple tournaments)
    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    // 🔹 Constructors
    public Team() {}

    public Team(String teamName, String village, String captainName) {
        this.teamName = teamName;
        this.village = village;
        this.captainName = captainName;
    }

    // 🔹 Getters & Setters
    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void setTotalLosses(int totalLosses) {
        this.totalLosses = totalLosses;
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