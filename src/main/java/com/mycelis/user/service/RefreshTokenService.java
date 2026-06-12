package com.mycelis.user.service;

import com.mycelis.user.entity.RefreshToken;
import com.mycelis.user.entity.User;
import com.mycelis.user.model.request.TokenRefreshRequest;
import com.mycelis.user.model.response.TokenRefreshResponse;

import java.util.Optional;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyExpiration(RefreshToken token);

    void deleteTokenByUser(User user);

    Optional<RefreshToken> findByToken(String token);

    TokenRefreshResponse generateNewAccessToken(TokenRefreshRequest request);
}