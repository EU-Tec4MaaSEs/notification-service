package gr.atc.t4m.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gr.atc.t4m.controller.responses.BaseAppResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

@Component
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        // Check if the request path is excluded from unauthorized handling
        String requestPath = request.getRequestURI();
        if (isExcludedPath(requestPath, request.getMethod())) {
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        // Check the validity of the token
        String errorMessage = "Unauthorized request. Check token and try again.";
        String errorCode = "Invalid or missing Token";

        if (authException instanceof OAuth2AuthenticationException) {
            errorMessage = "Invalid JWT provided";
            errorCode = "JWT has expired or is invalid";
        }

        BaseAppResponse<String> responseMessage = BaseAppResponse.error(errorMessage, errorCode);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.writeValue(response.getWriter(), responseMessage);

        response.getWriter().flush();
    }

    private boolean isExcludedPath(String path, String method) {
        // Define paths to exclude from unauthorized handling
        return antPathMatcher.match("/api/notification-service/**", path)
                || method.equalsIgnoreCase("OPTIONS")
                || antPathMatcher.match("/notifications/websocket/**", path)
                || antPathMatcher.match("/actuator/**", path);
    }
}