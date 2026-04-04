package com.example.Tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.Tournament.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Integer> {
}