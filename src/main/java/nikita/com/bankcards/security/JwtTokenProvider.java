package nikita.com.bankcards.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenProvider {

    private SecretKey jwtSecretKey;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationInMs;

    @Value("${app.jwt.issuer:bank-cards-api}")
    private String issuer;

    public JwtTokenProvider() {
    }

    @PostConstruct
    public void init() {
        String key = "mySecretKey1234567890123456789012345678901234567890123456789012345678";
        this.jwtSecretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        log.info("JWT key initialized ({} bytes)", key.getBytes().length);
    }

    // ДОБАВЬТЕ ЭТОТ МЕТОД!
    private SecretKey getSigningKey() {
        return jwtSecretKey;
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildToken(userDetails.getUsername(), extractRoles(authentication));
    }

    public String generateToken(String username, List<String> roles) {
        return buildToken(username, roles);
    }

    private String buildToken(String subject, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    private List<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.replace("ROLE_", ""))
                .toList();
    }

    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String getTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String authToken) {
        try {
            extractAllClaims(authToken);
            return !isTokenExpired(authToken);
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
        }
        return false;
    }

    private boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }

    public long getExpirationTime() {
        return jwtExpirationInMs;
    }
}