package com.gtd.backend.auth.service;

import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.dto.RegisterResponse;
import com.gtd.backend.auth.exception.EmailAlreadyExistsException;
import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);

        return RegisterResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
