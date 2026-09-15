package bdd;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Component
public class CoreHttpSupport {

    private final MockMvc mockMvc;
    private final CoreWorld world;

    public CoreHttpSupport(MockMvc mockMvc, CoreWorld world) {
        this.mockMvc = mockMvc;
        this.world = world;
    }

    public void postJson(String path, String json) throws Exception {
        postJson(path, json, world.accessToken());
    }

    public void postJson(String path, String json, String token) throws Exception {
        MockHttpServletRequestBuilder request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
        if (token != null && !token.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        perform(request);
    }

    public void getPath(String path) throws Exception {
        getPath(path, world.accessToken());
    }

    public void getPath(String path, String token) throws Exception {
        MockHttpServletRequestBuilder request = get(path);
        if (token != null && !token.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        perform(request);
    }

    public void perform(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        world.setStatus(result.getResponse().getStatus());
        world.setBody(result.getResponse().getContentAsString());
    }
}
