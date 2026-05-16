package com.magadhexplora.api.config;

import com.magadhexplora.api.user.RoleEntity;
import com.magadhexplora.api.user.RoleRepository;
import com.magadhexplora.api.user.UserEntity;
import com.magadhexplora.api.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@magadhexplora.local}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.name:Magadh Admin}")
    private String adminName;

    public AdminBootstrap(UserRepository userRepository, RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }

        RoleEntity adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing — check Flyway migrations"));

        UserEntity admin = new UserEntity();
        admin.setName(adminName);
        admin.setEmail(adminEmail.toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);
        userRepository.save(admin);

        log.warn("Bootstrapped default admin user: {} (change ADMIN_PASSWORD env var for production)", adminEmail);
    }
}
