package edu.uni.smartdocs.config;

import edu.uni.smartdocs.security.ForceLogoutIfDisabledFilter;
import edu.uni.smartdocs.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationSuccessHandler successHandler;
    private final CustomUserDetailsService customUserDetailsService;
    private final ForceLogoutIfDisabledFilter forceLogoutIfDisabledFilter;

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        provider.setPreAuthenticationChecks(user -> {
            if (!user.isEnabled()) {
                throw new DisabledException("Account is locked");
            }
        });

        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==================== 1. USER SECURITY - ƯU TIÊN CAO NHẤT ====================
    @Bean
    @Order(1)
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {

        http

                .securityMatcher("/user/**", "/", "/chat/**","/api/semantic-search/**")
                .authenticationProvider(authenticationProvider())

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/chat/**",
                                "/user/document-viewed/**",
                                "/user/document-action/**",
                                "/api/notifications/**",
                                "/user/share/**",
                                "/share/**",
                                "/api/semantic-search/**",
                                "/api/chat/**"
                        )
                )

                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/user/account/login").permitAll()   // Cho phép root và login user
                        .requestMatchers("/chat/**").hasAnyRole("EMPLOYEE", "CEO")
                        .requestMatchers("/user/**").hasAnyRole("EMPLOYEE", "CEO")
                        .requestMatchers("/api/semantic-search/**").permitAll()
                        .requestMatchers("/api/chat/**").permitAll()
                        .anyRequest().authenticated()
                )

                // Login User
                .formLogin(form -> form
                        .loginPage("/user/account/login")
                        .loginProcessingUrl("/user/account/login")
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> {
                            String error = "Sai email hoặc mật khẩu";
                            if (exception instanceof DisabledException) {
                                error = "Tài khoản của bạn đã bị khóa! Vui lòng liên hệ quản trị viên.";
                            }
                            request.getSession().setAttribute("error", error);
                            response.sendRedirect("/user/account/login?error");
                        })
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/user/account/logout")
                        .logoutSuccessUrl("/user/account/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )

                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .sessionRegistry(sessionRegistry())
                )

                .addFilterBefore(forceLogoutIfDisabledFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ==================== 2. ADMIN SECURITY - Giữ nguyên logic cũ ====================
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authenticationProvider(authenticationProvider())

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/user/document-viewed/**",
                                "/user/document-action/**",
                                "/api/notifications/**",
                                "/user/share/**",
                                "/share/**",
                                "/api/semantic-search/**",
                                "/api/chat/**"
                        )
                )

                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/admin/account/login",
                                "/admin/account/register",
                                "/admin/account/forgot-password",
                                "/admin/account/reset-password"
                        ).permitAll()

                        .requestMatchers("/share/**", "/view/pdf/**", "/user/pdf-preview/**", "/uploads/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()

                        .requestMatchers("/api/semantic-search/**").permitAll()
                        .requestMatchers("/api/chat/**").permitAll()

                        .requestMatchers("/api/notifications/**").hasAnyRole("EMPLOYEE", "CEO")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/admin/account/login")
                        .loginProcessingUrl("/admin/account/login")
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> {
                            String error = "Sai email hoặc mật khẩu";
                            if (exception instanceof DisabledException) {
                                error = "Tài khoản của bạn đã bị khóa! Vui lòng liên hệ quản trị viên.";
                            }
                            request.getSession().setAttribute("error", error);
                            response.sendRedirect("/admin/account/login?error");
                        })
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/admin/account/logout")
                        .logoutSuccessUrl("/admin/account/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )

                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .sessionRegistry(sessionRegistry())
                )

                .addFilterBefore(forceLogoutIfDisabledFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}