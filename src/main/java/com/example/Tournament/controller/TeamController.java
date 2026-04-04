package com.example.Tournament.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.Tournament.entity.Team;
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