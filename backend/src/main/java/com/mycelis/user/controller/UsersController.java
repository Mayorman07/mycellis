package com.mycelis.user.controller;

import com.mycelis.user.model.request.CreateUserRequest;
import com.mycelis.user.model.request.UpdateUserRequest;
import com.mycelis.user.model.response.CreateUserResponse;
import com.mycelis.user.model.response.UserProfileResponse;
import com.mycelis.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;

    @PostMapping(path = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Create user request for {}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping(path = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateUser(@PathVariable("id") UUID id,
                                                          @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_READ') or @userSecurity.canViewProfile(#id, authentication)")
    public ResponseEntity<UserProfileResponse> viewProfile(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.viewProfile(id));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_READ')")
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 15) Pageable pageable) {
        return ResponseEntity.ok(userService.findAllUsers(pageable, keyword));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PROFILE_DEACTIVATE')")
    public ResponseEntity<Void> deactivateUser(@PathVariable("id") UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PROFILE_DELETE')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}