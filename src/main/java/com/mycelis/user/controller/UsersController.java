package com.mycelis.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.cloud.context.config.annotation.RefreshScope;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final RefreshTokenService refreshTokenService;
    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

    @PostMapping(path ="/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest createUserRequest){
        logger.info("The incoming create user request {} " , createUserRequest);
        UserDto userDto = modelMapper.map(createUserRequest,UserDto.class);
        UserDto createdUserDto = userService.createUser(userDto);
        CreateUserResponse returnValue = modelMapper.map(createdUserDto, CreateUserResponse.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(returnValue);

    }
    @PutMapping(path ="/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateUserResponse> updateUser(@Valid @RequestBody UpdateUserRequest updateUserRequest){
        logger.info("The incoming update user request {} " , updateUserRequest);
        UserDto userDto = modelMapper.map(updateUserRequest, UserDto.class);
        UserDto userToBeUpdated = userService.updateUser(userDto);
        CreateUserResponse returnValue = modelMapper.map(userToBeUpdated, CreateUserResponse.class);
        logger.info("The outgoing update user response {} " , returnValue);
        return ResponseEntity.status(HttpStatus.CREATED).body(returnValue);

    }
    @DeleteMapping(path ="/{email}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PROFILE_DELETE')")
    public ResponseEntity<Void> deleteUser(@PathVariable("email") String email){
        logger.info("The incoming request to delete a user {}", email);
        userService.deleteUser(email);
        logger.info("Employee with email {} deleted successfully.", email);
        return ResponseEntity.noContent().build();
    }
    @PostMapping(path ="/{email}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PROFILE_DELETE')")
    public void deactivateUser(@PathVariable String email){
        logger.info("The incoming deactivate user request {} " , email);
        userService.deactivateUser(email);
        logger.info("User with email {} has been deactivated successfully.", email);
    }

    @GetMapping(path ="/view/{email}")
    @PreAuthorize("@userSecurity.canViewProfile(#email, authentication)")
    public ResponseEntity<UserProfileResponse> viewProfile(@PathVariable("email") String email) {
        logger.info("Dossier access requested for: {}", email);

        UserProfileDto userDto = userService.viewProfile(email);
        return ResponseEntity.ok(modelMapper.map(userDto, UserProfileResponse.class));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = refreshTokenService.generateNewAccessToken(request);
        return ResponseEntity.ok(response);
    }
    @GetMapping(path = "/all")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:READ')")
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(page = 0, size = 15) Pageable pageable) {

        Page<UserProfileDto> userPage = userService.findAllUsers(pageable, keyword);

        Page<UserProfileResponse> response = userPage.map(userDto ->
                modelMapper.map(userDto, UserProfileResponse.class)
        );

        return ResponseEntity.ok(response);
    }
}