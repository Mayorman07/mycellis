package com.mycelis.user.service;

import com.mycelis.user.model.request.CreateUserRequest;
import com.mycelis.user.model.request.UpdateUserRequest;
import com.mycelis.user.model.response.CreateUserResponse;
import com.mycelis.user.model.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    CreateUserResponse createUser(CreateUserRequest request);

    UserProfileResponse updateUser(String userId, UpdateUserRequest request);

    UserProfileResponse viewProfile(String userId);

    Page<UserProfileResponse> findAllUsers(Pageable pageable, String keyword);

    void deactivateUser(String userId);

    void deleteUser(String userId);

    void updateLastLoggedIn(String userId);
}