package com.magadhexplora.api.auth;

import com.magadhexplora.api.auth.dto.*;
import com.magadhexplora.api.security.JwtUtil;
import com.magadhexplora.api.user.RoleEntity;
import com.magadhexplora.api.user.RoleRepository;
import com.magadhexplora.api.user.UserEntity;
import com.magadhexplora.api.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        RoleEntity userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("USER role missing — run migrations"));

        UserEntity u = new UserEntity();
        u.setName(req.getName().trim());
        u.setEmail(req.getEmail().toLowerCase().trim());
        u.setMobile(req.getMobile());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(userRole);
        u.setRoles(roles);
        userRepository.save(u);

        return new AuthResponse(jwtUtil.generate(u.getEmail(), List.of("USER")));
    }

    public AuthResponse login(LoginRequest req) {
        UserEntity u = userRepository.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!u.isActive() || !passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        List<String> roles = u.getRoles().stream().map(RoleEntity::getName).toList();
        return new AuthResponse(jwtUtil.generate(u.getEmail(), roles));
    }

    public UserResponse me(String email) {
        UserEntity u = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        String role = u.getRoles().stream()
                .map(RoleEntity::getName)
                .anyMatch("ADMIN"::equals) ? "ADMIN" : "USER";
        return new UserResponse(u.getName(), u.getEmail(), u.getMobile(), role);
    }
}
