package ISIMS.olive_connect.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${olive.app.jwtSecret}")
    private String jwtSecret;

    @Value("${olive.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${olive.app.jwtRefreshSecret}")
    private String jwtRefreshSecret;

    @Value("${olive.app.refreshTokenDurationMs}")
    private long refreshTokenDurationMs;

    private Key getAccessSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private Key getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(jwtRefreshSecret.getBytes());
    }

    // 1. Generate Access Token using Access Secret
    public String generateJwtToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getAccessSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. Generate Refresh Token (JWT-based) using Refresh Secret
    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + refreshTokenDurationMs))
                .signWith(getRefreshSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getAccessSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getUserNameFromRefreshToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getRefreshSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getAccessSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException e) {
            System.err.println("Invalid Access JWT token: " + e.getMessage());
        }
        return false;
    }

    public boolean validateRefreshToken(String refreshToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getRefreshSigningKey()).build().parseClaimsJws(refreshToken);
            return true;
        } catch (JwtException e) {
            System.err.println("Invalid Refresh JWT token: " + e.getMessage());
        }
        return false;
    }
}