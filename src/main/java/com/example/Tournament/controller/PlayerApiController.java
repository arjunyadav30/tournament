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
}