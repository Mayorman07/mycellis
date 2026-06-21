package com.mycelis.user.service;

import com.mycelis.user.model.request.CreateUserRequest;
import com.mycelis.user.model.request.UpdateUserRequest;
import com.mycelis.user.model.response.CreateUserResponse;
import com.mycelis.user.model.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    CreateUserResponse createUser(CreateUserRequest request);

    UserProfileResponse updateUser(UUID id, UpdateUserRequest request);

    UserProfileResponse viewProfile(UUID id);

    Page<UserProfileResponse> findAllUsers(Pageable pageable, String keyword);

    void deactivateUser(UUID id);

    void deleteUser(UUID id);

    void updateLastLoggedIn(UUID id);
}