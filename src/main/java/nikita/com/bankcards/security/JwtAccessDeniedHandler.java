package nikita.com.bankcards.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.error("Access denied for '{}': {}", request.getRequestURI(), accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpServletResponse.SC_FORBIDDEN);
        problemDetail.setTitle("Forbidden");
        problemDetail.setDetail("You don't have permission to access this resource");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}