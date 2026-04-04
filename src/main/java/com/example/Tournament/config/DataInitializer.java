package com.example.Tournament.config;

import com.example.Tournament.entity.User;
import com.example.Tournament.enums.Role;
import com.example.Tournament.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only create admin if no users exist at all
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@cricketpro.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("✅ Admin created: admin@cricketpro.com / admin123");
        }

        System.out.println("✅ CricketPro started successfully on port 8082");
    }
}
