package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.UserRepository;
import edu.uni.smartdocs.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));

        CustomUserDetails details = new CustomUserDetails(user);
        System.out.println("Created CustomUserDetails from package: " + details.getClass().getName());
        return details;
    }
}


