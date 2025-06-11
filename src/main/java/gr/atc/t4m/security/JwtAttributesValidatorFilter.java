package gr.atc.t4m.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.t4m.controller.responses.BaseAppResponse;
import gr.atc.t4m.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAttributesValidatorFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public JwtAttributesValidatorFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws IOException, ServletException {

        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtToken) {
            Jwt jwt = jwtToken.getToken();

            // Extract required fields
            String userId = JwtUtils.extractUserId(jwt);
            String pilotRole = JwtUtils.extractPilotRole(jwt);

            // Validate presence of required claims
            if (isEmpty(userId) ||  isEmpty(pilotRole)) {
                // Headers
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                // Response
                BaseAppResponse<String> responseMessage = BaseAppResponse.error("Invalid JWT token attributes", "Some information regarding Pilot Role or the User ID are missing from the token");
                String jsonResponse = objectMapper.writeValueAsString(responseMessage);

                response.getWriter().write(jsonResponse);
                response.getWriter().flush();
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
