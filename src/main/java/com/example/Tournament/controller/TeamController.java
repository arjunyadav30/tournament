package com.example.Tournament.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.Tournament.entity.Team;
import com.example.Tournament.entity.Tournament;
import com.example.Tournament.entity.Player;
import com.example.Tournament.repository.*;

import java.util.*;

@Controller
public class TeamController {

    @Autowired private TeamRepository teamRepository;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private BallRepository ballRepository;
    @Autowired private InningsRepository inningsRepository;
    @Autowired private ScorecardRepository scorecardRepository;
    @Autowired private TournamentRepository tournamentRepository;

    // ════════════════════════════════════════
    // ✅ PUBLIC ROUTE (ONLY DETAILS PAGE)
    // ════════════════════════════════════════

    @GetMapping("/teams/{teamId}")
    public String publicTeamDetails(@PathVariable int teamId, Model model) {

        Optional<Team> optionalTeam = teamRepository.findById(teamId);

        if (optionalTeam.isEmpty()) {
            return "redirect:/teams"; // safe redirect
        }

        Team team = optionalTeam.get();
        var players = playerRepository.findByTeam_TeamId(teamId);

        model.addAttribute("team", team);
        model.addAttribute("players", players);

        return "public/team-details"; // ✅ :contentReference[oaicite:0]{index=0}
    }

    // ════════════════════════════════════════
    // 🔥 ADMIN ROUTES
    // ════════════════════════════════════════

    @GetMapping("/admin/teams")
    public String teamsHome(Model model) {
        model.addAttribute("teams", teamRepository.findAll());
        return "admin/teams";
    }

    @GetMapping("/admin/teams/add")
    public String showForm(@RequestParam(required = false) Integer tournamentId, Model model) {
        model.addAttribute("tournaments", tournamentRepository.findAll());

        if (tournamentId != null) {
            model.addAttribute("selectedTournamentId", tournamentId);
        }

        return "admin/add-team";
    }

    @GetMapping("/admin/teams/scan")
    public String showTeamScanPage(@RequestParam(required = false) Integer tournamentId, Model model) {
        model.addAttribute("tournaments", tournamentRepository.findAll());
        if (tournamentId != null && tournamentId > 0) {
            model.addAttribute("selectedTournamentId", tournamentId);
        }
        return "admin/scan-team";
    }

    @GetMapping("/admin/teams/scan/register")
    public String showScanRegistrationForm(@RequestParam(required = false) Integer tournamentId,
                                           @RequestParam(required = false) Integer teamId,
                                           Model model,
                                           RedirectAttributes ra) {
        Tournament tournament = null;
        Team team = null;

        if (teamId != null && teamId > 0) {
            team = teamRepository.findById(teamId).orElse(null);
            if (team == null) {
                ra.addFlashAttribute("error", "Invalid Team ID scanned. Please try again.");
                return "redirect:/admin/teams/scan";
            }
            tournament = team.getTournament();
        }

        if (tournament == null && tournamentId != null && tournamentId > 0) {
            tournament = tournamentRepository.findById(tournamentId).orElse(null);
        }

        if (tournament == null) {
            ra.addFlashAttribute("error", "Please select a tournament first.");
            return "redirect:/admin/teams/scan";
        }

        model.addAttribute("tournament", tournament);
        model.addAttribute("team", team != null ? team : new Team());
        model.addAttribute("players", team != null ? playerRepository.findByTeam_TeamId(team.getTeamId()) : Collections.emptyList());
        return "admin/scan-team-register";
    }

    @PostMapping("/admin/teams/scan/register")
    public String saveScannedTeamRegistration(@RequestParam(required = false) Integer teamId,
                                              @RequestParam(required = false) Integer tournamentId,
                                              @RequestParam String teamName,
                                              @RequestParam(required = false) String village,
                                              @RequestParam(required = false) String captainName,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String[] playerNames,
                                              @RequestParam(required = false) String[] playerRoles,
                                              RedirectAttributes ra) {

        Team team;
        if (teamId != null && teamId > 0) {
            team = teamRepository.findById(teamId).orElse(new Team());
        } else {
            team = new Team();
        }

        team.setTeamName(teamName);
        team.setVillage(village);
        team.setCaptainName(captainName);
        team.setStatus(status != null ? status : "Active");

        if (tournamentId != null && tournamentId > 0) {
            tournamentRepository.findById(tournamentId).ifPresent(team::setTournament);
        }

        teamRepository.save(team);

        if (teamId != null && teamId > 0) {
            var existingPlayers = playerRepository.findByTeam_TeamId(team.getTeamId());
            playerRepository.deleteAll(existingPlayers);
        }

        if (playerNames != null && playerRoles != null) {
            int count = Math.min(playerNames.length, playerRoles.length);
            for (int i = 0; i < count; i++) {
                String name = playerNames[i].trim();
                String role = playerRoles[i].trim();
                if (name.isEmpty() && role.isEmpty()) {
                    continue;
                }
                Player player = new Player();
                player.setPlayerName(name);
                player.setRole(role);
                player.setTeam(team);
                player.setMatchesPlayed(0);
                player.setTotalRuns(0);
                player.setBallsPlayed(0);
                player.setStrikeRate(0);
                player.setTotalWickets(0);
                player.setEconomy(0);
                player.setFifties(0);
                player.setHundreds(0);
                playerRepository.save(player);
            }
        }

        ra.addFlashAttribute("success", "Team and players registered successfully!");
        return "redirect:/admin/teams/scan";
    }

    @PostMapping("/admin/teams/add")
    public String addTeam(@RequestParam String teamName,
                          @RequestParam(required = false) String captainName,
                          @RequestParam(required = false) String village,
                          @RequestParam String status,
                          @RequestParam(required = false) Integer tournamentId,
                          RedirectAttributes ra) {

        Team team = new Team();
        team.setTeamName(teamName);
        team.setCaptainName(captainName);
        team.setVillage(village);
        team.setStatus(status);
        team.setTotalMatches(0);
        team.setTotalWins(0);
        team.setTotalLosses(0);

        if (tournamentId != null && tournamentId > 0) {
            team.setTournament(tournamentRepository.findById(tournamentId).orElse(null));
        }

        teamRepository.save(team);

        ra.addFlashAttribute("success", "Team add ho gayi!");
        return "redirect:/admin/teams";
    }

    @GetMapping("/admin/teams/list")
    public String listTeams(Model model) {
        model.addAttribute("teams", teamRepository.findAll());
        return "admin/team-list";
    }

    @PostMapping("/admin/teams/delete/{id}")
    public String deleteTeam(@PathVariable int id, RedirectAttributes ra) {

        try {
            Team team = teamRepository.findById(id).orElse(null);

            if (team == null) {
                ra.addFlashAttribute("error", "Team nahi mili!");
                return "redirect:/admin/teams";
            }

            // 🔥 unlink players
            var players = playerRepository.findByTeam_TeamId(id);
            players.forEach(p -> {
                p.setTeam(null);
                playerRepository.save(p);
            });

            // 🔥 delete matches
            var allMatches = matchRepository.findAll();

            for (var m : allMatches) {
                boolean involved =
                        (m.getTeam1() != null && m.getTeam1().getTeamId() == id) ||
                        (m.getTeam2() != null && m.getTeam2().getTeamId() == id);

                if (involved) {
                    int mid = m.getMatchId();

                    ballRepository.deleteAll(ballRepository.findByMatchMatchId(mid));
                    scorecardRepository.deleteAll(scorecardRepository.findByMatch_MatchId(mid));
                    inningsRepository.deleteAll(inningsRepository.findByMatchMatchId(mid));

                    matchRepository.deleteById(mid);
                }
            }

            teamRepository.deleteById(id);

            ra.addFlashAttribute("success", "Team delete ho gayi!");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }

        return "redirect:/admin/teams";
    }
}