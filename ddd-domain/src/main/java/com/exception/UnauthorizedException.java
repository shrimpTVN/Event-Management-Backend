package com.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * Thrown when authentication fails due to invalid credentials supplied by the client.
 *
 * <p>Examples:
 * <ul>
 *   <li>Incorrect password on login or password-change request</li>
 *   <li>Old password does not match during change-password flow</li>
 *   <li>Email / account mismatch during password-change</li>
 * </ul>
 *
 * <p>Maps to HTTP 401 Unauthorized — the client must authenticate itself to get the
 * requested resource.
 *
 * <p><b>Note:</b> Do NOT use this for role-based access denials; use Spring Security's
 * {@code AccessDeniedException} (→ 403 Forbidden) for those cases.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
