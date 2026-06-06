package com.example.Tournament.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.Tournament.entity.Player;
import com.example.Tournament.repository.PlayerRepository;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@CrossOrigin
public class PlayerApiController {

    @Autowired
    private PlayerRepository playerRepository;

    @GetMapping("/team/{teamId}")
    public List<Player> getPlayersByTeam(@PathVariable int teamId) {
        return playerRepository.findByTeam_TeamId(teamId);
    }

    @GetMapping("/by-mobile")
    public Object findByMobile(@RequestParam String mobile) {
        String m = mobile == null ? "" : mobile.trim();
        boolean valid = m.matches("\\d{10,15}");
        Player p = null;
        if (valid) p = playerRepository.findByMobileNumber(m);
        return java.util.Map.of(
                "valid", valid,
                "found", p != null,
                "playerId", p != null ? p.getPlayerId() : null,
                "playerName", p != null ? p.getPlayerName() : null
        );
    }
}