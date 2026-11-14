package ISIMS.olive_connect.service;

import ISIMS.olive_connect.entity.RefreshToken;
import ISIMS.olive_connect.exception.TokenRefreshException;
import ISIMS.olive_connect.repository.RefreshTokenRepository;
import ISIMS.olive_connect.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class RefreshTokenService {

    @Value("${olive.app.refreshTokenDurationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtils jwtUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtils = jwtUtils;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(UserDetails userDetails) {
        // Clear any prior refresh tokens for this user
        refreshTokenRepository.deleteByEmail(userDetails.getUsername());

        String tokenString = jwtUtils.generateRefreshToken(userDetails);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEmail(userDetails.getUsername());
        refreshToken.setToken(tokenString);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token was revoked. Please log in again.");
        }

        if (token.getExpiryDate().compareTo(Instant.now()) < 0 || !jwtUtils.validateRefreshToken(token.getToken())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token was expired or invalid. Please sign in again.");
        }

        return token;
    }

    public void revokeToken(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }
}