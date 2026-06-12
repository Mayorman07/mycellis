package com.mycelis.user.mapper;

import com.mycelis.user.constant.Gender;
import com.mycelis.user.entity.Role;
import com.mycelis.user.entity.User;
import com.mycelis.user.model.dto.UserDto;
import com.mycelis.user.model.request.CreateUserRequest;
import com.mycelis.user.model.response.CreateUserResponse;
import com.mycelis.user.model.response.UserProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // ---- INBOUND: CreateUserRequest -> UserDto ----
    // Only request fields exist yet; everything the service fills later is ignored.
    @Mapping(target = "encryptedPassword", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoggedIn", ignore = true)
    UserDto toDto(CreateUserRequest request);

    // ---- UserDto -> User entity ----
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "verificationToken", ignore = true)
    @Mapping(target = "passwordResetToken", ignore = true)
    @Mapping(target = "passwordResetTokenExpiryDate", ignore = true)
    @Mapping(target = "lastPasswordResetDate", ignore = true)
    @Mapping(target = "lastReactivationEmailSentDate", ignore = true)
    User toEntity(UserDto dto);

    // ---- OUTBOUND: User -> responses ----
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    CreateUserResponse toCreateResponse(User user);

    UserProfileResponse toProfileResponse(User user);

    // ---- Custom conversions MapStruct auto-applies by type ----

    // String -> Gender, case-insensitive (request allows "male", enum is MALE)
    default Gender mapGender(String gender) {
        return gender == null ? null : Gender.valueOf(gender.trim().toUpperCase());
    }

    // Set<Role> -> Set<String> (role entities -> role names)
    @Named("rolesToNames")
    default Set<String> rolesToNames(Set<Role> roles) {
        return roles == null ? null
                : roles.stream().map(Role::getName).collect(Collectors.toSet());
    }
}