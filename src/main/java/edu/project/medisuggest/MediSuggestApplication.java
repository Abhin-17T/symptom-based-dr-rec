package edu.project.medisuggest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class MediSuggestApplication {
    public static void main(String[] args) { SpringApplication.run(MediSuggestApplication.class, args); }
    @Bean BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean CommandLineRunner seedAdmin(JdbcTemplate db, BCryptPasswordEncoder encoder,
            @Value("${app.admin.email}") String email, @Value("${app.admin.password}") String password) {
        return args -> {
            Integer count = db.queryForObject("SELECT COUNT(*) FROM users WHERE email=?", Integer.class, email);
            if (count != null && count == 0) db.update("INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,'ADMIN')",
                    "System Administrator", email.toLowerCase(), encoder.encode(password));
        };
    }
}
