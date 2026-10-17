package com.example.supplychain.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.supplychain.entity.Role;
import com.example.supplychain.entity.UserAccount;
import com.example.supplychain.repository.UserAccountRepository;
import com.example.supplychain.service.AuditLogService;
import com.example.supplychain.service.LoginAttemptService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountRepository userAccountRepository;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(
            UserAccountRepository userAccountRepository,
            AuthenticationManager authenticationManager,
            AuditLogService auditLogService,
            LoginAttemptService loginAttemptService,
            PasswordEncoder passwordEncoder) {

        this.userAccountRepository = userAccountRepository;
        this.authenticationManager = authenticationManager;
        this.auditLogService = auditLogService;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserAccount request) {

        if (request == null
                || request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Username v├á password kh├┤ng ─æ╞░ß╗úc ─æß╗â trß╗æng");
        }

        String username = request.getUsername().trim();

        if (userAccountRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username ─æ├ú tß╗ôn tß║íi");
        }

        UserAccount newAccount = new UserAccount();
        newAccount.setUsername(username);
        newAccount.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // ─É─âng k├╜ c├┤ng khai kh├┤ng ─æ╞░ß╗úc tß╗▒ cß║Ñp quyß╗ün ADMIN.
        newAccount.setRole(Role.WAREHOUSE_STAFF);

        UserAccount saved = userAccountRepository.save(newAccount);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toSafeResponse(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody UserAccount requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (requestBody == null
                || requestBody.getUsername() == null
                || requestBody.getUsername().isBlank()
                || requestBody.getPassword() == null) {

            return ResponseEntity.badRequest()
                    .body("Thiß║┐u username hoß║╖c password");
        }

        String username = requestBody.getUsername().trim();
        String attemptKey = username.toLowerCase(java.util.Locale.ROOT);

        if (loginAttemptService.isBlocked(attemptKey)) {

            auditLogService.record(
                    username,
                    "LOGIN_BLOCKED",
                    "UserAccount:" + username
            );

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("T├ái khoß║ún tß║ím thß╗¥i bß╗ï giß╗¢i hß║ín ─æ─âng nhß║¡p");
        }

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    requestBody.getPassword()
                            )
                    );

            UserAccount account = userAccountRepository
                    .findByUsername(username)
                    .orElseThrow(() ->
                            new org.springframework.security.authentication
                                    .BadCredentialsException(
                                            "Sai username hoß║╖c password"
                                    )
                    );

            loginAttemptService.recordSuccess(attemptKey);

            // Chß╗æng session fixation.
            HttpSession existingSession = request.getSession(false);

            if (existingSession != null) {
                request.changeSessionId();
            } else {
                request.getSession(true);
            }

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context,
                    request,
                    response
            );

            auditLogService.record(
                    username,
                    "LOGIN_SUCCESS",
                    "UserAccount:" + account.getId()
            );

            return ResponseEntity.ok(toSafeResponse(account));

        } catch (AuthenticationException ex) {

            loginAttemptService.recordFailure(attemptKey);

            SecurityContextHolder.clearContext();

            auditLogService.record(
                    username,
                    "LOGIN_FAILURE",
                    "UserAccount:" + username
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Sai username hoß║╖c password");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok("─É─âng xuß║Ñt th├ánh c├┤ng");
    }

    private Map<String, Object> toSafeResponse(UserAccount account) {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", account.getId());
        result.put("username", account.getUsername());
        result.put("password", null);
        result.put("role", account.getRole());

        return result;
    }
}
