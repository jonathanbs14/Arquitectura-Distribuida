package com.prueba.graftsql.credito.auth.application.usercase;

/**
 * Evita enlazar automáticamente una identidad externa a una cuenta local existente.
 * Ese enlace debe hacerse tras autenticar al propietario de la cuenta local.
 */
public class OAuthAccountAlreadyExistsException extends RuntimeException {

    public OAuthAccountAlreadyExistsException() {
        super("Ya existe una cuenta con este correo; inicie sesión con el método original para vincularla.");
    }
}
