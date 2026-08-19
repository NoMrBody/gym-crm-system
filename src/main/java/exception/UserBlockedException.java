package exception;

/**
 * Raised when a user is temporarily locked out after too many failed login attempts.
 */
public class UserBlockedException extends RuntimeException {

    public UserBlockedException(String message) {
        super(message);
    }
}
