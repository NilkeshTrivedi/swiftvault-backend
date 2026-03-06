package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Adapter that exposes UserRepository as a Spring Security UserDetailsService.
 * Required by SecurityConfig → DaoAuthenticationProvider.
 *
 * The User entity already implements UserDetails, so we just load by email
 * (email is the "username" in this project).
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
    }
}