package com.authplatform.service;

import com.authplatform.dto.AuthResponse;
import com.authplatform.dto.LoginRequest;
import com.authplatform.dto.MeResponse;
import com.authplatform.dto.SignupRequest;
import com.authplatform.model.RefreshToken;
import com.authplatform.model.User;
import com.authplatform.repository.UserRepository;
import com.authplatform.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User(email, passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);

        String accessToken = jwtUtil.generateToken(saved.getId(), saved.getEmail());
        String refreshToken = refreshTokenService.createToken(saved.getId());
        return new AuthResponse(accessToken, refreshToken);
    }

    public MeResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        return new MeResponse(user.getId(), user.getEmail(), user.isVerified(), user.getCreatedAt());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail());
        String refreshToken = refreshTokenService.createToken(user.getId());
        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken old = refreshTokenService.validateAndRotate(rawRefreshToken);

        User user = userRepository.findById(old.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getEmail());
        String newRefreshToken = refreshTokenService.createToken(user.getId());
        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
    }
}
