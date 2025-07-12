package com.server.running_handai.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * Secret Key 객체를 생성합니다.
     * Base64로 인코딩된 Secret Key를 디코딩하여 HMAC 서명용 Secret Key 객체로 반환합니다.
     *
     * @return Jwt 서명에 사용할 Secret Key 객체
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token을 생성합니다.
     *
     * @param id 사용자 id
     * @return 생성된 Access Token
     */
    public String createAccessToken(Long id) {
        return Jwts.builder()
                .setSubject(String.valueOf(id))
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token을 생성합니다.
     *
     * @return 생성된 Refresh Token
     */
    public String createRefreshToken() {
        return Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Jwt Token의 유효성(서명, 만료 등)을 검증합니다.
     *
     * @param token 검증할 Jwt Token
     * @return 토큰이 유효하면 true, 아니면 false
     */
    public boolean isTokenValidate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Jwt Token의 만료 여부를 검증합니다.
     *
     * @param token 검증할 Jwt Token
     * @return 만료되면 true, 아니면 false
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration =
                    Jwts.parserBuilder()
                            .setSigningKey(getSigningKey())
                            .build()
                            .parseClaimsJws(token)
                            .getBody()
                            .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            // 파싱에 실패할 경우, 만료되었다고 간주
            return true;
        }
    }

    /**
     * Access Token에 담긴 사용자 id를 추출합니다.
     *
     * @param accessToken Access Token
     * @return Access Token에 담긴 사용자 id
     */
    public String getId(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(accessToken)
                .getBody()
                .getSubject();
    }

    /**
     * HTTP 요청 헤더에서 Access Token을 추출합니다.
     * "Bearer " 접두어가 붙은 곳에서 실제 Access Token만 반환합니다.
     *
     * @param httpServletRequest HTTP 요청 객체
     * @return Authorization Header에서 추출한 Access Token, 없거나 파싱 실패할 경우 null
     */
    public String getToken(HttpServletRequest httpServletRequest) {
        String bearerToken = httpServletRequest.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
