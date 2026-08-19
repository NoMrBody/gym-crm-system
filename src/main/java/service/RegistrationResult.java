package service;

import dto.TokenResponse;

/**
 * A freshly created profile together with its raw generated password and, once the facade
 * has enriched it, the bearer token the caller can use straight away. The password cannot
 * be read back from the entity, which stores only a salted hash.
 */
public record RegistrationResult<T>(T profile, String rawPassword, TokenResponse token) {

    public RegistrationResult(T profile, String rawPassword) {
        this(profile, rawPassword, null);
    }

    public RegistrationResult<T> withToken(TokenResponse token) {
        return new RegistrationResult<>(profile, rawPassword, token);
    }
}
