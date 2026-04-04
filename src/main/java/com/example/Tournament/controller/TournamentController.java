package com.example.Tournament.controller;

import com.example.Tournament.entity.Tournament;
import com.example.Tournament.entity.User;
import com.example.Tournament.entity.Team;
import com.example.Tournament.entity.Match;
import com.example.Tournament.repository.TournamentRepository;
import com.example.Tournament.repository.TeamRepository;
import com.example.Tournament.repository.PlayerRepository;
import com.example.Tournament.repository.MatchRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/tournaments")
public class TournamentController {

    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private MatchRepository matchRepository;

    // Display all tournaments
    @GetMapping
    public String listTournaments(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Tournament> tournaments = tournamentRepository.findAll();
        model.addAttribute("tournaments", tournaments);
        model.addAttribute("tournamentCount", tournaments.size());
        return "admin/tournaments";
    }

    // Show add tournament form
    @GetMapping("/add")
    public String showAddForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("tournament", new Tournament());
        return "admin/add-tournament";
    }

    // Save tournament
    @PostMapping("/add")
    public String saveTournament(@ModelAttribute Tournament tournament,
                                 @RequestParam(required = false) String startDateStr,
                                 @RequestParam(required = false) String endDateStr,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Parse dates if provided
            if (startDateStr != null && !startDateStr.isEmpty()) {
                tournament.setStartDate(LocalDate.parse(startDateStr));
            }
            if (endDateStr != null && !endDateStr.isEmpty()) {
                tournament.setEndDate(LocalDate.parse(endDateStr));
            }

            // Set default status if not provided
            if (tournament.getStatus() == null || tournament.getStatus().isEmpty()) {
                tournament.setStatus("Upcoming");
            }

            tournamentRepository.save(tournament);
            redirectAttributes.addFlashAttribute("successMessage", "Tournament created successfully!");
            return "redirect:/admin/tournaments";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating tournament: " + e.getMessage());
            return "redirect:/admin/tournaments/add";
        }
    }

    // Show edit form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable int id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Tournament tournament = tournamentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tournament not found"));
        model.addAttribute("tournament", tournament);
        return "admin/edit-tournament";
    }

    // Update tournament
    @PostMapping("/update/{id}")
    public String updateTournament(@PathVariable int id,
                                   @ModelAttribute Tournament tournament,
                                   @RequestParam(required = false) String startDateStr,
                                   @RequestParam(required = false) String endDateStr,
                                   RedirectAttributes redirectAttributes) {
        try {
            Tournament existingTournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

            existingTournament.setTournamentName(tournament.getTournamentName());
            existingTournament.setLocation(tournament.getLocation());
            existingTournament.setOrganizer(tournament.getOrganizer());
            existingTournament.setStatus(tournament.getStatus());

            if (startDateStr != null && !startDateStr.isEmpty()) {
                existingTournament.setStartDate(LocalDate.parse(startDateStr));
            }
            if (endDateStr != null && !endDateStr.isEmpty()) {
                existingTournament.setEndDate(LocalDate.parse(endDateStr));
            }

            tournamentRepository.save(existingTournament);
            redirectAttributes.addFlashAttribute("successMessage", "Tournament updated successfully!");
            return "redirect:/admin/tournaments";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating tournament: " + e.getMessage());
            return "redirect:/admin/tournaments/edit/" + id;
        }
    }

    // Delete tournament
    @GetMapping("/delete/{id}")
    public String deleteTournament(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            tournamentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Tournament deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting tournament: " + e.getMessage());
        }
        return "redirect:/admin/tournaments";
    }

    // View tournament details with teams, players, and matches (SPECIFIC ROUTE FIRST)
    @GetMapping("/view/{id}")
    public String viewTournamentDetails(@PathVariable int id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Tournament tournament = tournamentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tournament not found"));
        
        model.addAttribute("tournament", tournament);

        // Add tournaments list for stats widgets in the detail template
        List<Tournament> tournaments = tournamentRepository.findAll();
        model.addAttribute("tournaments", tournaments);
        
        // Filter teams that belong to this tournament
        List<Team> allTeams = teamRepository.findAll();
        List<Team> tournamentTeams = new ArrayList<>();
        for (Team team : allTeams) {
            if (team.getTournament() != null && team.getTournament().getTournamentId() == id) {
                tournamentTeams.add(team);
            }
        }
        model.addAttribute("teams", tournamentTeams);
        
        // Get all matches for this tournament
        List<Match> allMatches = matchRepository.findAll();
        List<Match> tournamentMatches = new ArrayList<>();
        for (Match m : allMatches) {
            if (m.getTournament() != null && m.getTournament().getTournamentId() == id) {
                tournamentMatches.add(m);
            }
        }
        model.addAttribute("matches", tournamentMatches);
        
        return "admin/tournament-detail";
    }

    // Generic tournament detail route (kept for backward compatibility)
    @GetMapping("/{id}")
    public String viewTournament(@PathVariable int id, Model model, HttpSession session) {
        return viewTournamentDetails(id, model, session);
    }
}
