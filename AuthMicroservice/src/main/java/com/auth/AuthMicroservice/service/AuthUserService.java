package com.auth.AuthMicroservice.service;

import com.auth.AuthMicroservice.config.JwtUtil;
import com.auth.AuthMicroservice.dto.LoginRequest;
import com.auth.AuthMicroservice.dto.LoginResponse;
import com.auth.AuthMicroservice.dto.RegisterRequest;
import com.auth.AuthMicroservice.entity.User;
import com.auth.AuthMicroservice.external.UserClient;
import com.auth.AuthMicroservice.payload.UserDto;
import com.auth.AuthMicroservice.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthUserService {

    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private UserClient userClient;

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        String generatedUserId = UUID.randomUUID().toString();
        User user = User.builder()
                .userId(generatedUserId)
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : "USER")
                .build();

        userRepository.save(user);
        userClient.createUser(UserDto.builder()
                .userId(generatedUserId)
                .userName(request.getUsername())
                .userEmail(request.getEmail()) // ensure it's present in RegisterRequest
                .userAbout("Created via AuthService")
                .userGender("Not specified") // optional
                .build());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getUserId(), user.getRole());
        return new LoginResponse(token, user.getUserId());
    }

}
