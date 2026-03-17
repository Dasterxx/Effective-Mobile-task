package nikita.com.bankcards.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("🔍 JWT FILTER START: {}", request.getRequestURI());

        String jwt = resolveToken(request);
        log.info("=== JWT DEBUG ===");
        log.info("URI: {}", request.getRequestURI());
        log.info("JWT present: {}", jwt != null);

        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Authentication auth = createAuthentication(jwt, request);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("✅ Auth set: {}, authorities: {}", auth.getName(), auth.getAuthorities());
        }

        log.info("🔍 JWT FILTER END");
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private Authentication createAuthentication(String jwt, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromToken(jwt);
        List<String> roles = tokenProvider.getRolesFromToken(jwt);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        return authentication;
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean skip = path.startsWith("/api/auth/") ||
                path.startsWith("/actuator/health") ||
                path.equals("/error") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/");

        log.info("🔍 shouldNotFilter: {} -> SKIP={}", path, skip);
        return skip;
    }

}