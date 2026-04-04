package com.example.Tournament.controller;

import com.example.Tournament.entity.User;
import com.example.Tournament.enums.Role;
import com.example.Tournament.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 🔹 Show Signup Page
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    // 🔹 Handle Signup
    @PostMapping("/signup")
    public String signup(@RequestParam String name,
                         @RequestParam String email,
                         @RequestParam String password,
                         @RequestParam Role role) {

        // BUG FIX: no duplicate email check existed — caused a DB unique constraint
        // crash (if email column is unique) or silent duplicate accounts otherwise.
        if (userRepository.findByEmail(email).isPresent()) {
            return "redirect:/signup?error=email_exists";
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        userRepository.save(user);

        return "redirect:/login?registered";
    }

    // 🔹 Show Login Page
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 🔹 Handle Login
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session) { // HttpSession add kiya gaya hai

        User user = userRepository.findByEmail(email).orElse(null);

        // Password check
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            
            // ⭐ SABSE ZAROORI LINE: User ko session mein save karein
            // Iske bina SessionInterceptor hamesha redirect karega
            session.setAttribute("user", user);

            if(user.getRole().name().equals("ADMIN")) {
                return "redirect:/admin/dashboard";
            }

            return "redirect:/";
        }

        // Login fail hone par
        return "redirect:/login?error";
    }

    // 🔹 Handle Logout (Safe side ke liye yahan bhi rakh sakte hain)
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout";
    }
}