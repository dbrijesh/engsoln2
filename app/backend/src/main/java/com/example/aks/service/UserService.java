// Service layer for User management
package com.example.aks.service;

import com.example.aks.dto.CreateUserRequest;
import com.example.aks.entity.User;
import com.example.aks.exception.BusinessException;
import com.example.aks.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional
  public User createUser(CreateUserRequest request) {
    log.debug("Creating user with username: {}", request.getUsername());

    // Business validation: Check if username already exists
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new BusinessException(
          String.format("Username '%s' is already taken", request.getUsername()));
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setCreatedAt(LocalDateTime.now());

    User savedUser = userRepository.save(user);
    log.info("User created successfully with ID: {}", savedUser.getId());

    return savedUser;
  }

  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  public List<User> findAll() {
    return userRepository.findAll();
  }

  public boolean existsById(Long id) {
    return userRepository.existsById(id);
  }

  @Transactional
  public void deleteById(Long id) {
    log.info("Deleting user with ID: {}", id);
    userRepository.deleteById(id);
  }
}
