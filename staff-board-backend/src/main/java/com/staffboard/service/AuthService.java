package com.staffboard.service;

import com.staffboard.dto.response.LoginResponse;
import com.staffboard.entity.User;
import com.staffboard.exception.BusinessException;
import com.staffboard.exception.ErrorCode;
import com.staffboard.repository.UserRepository;
import com.staffboard.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        return LoginResponse.builder()
            .token(token)
            .username(user.getUsername())
            .role(user.getRole())
            .build();
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
