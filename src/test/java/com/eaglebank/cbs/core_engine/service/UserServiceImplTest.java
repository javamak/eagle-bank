package com.eaglebank.cbs.core_engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eaglebank.cbs.core_engine.entity.Address;
import com.eaglebank.cbs.core_engine.entity.User;
import com.eaglebank.cbs.core_engine.model.CreateUserRequest;
import com.eaglebank.cbs.core_engine.model.CreateUserRequestAddress;
import com.eaglebank.cbs.core_engine.model.UpdateUserRequest;
import com.eaglebank.cbs.core_engine.model.UserResponse;
import com.eaglebank.cbs.core_engine.repository.UserRepository;
import com.eaglebank.cbs.core_engine.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserServiceImpl userService;

  @Test
  void createUser_success_returns201AndMappedResponse() {
    CreateUserRequest request = buildCreateUserRequest();

    when(userRepository.existsByUsernameAndDeletedFalse("testuser")).thenReturn(false);
    when(userRepository.existsByEmailAndDeletedFalse("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User saved = invocation.getArgument(0);
              Instant now = Instant.now();
              saved.setCreatedTimestamp(now);
              saved.setUpdatedTimestamp(now);
              return saved;
            });

    var response = userService.createUser(request);

    assertEquals(201, response.getStatusCode().value());
    UserResponse body = response.getBody();
    assertNotNull(body);
    assertTrue(body.getId().startsWith("usr-"));
    assertEquals("Test User", body.getName());
    assertEquals("testuser", body.getUsername());
    assertEquals("test@example.com", body.getEmail());
    assertNotNull(body.getCreatedTimestamp());
    assertNotNull(body.getUpdatedTimestamp());

    verify(passwordEncoder).encode("password123");
  }

  @Test
  void createUser_whenUsernameExists_returns409() {
    CreateUserRequest request = buildCreateUserRequest();
    when(userRepository.existsByUsernameAndDeletedFalse("testuser")).thenReturn(true);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> userService.createUser(request));

    assertEquals(409, ex.getStatusCode().value());
    assertEquals("Username already exists", ex.getReason());
  }

  @Test
  void fetchUserByID_whenFound_returns200() {
    User user = buildEntityUser();
    when(userRepository.findByIdAndDeletedFalse("usr-1")).thenReturn(Optional.of(user));

    var response = userService.fetchUserByID("usr-1");

    assertEquals(200, response.getStatusCode().value());
    assertEquals("usr-1", response.getBody().getId());
    assertEquals("testuser", response.getBody().getUsername());
  }

  @Test
  void updateUserByID_whenFound_updatesMutableFields() {
    User user = buildEntityUser();
    UpdateUserRequest update = new UpdateUserRequest();
    update.setName("Updated Name");
    update.setPassword("new-password");

    when(userRepository.findByIdAndDeletedFalse("usr-1")).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = userService.updateUserByID("usr-1", update);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("Updated Name", response.getBody().getName());
    assertEquals("encoded-new-password", user.getPassword());
  }

  @Test
  void deleteUserByID_whenMissing_returns404() {
    when(userRepository.findByIdAndDeletedFalse("usr-missing")).thenReturn(Optional.empty());

    var response = userService.deleteUserByID("usr-missing");

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void deleteUserByID_whenFound_softDeletesUser() {
    User user = buildEntityUser();
    when(userRepository.findByIdAndDeletedFalse("usr-1")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = userService.deleteUserByID("usr-1");

    assertEquals(204, response.getStatusCode().value());
    assertTrue(user.isDeleted());
    verify(userRepository).save(user);
  }

  private CreateUserRequest buildCreateUserRequest() {
    CreateUserRequestAddress address =
        new CreateUserRequestAddress("line1", "town", "county", "postcode");

    return new CreateUserRequest(
        "Test User",
        "testuser",
        "password123",
        address,
        "+447700900000",
        "test@example.com");
  }

  private User buildEntityUser() {
    Address address = new Address("line1", null, null, "town", "county", "postcode");
    User user = new User();
    user.setId("usr-1");
    user.setName("Test User");
    user.setUsername("testuser");
    user.setPassword("encoded-password");
    user.setAddress(address);
    user.setPhoneNumber("+447700900000");
    user.setEmail("test@example.com");
    user.setCreatedTimestamp(Instant.now().minusSeconds(86400));
    user.setUpdatedTimestamp(Instant.now().minusSeconds(3600));
    return user;
  }
}
