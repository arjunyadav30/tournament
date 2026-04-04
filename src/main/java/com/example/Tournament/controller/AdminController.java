package com.example.Tournament.controller;

import com.example.Tournament.entity.User;
import com.example.Tournament.repository.MatchRepository;
import com.example.Tournament.repository.PlayerRepository;
import com.example.Tournament.repository.TeamRepository;
import com.example.Tournament.repository.TournamentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private TeamRepository teamRepository;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private TournamentRepository tournamentRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("adminName", user != null ? user.getName() : "Admin");
        model.addAttribute("teamCount", teamRepository.count());
        model.addAttribute("playerCount", playerRepository.count());
        model.addAttribute("totalMatches", matchRepository.count());
        model.addAttribute("tournamentCount", tournamentRepository.count());
        model.addAttribute("liveMatches", matchRepository.findByStatus("Live").size());
        model.addAttribute("completedMatches", matchRepository.findByStatus("Completed").size());
        model.addAttribute("upcomingMatches", matchRepository.findByStatus("Upcoming").size());
        return "admin/dashboard";
    }

    @GetMapping("/profile")
    public String profile() {
        return "admin/profile";
    }
}
