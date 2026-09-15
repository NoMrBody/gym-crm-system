package bdd;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Component
public class WorkloadHttpSupport {

    private final MockMvc mockMvc;
    private final WorkloadWorld world;
    private final TestJwtTokens tokens;

    public WorkloadHttpSupport(MockMvc mockMvc, WorkloadWorld world, TestJwtTokens tokens) {
        this.mockMvc = mockMvc;
        this.world = world;
        this.tokens = tokens;
    }

    public void getSummary(String username) throws Exception {
        getPath("/api/v1/trainer-workloads/" + username);
    }

    public void getMonthly(String username, int year, int month) throws Exception {
        getPath("/api/v1/trainer-workloads/" + username + "/years/" + year + "/months/" + month);
    }

    public void getPath(String path) throws Exception {
        MockHttpServletRequestBuilder request = get(path);
        switch (world.tokenMode()) {
            case "none" -> {
            }
            case "garbage" -> request.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt");
            default -> request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.token("component.tester"));
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        world.setStatus(result.getResponse().getStatus());
        world.setBody(result.getResponse().getContentAsString());
    }
}
