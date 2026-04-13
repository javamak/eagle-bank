package com.eaglebank.cbs.core_engine.service;

import com.eaglebank.cbs.core_engine.api.UserApiDelegate;
import com.eaglebank.cbs.core_engine.entity.Address;
import com.eaglebank.cbs.core_engine.entity.User;
import com.eaglebank.cbs.core_engine.model.*;
import com.eaglebank.cbs.core_engine.repository.UserRepository;
import com.eaglebank.cbs.core_engine.security.JwtService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserApiDelegate {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  @Override
  public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
    if (userRepository.existsByUsernameAndDeletedFalse(createUserRequest.getUsername())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
    }
    if (userRepository.existsByEmailAndDeletedFalse(createUserRequest.getEmail())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    String id = "usr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    User user = new User();
    user.setId(id);
    user.setName(createUserRequest.getName());
    user.setUsername(createUserRequest.getUsername());
    user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
    user.setAddress(toAddressEntity(createUserRequest.getAddress()));
    user.setPhoneNumber(createUserRequest.getPhoneNumber());
    user.setEmail(createUserRequest.getEmail());

    User saved = userRepository.save(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(saved));
  }

  @Override
  @PreAuthorize("#userId == authentication.principal")
  public ResponseEntity<Void> deleteUserByID(String userId) {
    Optional<User> existing = userRepository.findByIdAndDeletedFalse(userId);
    if (existing.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    User user = existing.get();
    user.setDeleted(true);
    userRepository.save(user);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("#userId == authentication.principal")
  public ResponseEntity<UserResponse> fetchUserByID(String userId) {
    return userRepository
        .findByIdAndDeletedFalse(userId)
        .map(user -> ResponseEntity.ok(toUserResponse(user)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("#userId == authentication.principal")
  public ResponseEntity<UserResponse> updateUserByID(
      String userId, UpdateUserRequest updateUserRequest) {
    Optional<User> existing = userRepository.findByIdAndDeletedFalse(userId);
    if (existing.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    User user = existing.get();

    if (updateUserRequest.getName() != null) {
      user.setName(updateUserRequest.getName());
    }
    if (updateUserRequest.getUsername() != null) {
      user.setUsername(updateUserRequest.getUsername());
    }
    if (updateUserRequest.getPassword() != null) {
      user.setPassword(passwordEncoder.encode(updateUserRequest.getPassword()));
    }
    if (updateUserRequest.getAddress() != null) {
      user.setAddress(toAddressEntity(updateUserRequest.getAddress()));
    }
    if (updateUserRequest.getPhoneNumber() != null) {
      user.setPhoneNumber(updateUserRequest.getPhoneNumber());
    }
    if (updateUserRequest.getEmail() != null) {
      user.setEmail(updateUserRequest.getEmail());
    }

    User saved = userRepository.save(user);
    return ResponseEntity.ok(toUserResponse(saved));
  }

  

  private Address toAddressEntity(CreateUserRequestAddress address) {
    Address addressEntity = new Address();
    addressEntity.setLine1(address.getLine1());
    addressEntity.setLine2(address.getLine2());
    addressEntity.setLine3(address.getLine3());
    addressEntity.setTown(address.getTown());
    addressEntity.setCounty(address.getCounty());
    addressEntity.setPostcode(address.getPostcode());
    return addressEntity;
  }

  private UserResponse toUserResponse(User user) {
    CreateUserRequestAddress address =
        new CreateUserRequestAddress(
            user.getAddress().getLine1(),
            user.getAddress().getTown(),
            user.getAddress().getCounty(),
            user.getAddress().getPostcode());
    address.setLine2(user.getAddress().getLine2());
    address.setLine3(user.getAddress().getLine3());

    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getUsername(),
        address,
        user.getPhoneNumber(),
        user.getEmail(),
        toOffsetDateTime(user.getCreatedTimestamp()),
        toOffsetDateTime(user.getUpdatedTimestamp()));
  }

  private OffsetDateTime toOffsetDateTime(Instant timestamp) {
    return timestamp == null ? null : timestamp.atOffset(ZoneOffset.UTC);
  }
}
