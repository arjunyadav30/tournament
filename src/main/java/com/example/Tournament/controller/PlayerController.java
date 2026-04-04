package com.example.Tournament.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.Tournament.entity.Player;
import com.example.Tournament.entity.Team;
import com.example.Tournament.repository.PlayerRepository;
import com.example.Tournament.repository.ScorecardRepository;
import com.example.Tournament.repository.TeamRepository;

@Controller
@RequestMapping("/admin/players")
public class PlayerController {

    @Autowired private PlayerRepository playerRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private ScorecardRepository scorecardRepository;

    // FIX: model mein players dalo — warna list empty aati thi
    @GetMapping
    public String playersPage(Model model) {
        model.addAttribute("players", playerRepository.findAll());
        model.addAttribute("teams", teamRepository.findAll());
        return "admin/players";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("teams", teamRepository.findAll());
        return "admin/add-player";
    }

    @PostMapping("/add")
    public String savePlayer(@RequestParam String playerName,
                             @RequestParam String role,
                             @RequestParam int teamId,
                             RedirectAttributes ra) {

        Player p = new Player();
        p.setPlayerName(playerName);
        p.setRole(role);
        Team team = teamRepository.findById(teamId).orElse(null);
        p.setTeam(team);
        p.setMatchesPlayed(0);
        p.setTotalRuns(0);
        p.setBallsPlayed(0);
        p.setStrikeRate(0);
        p.setTotalWickets(0);
        p.setEconomy(0);
        p.setFifties(0);
        p.setHundreds(0);
        playerRepository.save(p);

        ra.addFlashAttribute("success", "Player add ho gaya!");
        return "redirect:/admin/players";
    }

    @GetMapping("/list")
    public String listPlayers(Model model) {
        model.addAttribute("players", playerRepository.findAll());
        return "admin/player-list";
    }

    // ✅ DELETE PLAYER — scorecard entries bhi delete karo
    @PostMapping("/delete/{id}")
    public String deletePlayer(@PathVariable int id, RedirectAttributes ra) {
        try {
            Player p = playerRepository.findById(id).orElse(null);
            if (p == null) {
                ra.addFlashAttribute("error", "Player nahi mila!");
                return "redirect:/admin/players";
            }
            // Delete scorecard entries for this player
            scorecardRepository.deleteByPlayerId(id);

            playerRepository.deleteById(id);
            ra.addFlashAttribute("success", "Player delete ho gaya!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }
        return "redirect:/admin/players";
    }
}
