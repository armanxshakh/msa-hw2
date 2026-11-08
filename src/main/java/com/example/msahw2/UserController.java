package com.example.msahw2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
public class UserController {

    private static final Logger log =
            LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/user")
    public User createUser(@Valid @RequestBody User user) {

        User savedUser = userRepository.save(user);

        log.atInfo()
                .addKeyValue("operation", "CREATE")
                .addKeyValue("user_id", savedUser.getId())
                .log("User created");

        return savedUser;
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {

        return userRepository.findById(id)
                .map(user -> {
                    log.atInfo()
                            .addKeyValue("operation", "READ")
                            .addKeyValue("user_id", id)
                            .log("User found");

                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    log.atWarn()
                            .addKeyValue("operation", "READ")
                            .addKeyValue("user_id", id)
                            .log("User not found");

                    return ResponseEntity.notFound().build();
                });
    }

    @PutMapping("/user/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User updatedUser) {

        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(updatedUser.getUsername());
                    user.setFirstName(updatedUser.getFirstName());
                    user.setLastName(updatedUser.getLastName());
                    user.setEmail(updatedUser.getEmail());
                    user.setPhone(updatedUser.getPhone());

                    User savedUser = userRepository.save(user);

                    log.atInfo()
                            .addKeyValue("operation", "UPDATE")
                            .addKeyValue("user_id", savedUser.getId())
                            .log("User updated");

                    return ResponseEntity.ok(savedUser);
                })
                .orElseGet(() -> {
                    log.atWarn()
                            .addKeyValue("operation", "UPDATE")
                            .addKeyValue("user_id", id)
                            .log("User not found for update");

                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        if (!userRepository.existsById(id)) {

            log.atWarn()
                    .addKeyValue("operation", "DELETE")
                    .addKeyValue("user_id", id)
                    .log("User not found for delete");

            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(id);

        log.atInfo()
                .addKeyValue("operation", "DELETE")
                .addKeyValue("user_id", id)
                .log("User deleted");

        return ResponseEntity.noContent().build();
    }
}