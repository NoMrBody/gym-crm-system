package security;

import exception.AuthenticationException;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import service.AuthenticationService;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private AuthenticationService authenticationService;

    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter(authenticationService, JsonMapper.builder().build());
    }

    @Test
    void whitelistedRegistration_passesWithoutAuthorization() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/trainees");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        verify(authenticationService, never()).authenticate(anyString(), anyString());
        assertEquals(200, response.getStatus());
    }

    @Test
    void whitelistedPutLogin_passesWithoutAuthorization() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        verify(authenticationService, never()).authenticate(anyString(), anyString());
    }

    @Test
    void protectedEndpoint_missingHeader_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trainees/Jane.Smith");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        assertTrue(response.getContentAsString().contains("Authentication required"));
        assertEquals("Basic realm=\"gym-crm\"", response.getHeader("WWW-Authenticate"));
        verify(authenticationService, never()).authenticate(anyString(), anyString());
    }

    @Test
    void protectedEndpoint_invalidCredentials_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trainees/Jane.Smith");
        request.addHeader("Authorization", basic("Jane.Smith", "wrong"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(authenticationService.authenticate("Jane.Smith", "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        assertTrue(response.getContentAsString().contains("Invalid username or password"));
    }

    @Test
    void protectedEndpoint_validCredentials_invokesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trainees/Jane.Smith");
        request.addHeader("Authorization", basic("Jane.Smith", "secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        User user = new User();
        user.setUsername("Jane.Smith");
        when(authenticationService.authenticate("Jane.Smith", "secret")).thenReturn(user);

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        verify(authenticationService).authenticate("Jane.Smith", "secret");
    }

    @Test
    void actuatorPath_isWhitelisted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        verify(authenticationService, never()).authenticate(anyString(), anyString());
    }

    private static String basic(String username, String password) {
        String token = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
