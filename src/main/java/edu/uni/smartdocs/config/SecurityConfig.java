package edu.uni.smartdocs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurity(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index", "/admin",
                                        "/admin/account/login",
                                        "/admin/account/register",
                                        "/admin/account/forgot-password",
                                        "/admin/account/reset-password",
                                        "/admin/users/**").permitAll()
                        .requestMatchers("/admin/account/change-password").authenticated() // ✅ chỉ cho người đã login
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/admin/account/login") // ✅ dùng form login của bạn
                        .loginProcessingUrl("/admin/account/login") // ✅ Spring sẽ POST vào đây
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/account/login?error=true") // ✅ lỗi → quay lại login
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/account/logout")
                        .logoutSuccessUrl("/admin/account/login?logout=true")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain employeeSecurity(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index", "/employee/account/login").permitAll()
                        .requestMatchers("/employee/**").hasRole("EMPLOYEE")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/employee/account/login")
                        .defaultSuccessUrl("/employee/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")     // có thể tuỳ chỉnh, hoặc bỏ dòng này nếu dùng mặc định
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );
        return http.build();
    }

}
