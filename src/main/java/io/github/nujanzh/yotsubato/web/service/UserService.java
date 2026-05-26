package io.github.nujanzh.yotsubato.web.service;

import io.github.nujanzh.yotsubato.exception.UserAlreadyExistsException;
import io.github.nujanzh.yotsubato.exception.UserNotFoundException;
import io.github.nujanzh.yotsubato.model.user.User;
import io.github.nujanzh.yotsubato.model.user.UserStatus;
import io.github.nujanzh.yotsubato.repository.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String email, String username, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        }

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(
                    "User with username " + username + " already exists");
        }

        String hash = passwordEncoder.encode(password);

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(hash);
        user.setStatus(UserStatus.OFFLINE);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyExistsException("User with that email or username already exists");
        }
    }

    public User getByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
    }

    public User getById(UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    public List<User> getAllByIds(Collection<UUID> ids) {
        return userRepository.findAllById(ids);
    }

    public List<User> getAllByIdsOrThrow(Collection<UUID> ids) {
        List<User> users = userRepository.findAllById(ids);

        if (ids.size() != users.size()) {
            throw new IllegalArgumentException(
                    "One or more member IDs are invalid or do not exist");
        }

        return users;
    }
}
