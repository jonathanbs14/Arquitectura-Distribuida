package com.prueba.graftsql.credito.auth.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prueba.graftsql.credito.auth.application.AuthResponse;
import com.prueba.graftsql.credito.auth.application.usercase.OAuthAccountAlreadyExistsException;
import com.prueba.graftsql.credito.auth.application.usercase.RegisterOAuthUseCase;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** Devuelve los JWT propios solo después de que Spring Security validó la respuesta OAuth2. */
@Component
@Profile("oauth")
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2ProfileMapper profileMapper;
    private final RegisterOAuthUseCase registerOAuth;
    private final ObjectMapper objectMapper;

    public OAuthLoginSuccessHandler(OAuth2ProfileMapper profileMapper, RegisterOAuthUseCase registerOAuth,
                                    ObjectMapper objectMapper) {
        this.profileMapper = profileMapper;
        this.registerOAuth = registerOAuth;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
            OAuth2ProfileMapper.OAuthUserProfile profile = profileMapper.map(oauth);
            AuthResponse authResponse = registerOAuth.execute(profile.userInfo(), profile.provider());
            clearAuthenticationAttributes(request);
            writeJson(response, HttpStatus.OK, authResponse);
        } catch (OAuthAccountAlreadyExistsException exception) {
            writeJson(response, HttpStatus.CONFLICT, Map.of("error", "account_already_exists"));
        } catch (IllegalArgumentException exception) {
            writeJson(response, HttpStatus.UNPROCESSABLE_ENTITY, Map.of("error", "profile_not_usable"));
        }
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
