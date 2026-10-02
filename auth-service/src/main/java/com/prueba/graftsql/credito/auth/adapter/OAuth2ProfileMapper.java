package com.prueba.graftsql.credito.auth.adapter;

import com.prueba.graftsql.credito.auth.domain.AuthProvider;
import com.prueba.graftsql.credito.auth.domain.SocialUserInfo;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Normaliza el perfil validado por el proveedor sin persistir sus tokens de acceso. */
@Component
@Profile("oauth")
public class OAuth2ProfileMapper {

    private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";
    private final OAuth2AuthorizedClientService authorizedClients;
    private final RestClient restClient;

    public OAuth2ProfileMapper(OAuth2AuthorizedClientService authorizedClients, RestClient.Builder restClientBuilder) {
        this.authorizedClients = authorizedClients;
        this.restClient = restClientBuilder.build();
    }

    public OAuthUserProfile map(OAuth2AuthenticationToken authentication) {
        String registrationId = authentication.getAuthorizedClientRegistrationId();
        AuthProvider provider = switch (registrationId.toLowerCase()) {
            case "google" -> AuthProvider.GOOGLE;
            case "github" -> AuthProvider.GITHUB;
            default -> throw new IllegalArgumentException("Proveedor OAuth no permitido");
        };

        OAuth2User principal = authentication.getPrincipal();
        String externalId = requiredAttribute(principal, provider == AuthProvider.GOOGLE ? "sub" : "id");
        String email = stringAttribute(principal, "email");
        String name = stringAttribute(principal, "name", "login");

        if (provider == AuthProvider.GOOGLE && Boolean.FALSE.equals(principal.getAttribute("email_verified"))) {
            throw new IllegalArgumentException("Google no confirmó el correo de la cuenta");
        }
        if (provider == AuthProvider.GITHUB) {
            email = githubVerifiedEmail(authentication);
        }
        if (isBlank(email)) {
            throw new IllegalArgumentException("El proveedor no entregó un correo verificado");
        }
        if (isBlank(name)) {
            name = email;
        }
        return new OAuthUserProfile(new SocialUserInfo(externalId, email, name), provider);
    }

    private String githubVerifiedEmail(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClients.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(), authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalArgumentException("No fue posible obtener el token de GitHub");
        }
        List<GitHubEmail> emails = restClient.get()
                .uri(GITHUB_EMAILS_URL)
                .headers(headers -> {
                    headers.setBearerAuth(client.getAccessToken().getTokenValue());
                    headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                    headers.set(HttpHeaders.USER_AGENT, "sistema-credito-auth-service");
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        return emails == null ? null : emails.stream()
                .filter(GitHubEmail::verified)
                .sorted((first, second) -> Boolean.compare(second.primary(), first.primary()))
                .map(GitHubEmail::email)
                .filter(email -> !isBlank(email))
                .findFirst()
                .orElse(null);
    }

    private static String requiredAttribute(OAuth2User principal, String attribute) {
        String value = stringAttribute(principal, attribute);
        if (isBlank(value)) {
            throw new IllegalArgumentException("El proveedor no entregó el identificador de la cuenta");
        }
        return value;
    }

    private static String stringAttribute(OAuth2User principal, String... attributes) {
        for (String attribute : attributes) {
            Object value = principal.getAttribute(attribute);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record OAuthUserProfile(SocialUserInfo userInfo, AuthProvider provider) {
        public OAuthUserProfile {
            Objects.requireNonNull(userInfo);
            Objects.requireNonNull(provider);
        }
    }

    private record GitHubEmail(String email, boolean primary, boolean verified) {
    }
}
