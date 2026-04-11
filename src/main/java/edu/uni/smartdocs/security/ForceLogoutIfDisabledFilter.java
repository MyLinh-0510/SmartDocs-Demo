package edu.uni.smartdocs.security;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ForceLogoutIfDisabledFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails cud) {

            User dbUser = userRepository
                    .findById(cud.getUser().getId())
                    .orElse(null);

            if (dbUser == null || !dbUser.isEnabled()) {

                SecurityContextHolder.clearContext();

                if (request.getSession(false) != null) {
                    request.getSession().invalidate();
                }

                response.sendRedirect("/admin/account/login?locked");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
