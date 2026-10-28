package com.visitor.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
public UserDetailsService userDetailsService(
        PasswordEncoder passwordEncoder) {

    UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("Admin@123"))
            .roles("ADMIN")
            .build();

    UserDetails receptionist = User.builder()
            .username("reception")
            .password(passwordEncoder.encode("Reception@123"))
            .roles("RECEPTIONIST")
            .build();

    return new InMemoryUserDetailsManager(admin, receptionist);
}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    http
        .authorizeHttpRequests(auth -> auth

            // Login page sabke liye accessible hai
            .requestMatchers("/login", "/css/**", "/js/**", "/images/**")
            .permitAll()

            // Sirf Admin access kar sakta hai
            .requestMatchers("/history", "/history/filter", "/export/excel")
            .hasRole("ADMIN")

            // Baaki authenticated users access kar sakte hain
            .anyRequest().authenticated()
        )

        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/visitors", true)
            .permitAll()
        )

        .logout(logout -> logout
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        );

    return http.build();
}
}