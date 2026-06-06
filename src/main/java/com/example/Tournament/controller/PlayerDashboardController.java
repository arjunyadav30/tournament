package com.example.Tournament.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.Tournament.entity.Player;
import com.example.Tournament.entity.Scorecard;
import com.example.Tournament.entity.Match;
import com.example.Tournament.repository.PlayerRepository;
import com.example.Tournament.repository.ScorecardRepository;
import com.example.Tournament.repository.MatchRepository;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class PlayerDashboardController {

    @Autowired private PlayerRepository playerRepository;
    @Autowired private ScorecardRepository scorecardRepository;
    @Autowired private MatchRepository matchRepository;

    @GetMapping("/player/{id}")
    public String playerDashboard(@PathVariable int id, Model model) {
        Player p = playerRepository.findById(id).orElse(null);
        if (p == null) return "redirect:/players";

        List<Scorecard> scs = scorecardRepository.findByPlayer_PlayerId(id);

        // Basic aggregates
        int totalRuns = 0, totalWickets = 0, highest = 0, fifties = 0, hundreds = 0;
        int totalFours = 0, totalSixes = 0, totalOuts = 0;
        int totalRunsGiven = 0; float totalOversBowled = 0f; int bestWickets = 0; int bestRunsGiven = Integer.MAX_VALUE;

        Set<Integer> matchIds = new LinkedHashSet<>();

        for (Scorecard sc : scs) {
            totalRuns += sc.getRuns();
            totalFours += sc.getFours();
            totalSixes += sc.getSixes();
            highest = Math.max(highest, sc.getRuns());
            if (sc.isOut()) totalOuts++;
            if (sc.getRuns() >= 50 && sc.getRuns() < 100) fifties++;
            if (sc.getRuns() >= 100) hundreds++;
            totalWickets += sc.getWickets();
            totalRunsGiven += sc.getRunsGiven();
            totalOversBowled += sc.getOversBowled();
            if (sc.getWickets() > bestWickets || (sc.getWickets() == bestWickets && sc.getRunsGiven() < bestRunsGiven)) {
                bestWickets = sc.getWickets(); bestRunsGiven = sc.getRunsGiven();
            }
            if (sc.getMatch() != null) matchIds.add(sc.getMatch().getMatchId());
        }

        double battingAvg = totalOuts > 0 ? Math.round((totalRuns / (double) totalOuts) * 100.0) / 100.0 : totalRuns;
        double bowlingAvg = totalWickets > 0 ? Math.round((totalRunsGiven / (double) totalWickets) * 100.0) / 100.0 : Double.MAX_VALUE;
        double economy = totalOversBowled > 0 ? Math.round((totalRunsGiven / totalOversBowled) * 100.0) / 100.0 : Double.MAX_VALUE;

        // Load matches map
        Map<Integer, Match> matchMap = matchRepository.findAllById(new ArrayList<>(matchIds)).stream()
                .collect(Collectors.toMap(Match::getMatchId, m -> m));

        // Sort scorecards (newest first by match date then innings)
        scs.sort(Comparator.comparing((Scorecard s) -> s.getMatch() != null && s.getMatch().getMatchDate() != null ? s.getMatch().getMatchDate() : java.time.LocalDate.MIN).reversed()
                .thenComparingInt(s -> s.getInnings()));

        model.addAttribute("player", p);
        model.addAttribute("scorecards", scs);
        model.addAttribute("matches", matchMap);

        model.addAttribute("totalRuns", totalRuns);
        model.addAttribute("totalWickets", totalWickets);
        model.addAttribute("highestScore", highest);
        model.addAttribute("fifties", fifties);
        model.addAttribute("hundreds", hundreds);
        model.addAttribute("totalFours", totalFours);
        model.addAttribute("totalSixes", totalSixes);
        model.addAttribute("battingAvg", String.format(Locale.US, "%.2f", battingAvg));
        model.addAttribute("bowlingAvg", totalWickets>0 ? String.format(Locale.US, "%.2f", bowlingAvg) : "-");
        model.addAttribute("economy", totalOversBowled>0 ? String.format(Locale.US, "%.2f", economy) : "-");

        return "public/player-dashboard";
    }
}
