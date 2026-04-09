package com.eaglebank.cbs.core_engine.service;

import com.eaglebank.cbs.core_engine.api.AuthApiDelegate;
import com.eaglebank.cbs.core_engine.entity.User;
import com.eaglebank.cbs.core_engine.model.LoginRequest;
import com.eaglebank.cbs.core_engine.model.LoginResponse;
import com.eaglebank.cbs.core_engine.repository.UserRepository;
import com.eaglebank.cbs.core_engine.security.JwtService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthApiDelegate {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
    Optional<User> user = userRepository.findByUsername(loginRequest.getUsername());
    if (user.isEmpty()
        || !passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String token = jwtService.generateToken(user.get());
    return ResponseEntity.ok(new LoginResponse(user.get().getId(), "Login successful", token));
  }
}
