package com.example.msahw2;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(
            Authentication authentication) {

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok(
                        toProfileResponse(user)
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .map(user -> {

                    user.setFirstName(request.firstName());
                    user.setLastName(request.lastName());
                    user.setEmail(request.email());
                    user.setPhone(request.phone());

                    User savedUser = userRepository.save(user);

                    return ResponseEntity.ok(
                            toProfileResponse(savedUser)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ProfileResponse toProfileResponse(User user) {

        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfileById(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .map(currentUser -> {

                    if (!currentUser.getId().equals(id)) {
                        return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .<ProfileResponse>build();
                    }

                    return ResponseEntity.ok(
                            toProfileResponse(currentUser)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> updateProfileById(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .map(currentUser -> {

                    if (!currentUser.getId().equals(id)) {
                        return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .<ProfileResponse>build();
                    }

                    currentUser.setFirstName(request.firstName());
                    currentUser.setLastName(request.lastName());
                    currentUser.setEmail(request.email());
                    currentUser.setPhone(request.phone());

                    User savedUser = userRepository.save(currentUser);

                    return ResponseEntity.ok(
                            toProfileResponse(savedUser)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }
}