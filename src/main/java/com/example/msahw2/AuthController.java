package com.example.msahw2;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            KafkaTemplate<String, UserCreatedEvent> kafkaTemplate) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already exists"));
        }

        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already exists"));
        }

        User user = new User();

        user.setUsername(request.username());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());

        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        User savedUser = userRepository.save(user);

        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        );

        kafkaTemplate.send(
                "user-created",
                savedUser.getId().toString(),
                event
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new RegisterResponse(
                        savedUser.getId(),
                        savedUser.getUsername(),
                        savedUser.getEmail()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        return userRepository.findByUsername(request.username())
                .filter(user ->
                        passwordEncoder.matches(
                                request.password(),
                                user.getPassword()
                        )
                )
                .<ResponseEntity<?>>map(user -> {

                    String token = jwtService.generateToken(user);

                    return ResponseEntity.ok(
                            new LoginResponse(token)
                    );
                })
                .orElseGet(() ->
                        ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                        "error",
                                        "Invalid username or password"
                                ))
                );
    }
}