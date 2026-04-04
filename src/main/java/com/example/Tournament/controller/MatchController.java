package com.example.Tournament.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.Tournament.entity.Match;
import com.example.Tournament.entity.Tournament;
import com.example.Tournament.repository.BallRepository;
import com.example.Tournament.repository.InningsRepository;
import com.example.Tournament.repository.MatchRepository;
import com.example.Tournament.repository.ScorecardRepository;
import com.example.Tournament.repository.TeamRepository;
import com.example.Tournament.repository.TournamentRepository;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/matches")
public class MatchController {

    @Autowired private MatchRepository matchRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private BallRepository ballRepository;
    @Autowired private InningsRepository inningsRepository;
    @Autowired private ScorecardRepository scorecardRepository;
    @Autowired private TournamentRepository tournamentRepository;

    // Matches page + filter
    @GetMapping
    public String matches(@RequestParam(required = false) String status, Model model) {
        if (status == null) {
            model.addAttribute("matches", matchRepository.findAll());
        } else {
            model.addAttribute("matches", matchRepository.findByStatus(status));
        }
        return "admin/matches";
    }

    // Schedule Match Form - with optional tournament
    @GetMapping("/add")
    public String showMatchForm(@RequestParam(required = false) Integer tournamentId, Model model) {
        model.addAttribute("teams", teamRepository.findAll());
        model.addAttribute("tournaments", tournamentRepository.findAll());
        if (tournamentId != null) {
            model.addAttribute("selectedTournamentId", tournamentId);
        }
        return "admin/add-match";
    }

    // Save Match — FIX: tossWinner optional, status always Upcoming
    @PostMapping("/add")
    public String saveMatch(@RequestParam int team1Id,
                            @RequestParam int team2Id,
                            @RequestParam String matchDate,
                            @RequestParam String matchTime,
                            @RequestParam int overs,
                            @RequestParam int maxOversPerBowler,
                            @RequestParam(required = false) String team1Squad,
                            @RequestParam(required = false) String team2Squad,
                            @RequestParam(required = false) String venue,
                            @RequestParam(required = false) Integer tournamentId,
                            RedirectAttributes ra) {

        // Validate same team not selected
        if (team1Id == team2Id) {
            ra.addFlashAttribute("error", "Dono teams alag honi chahiye!");
            return "redirect:/admin/matches/add";
        }

        if (overs <= 0 || maxOversPerBowler <= 0) {
            ra.addFlashAttribute("error", "Overs aur bowler limit positive honi chahiye!");
            return "redirect:/admin/matches/add";
        }
        if (maxOversPerBowler > overs) {
            ra.addFlashAttribute("error", "Bowler max overs, match overs se zyada nahi ho sakta.");
            return "redirect:/admin/matches/add";
        }

        Match match = new Match();
        match.setTeam1(teamRepository.findById(team1Id).orElse(null));
        match.setTeam2(teamRepository.findById(team2Id).orElse(null));
        match.setMatchDate(LocalDate.parse(matchDate));
        match.setMatchTime(LocalTime.parse(matchTime));
        match.setOvers(overs);
        match.setMaxOversPerBowler(maxOversPerBowler);
        match.setTeam1Squad(cleanSquadCsv(team1Squad));
        match.setTeam2Squad(cleanSquadCsv(team2Squad));
        match.setVenue(venue != null ? venue : "");
        
        // Set tournament if provided
        if (tournamentId != null && tournamentId > 0) {
            match.setTournament(tournamentRepository.findById(tournamentId).orElse(null));
        }
        
        match.setStatus("Upcoming");
        // Toss match start pe hogi
        matchRepository.save(match);

        ra.addFlashAttribute("success", "Match schedule ho gaya!");
        return "redirect:/admin/matches";
    }

    private String cleanSquadCsv(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String[] parts = raw.split(",");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            String v = p.trim();
            if (v.matches("\\d+")) {
                if (!sb.isEmpty()) sb.append(",");
                sb.append(v);
            }
        }
        return sb.toString();
    }

    // Start match → go to toss (or skip straight to live scoring if toss already done)
    // BUG FIX: if the match already has toss data (e.g. page refresh after toss),
    // the old code always showed the toss form again, which would re-save the toss
    // and potentially reset the batting team for the live scoring page.
    @GetMapping("/start/{id}")
    public String startMatch(@PathVariable int id, Model model) {
        Match match = matchRepository.findById(id).orElse(null);
        if (match == null) return "redirect:/admin/matches";
        if (match.getTossWinner() != null && match.getTossDecision() != null) {
            return "redirect:/admin/matches/score/live/" + id;
        }
        model.addAttribute("match", match);
        return "admin/toss";
    }

    // Toss page (for live match resumption)
    @GetMapping("/score/toss/{id}")
    public String tossPage(@PathVariable int id, Model model) {
        Match match = matchRepository.findById(id).orElse(null);
        if (match != null && match.getTossWinner() != null && match.getTossDecision() != null) {
            return "redirect:/admin/matches/score/live/" + id;
        }
        model.addAttribute("match", match);
        return "admin/toss";
    }

    // Save toss + go live
    @PostMapping("/toss")
    public String saveToss(@RequestParam int matchId,
                           @RequestParam String tossWinner,
                           @RequestParam String decision) {

        Match match = matchRepository.findById(matchId).orElse(null);
        if (match != null) {
            match.setTossWinner(tossWinner.equals("team1") ? match.getTeam1() : match.getTeam2());
            match.setTossDecision(decision);
            match.setStatus("Live");
            matchRepository.save(match);
        }
        return "redirect:/admin/matches/score/live/" + matchId;
    }

    // Live scoring page
    @GetMapping("/score/live/{id}")
    public String liveScoring(@PathVariable int id, Model model) {
        Match match = matchRepository.findById(id).orElse(null);
        model.addAttribute("match", match);
        return "admin/live-scoring";
    }

    @GetMapping("/score/{id}")
    public String scoringPage(@PathVariable int id, Model model) {
        Match match = matchRepository.findById(id).orElse(null);
        model.addAttribute("match", match);
        return "admin/live-scoring";
    }

    // ✅ DELETE MATCH — cascade delete balls, innings, scorecards
    @PostMapping("/delete/{id}")
    public String deleteMatch(@PathVariable int id, RedirectAttributes ra) {
        try {
            Match match = matchRepository.findById(id).orElse(null);
            if (match == null) {
                ra.addFlashAttribute("error", "Match nahi mila!");
                return "redirect:/admin/matches";
            }
            // Delete dependent records first (FK constraints)
            ballRepository.deleteAll(ballRepository.findByMatchMatchId(id));
            scorecardRepository.deleteAll(scorecardRepository.findByMatch_MatchId(id));
            inningsRepository.deleteAll(inningsRepository.findByMatchMatchId(id));
            matchRepository.deleteById(id);
            ra.addFlashAttribute("success", "Match delete ho gaya!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }
        return "redirect:/admin/matches";
    }
}
