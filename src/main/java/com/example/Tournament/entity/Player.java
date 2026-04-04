package com.example.Tournament.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int playerId;

    @Column(nullable = false)
    private String playerName;

    // 🔥 JsonIgnoreProperties — circular JSON problem fix
    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonIgnoreProperties({"players", "hibernateLazyInitializer"})
    private Team team;

    private String role;
    private int matchesPlayed;
    private int totalRuns;
    private int ballsPlayed;
    private float strikeRate;
    private int totalWickets;
    private float economy;
    private int fifties;
    private int hundreds;

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }

    public int getTotalRuns() { return totalRuns; }
    public void setTotalRuns(int totalRuns) { this.totalRuns = totalRuns; }

    public int getBallsPlayed() { return ballsPlayed; }
    public void setBallsPlayed(int ballsPlayed) { this.ballsPlayed = ballsPlayed; }

    public float getStrikeRate() { return strikeRate; }
    public void setStrikeRate(float strikeRate) { this.strikeRate = strikeRate; }

    public int getTotalWickets() { return totalWickets; }
    public void setTotalWickets(int totalWickets) { this.totalWickets = totalWickets; }

    public float getEconomy() { return economy; }
    public void setEconomy(float economy) { this.economy = economy; }

    public int getFifties() { return fifties; }
    public void setFifties(int fifties) { this.fifties = fifties; }

    public int getHundreds() { return hundreds; }
    public void setHundreds(int hundreds) { this.hundreds = hundreds; }
}