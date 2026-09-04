package config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Logs each REST call with a transaction ID (added to MDC and the response header), recording
 * the endpoint, request body, response status and body, and the duration.
 *
 * <p>An incoming {@code X-Transaction-Id} is reused, so a transaction that starts in another
 * service keeps one ID end to end.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(TransactionLoggingFilter.class);

    private static final String TRANSACTION_ID = "transactionId";
    public static final String TRANSACTION_HEADER = "X-Transaction-Id";

    private static final int MAX_BODY_CHARS = 1024;
    private static final String[] UNLOGGED_PREFIXES = {"/actuator", "/swagger-ui", "/v3/api-docs"};

    /** Credentials travel through these endpoints, so any password-like field is redacted. */
    private static final Pattern PASSWORD_FIELD =
            Pattern.compile("(\"[^\"]*[pP]assword[^\"]*\"\\s*:\\s*)\"[^\"]*\"");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String transactionId = resolveTransactionId(request);
        MDC.put(TRANSACTION_ID, transactionId);
        response.setHeader(TRANSACTION_HEADER, transactionId);

        String endpoint = describe(request);
        boolean logBodies = shouldLogBodies(request);
        long start = System.currentTimeMillis();

        ContentCachingRequestWrapper wrappedRequest =
                logBodies ? new ContentCachingRequestWrapper(request, MAX_BODY_CHARS) : null;
        ContentCachingResponseWrapper wrappedResponse =
                logBodies ? new ContentCachingResponseWrapper(response) : null;

        log.info("Incoming REST call: {}", endpoint);
        try {
            filterChain.doFilter(
                    logBodies ? wrappedRequest : request,
                    logBodies ? wrappedResponse : response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (logBodies) {
                log.info("Completed REST call: {} request={} -> status {} response={} ({} ms)",
                        endpoint,
                        body(wrappedRequest.getContentAsByteArray()),
                        wrappedResponse.getStatus(),
                        body(wrappedResponse.getContentAsByteArray()),
                        duration);
                // Without this the cached body never reaches the real response.
                wrappedResponse.copyBodyToResponse();
            } else {
                log.info("Completed REST call: {} -> status {} ({} ms)",
                        endpoint, response.getStatus(), duration);
            }
            MDC.remove(TRANSACTION_ID);
        }
    }

    private static String resolveTransactionId(HttpServletRequest request) {
        String inbound = request.getHeader(TRANSACTION_HEADER);
        return StringUtils.hasText(inbound) ? inbound : UUID.randomUUID().toString();
    }

    private static String describe(HttpServletRequest request) {
        String query = request.getQueryString();
        return request.getMethod() + " " + request.getRequestURI() + (query != null ? "?" + query : "");
    }

    private static boolean shouldLogBodies(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : UNLOGGED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private static String body(byte[] content) {
        if (content.length == 0) {
            return "<empty>";
        }
        String text = PASSWORD_FIELD.matcher(new String(content, StandardCharsets.UTF_8))
                .replaceAll("$1\"***\"");
        return text.length() <= MAX_BODY_CHARS ? text : text.substring(0, MAX_BODY_CHARS) + "...<truncated>";
    }
}
