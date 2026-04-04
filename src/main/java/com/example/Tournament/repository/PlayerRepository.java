package com.example.Tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.example.Tournament.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Integer> {

    // 🔥 Team ke hisaab se players nikalne ke liye
    List<Player> findByTeam_TeamId(int teamId);

}