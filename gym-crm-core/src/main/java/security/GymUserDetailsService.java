package security;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.UserRepository;

/**
 * Loads gym users for authentication. The domain {@code isActive} flag marks an active gym
 * membership rather than an enabled account, so deactivated members can still sign in and
 * reactivate themselves; only a brute-force block locks the account.
 */
@Service
public class GymUserDetailsService implements UserDetailsService {
    public static final String DEFAULT_ROLE = "ROLE_USER";

    private static final Logger log = LoggerFactory.getLogger(GymUserDetailsService.class);

    private final UserRepository userRepository;
    private final BruteForceProtector bruteForceProtector;

    public GymUserDetailsService(UserRepository userRepository, BruteForceProtector bruteForceProtector) {
        this.userRepository = userRepository;
        this.bruteForceProtector = bruteForceProtector;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.debug("No user found with username '{}'", username);
                    return new UsernameNotFoundException("Invalid username or password");
                });

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(AuthorityUtils.createAuthorityList(DEFAULT_ROLE))
                .accountLocked(bruteForceProtector.isBlocked(user.getUsername()))
                .build();
    }
}
