package com.example.supplychain.controller;

import com.example.supplychain.entity.Role;
import com.example.supplychain.entity.UserAccount;
import com.example.supplychain.repository.UserAccountRepository;
import com.example.supplychain.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountRepository userAccountRepository;

    private final AuthenticationManager authenticationManager;

    private final AuditLogService auditLogService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(
            UserAccountRepository userAccountRepository,
            AuthenticationManager authenticationManager,
            AuditLogService auditLogService) {

        this.userAccountRepository = userAccountRepository;
        this.authenticationManager = authenticationManager;
        this.auditLogService = auditLogService;
    }

    // =========================
    // ─É─éNG K├¥
    // =========================

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserAccount user) {

        if (userAccountRepository
                .findByUsername(user.getUsername())
                .isPresent()) {

            return ResponseEntity.badRequest()
                    .body("Username ─æ├ú tß╗ôn tß║íi");
        }

        user.setRole(Role.WAREHOUSE_STAFF);

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        UserAccount savedUser =
                userAccountRepository.save(user);

        savedUser.setPassword(null);

        return ResponseEntity.ok(savedUser);
    }

    // =========================
    // ─É─éNG NHß║¼P
    // =========================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody UserAccount user,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    user.getUsername(),
                                    user.getPassword()
                            )
                    );

            // ─Éß║╖t authentication v├áo SecurityContext
            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            // L╞░u SecurityContext v├áo session
            securityContextRepository.saveContext(
                    context,
                    request,
                    response
            );

            UserAccount account =
                    userAccountRepository
                            .findByUsername(user.getUsername())
                            .orElseThrow();

            auditLogService.record(
                    authentication.getName(),
                    "LOGIN_SUCCESS",
                    "UserAccount:" + account.getId());

            account.setPassword(null);

            return ResponseEntity.ok(account);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body("Sai username hoß║╖c password");
        }
    }

    // =========================
    // ─É─éNG XUß║ñT
    // =========================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request) {

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(
                "─É─âng xuß║Ñt th├ánh c├┤ng"
        );
    }
}
