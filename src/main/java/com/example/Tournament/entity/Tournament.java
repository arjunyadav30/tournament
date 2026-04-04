package com.example.Tournament.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tournament")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int tournamentId;

    @Column(nullable = false)
    private String tournamentName;

    private LocalDate startDate;

    // 🔥 End date optional hai (fix nahi hai)
    @Column(nullable = true)
    private LocalDate endDate;

    private String location;

    private String organizer;

    private String status; // Upcoming / Ongoing / Completed

    // 🔹 Constructors
    public Tournament() {}

    public Tournament(String tournamentName, LocalDate startDate) {
        this.tournamentName = tournamentName;
        this.startDate = startDate;
    }

    // 🔹 Getters & Setters
    public int getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(int tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}