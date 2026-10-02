package com.prueba.graftsql.credito.auth.web;

import com.prueba.graftsql.credito.auth.application.AuthResponse;
import com.prueba.graftsql.credito.auth.application.LoginRequest;
import com.prueba.graftsql.credito.auth.application.MFASetupResponse;
import com.prueba.graftsql.credito.auth.application.MFAVerifyRequest;
import com.prueba.graftsql.credito.auth.application.usercase.LoginUseCase;
import com.prueba.graftsql.credito.auth.application.usercase.RegisterLocalUseCase;
import com.prueba.graftsql.credito.auth.application.usercase.SetupMFAUseCase;
import com.prueba.graftsql.credito.auth.application.usercase.VerifyMFAUseCase;
import com.prueba.graftsql.credito.auth.adapter.MFAVerifyResponse;
import com.prueba.graftsql.credito.auth.adapter.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.bind.annotation.*;

/**
 * Límite de identidad del sistema. No acepta tokens sin verificarlos contra un proveedor OIDC.
 * Las URL y secretos del proveedor se inyectan al desplegar y nunca se guardan en este repositorio.
 */

@RestController
@RequestMapping({"", "/api/auth"})
public class AuthController {
    private final RegisterLocalUseCase registerLocalUseCase;
    private final LoginUseCase loginUseCase;
    private final SetupMFAUseCase setupMFAUseCase;
    private final VerifyMFAUseCase verifyMFAUseCase;

    public AuthController(RegisterLocalUseCase registerLocalUseCase, LoginUseCase loginUseCase,
                          SetupMFAUseCase setupMFAUseCase, VerifyMFAUseCase verifyMFAUseCase) {
        this.registerLocalUseCase = registerLocalUseCase;
        this.loginUseCase = loginUseCase;
        this.setupMFAUseCase = setupMFAUseCase;
        this.verifyMFAUseCase = verifyMFAUseCase;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return registerLocalUseCase.execute(request.getEmail(), request.getFullName(), request.getPassword());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return loginUseCase.execute(request);
    }

    @GetMapping("/oauth/{provider}")
    public void iniciarOAuth(@PathVariable String provider, HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        String registrationId = switch (provider.toLowerCase()) {
            case "google", "github" -> provider.toLowerCase();
            default -> throw new IllegalArgumentException("Proveedor OAuth no soportado");
        };
        response.sendRedirect(request.getContextPath() + "/oauth2/authorization/" + registrationId);
    }

    @PostMapping("/mfa/setup/{userId}")
    public MFASetupResponse setupMFA(@PathVariable String userId) {
        return setupMFAUseCase.execute(userId);
    }

    @PostMapping("/mfa/verify")
    public MFAVerifyResponse verifyMFA(@RequestBody MFAVerifyRequest request) {
        boolean isValid = verifyMFAUseCase.execute(request);
        return new MFAVerifyResponse(isValid);
    }
}
