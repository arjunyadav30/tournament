package com.example.Tournament.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.Tournament.entity.Player;
import com.example.Tournament.repository.PlayerRepository;

import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Controller
public class PlayerPublicController {

    private static final Logger LOG = Logger.getLogger(PlayerPublicController.class.getName());

    @Autowired private PlayerRepository playerRepository;

    // Temporary in-memory OTP store: mobile -> (code, expiry)
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();

    @GetMapping("/public/register")
    public String showRegister(@RequestParam(required = false) String mobile, Model model) {
        model.addAttribute("mobile", mobile != null ? mobile : "");
        return "public/register-mobile";
    }

    @PostMapping("/public/register")
    public String doRegister(@RequestParam String playerName,
                             @RequestParam String mobile,
                             RedirectAttributes ra,
                             HttpSession session) {

        String m = mobile == null ? "" : mobile.trim();
        if (!m.matches("\\d{10,15}")) {
            ra.addFlashAttribute("error", "Invalid mobile number. Use 10-15 digits.");
            return "redirect:/public/register?mobile=" + mobile;
        }

        Player existing = playerRepository.findByMobileNumber(m);
        if (existing != null) {
            ra.addFlashAttribute("success", "Mobile already registered. Logged in.");
            session.setAttribute("playerId", existing.getPlayerId());
            return "redirect:/players";
        }

        Player p = new Player();
        p.setPlayerName(playerName);
        p.setMobileNumber(m);
        playerRepository.save(p);

        session.setAttribute("playerId", p.getPlayerId());
        ra.addFlashAttribute("success", "Registered and logged in.");
        return "redirect:/players";
    }

    @GetMapping("/public/otp")
    public String otpForm() { return "public/otp-send"; }

    @PostMapping("/public/otp/send")
    public String sendOtp(@RequestParam String mobile, RedirectAttributes ra) {
        String m = mobile == null ? "" : mobile.trim();
        if (!m.matches("\\d{10,15}")) {
            ra.addFlashAttribute("error", "Invalid mobile number.");
            return "redirect:/public/otp";
        }
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        otpStore.put(m, code);
        otpExpiry.put(m, Instant.now().getEpochSecond() + 300); // 5 minutes

        // In production, send SMS. For dev, log it so admin can see the OTP.
        LOG.info("[DEV OTP] send to " + m + " code=" + code);

        ra.addFlashAttribute("info", "OTP sent (development mode). Check server logs.");
        return "redirect:/public/otp/verify?mobile=" + m;
    }

    @GetMapping("/public/otp/verify")
    public String otpVerifyForm(@RequestParam String mobile, Model model) {
        model.addAttribute("mobile", mobile);
        return "public/otp-verify";
    }

    @PostMapping("/public/otp/verify")
    public String verifyOtp(@RequestParam String mobile,
                            @RequestParam String code,
                            RedirectAttributes ra,
                            HttpSession session) {
        String m = mobile == null ? "" : mobile.trim();
        String expected = otpStore.get(m);
        Long exp = otpExpiry.get(m);
        long now = Instant.now().getEpochSecond();
        if (expected == null || exp == null || now > exp) {
            ra.addFlashAttribute("error", "OTP expired or not found. Request a new one.");
            return "redirect:/public/otp";
        }
        if (!expected.equals(code.trim())) {
            ra.addFlashAttribute("error", "Wrong OTP. Try again.");
            return "redirect:/public/otp/verify?mobile=" + m;
        }

        // OTP valid
        otpStore.remove(m); otpExpiry.remove(m);

        Player p = playerRepository.findByMobileNumber(m);
        if (p == null) {
            p = new Player();
            p.setPlayerName("Player " + m);
            p.setMobileNumber(m);
            playerRepository.save(p);
        }

        session.setAttribute("playerId", p.getPlayerId());
        ra.addFlashAttribute("success", "Logged in successfully.");
        return "redirect:/players";
    }
}
