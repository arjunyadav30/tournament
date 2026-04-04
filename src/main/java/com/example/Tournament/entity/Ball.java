package com.example.Tournament.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ball")
public class Ball {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    private int overNumber;
    private int ballNumber;
    private int runs;
    private String type;
    private boolean wicket;
    private String howOut;
    private String batsman;
    private String bowler;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }

    public int getOverNumber() { return overNumber; }
    public void setOverNumber(int overNumber) { this.overNumber = overNumber; }

    public int getBallNumber() { return ballNumber; }
    public void setBallNumber(int ballNumber) { this.ballNumber = ballNumber; }

    public int getRuns() { return runs; }
    public void setRuns(int runs) { this.runs = runs; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isWicket() { return wicket; }
    public void setWicket(boolean wicket) { this.wicket = wicket; }

    public String getHowOut() { return howOut; }
    public void setHowOut(String howOut) { this.howOut = howOut; }

    public String getBatsman() { return batsman; }
    public void setBatsman(String batsman) { this.batsman = batsman; }

    public String getBowler() { return bowler; }
    public void setBowler(String bowler) { this.bowler = bowler; }
}