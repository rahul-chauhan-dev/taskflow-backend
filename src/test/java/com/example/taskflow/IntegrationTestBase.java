package com.example.taskflow;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    public record TestUser(String email, String token) {
        public String bearer() {
            return "Bearer " + token;
        }
    }

    // A brand-new user with a random email, so tests never share data
    protected TestUser registerUser(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String body = "{\"name\":\"" + name + "\",\"email\":\"" + email
                + "\",\"password\":\"password123\"}";

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(response, "$.token");
        return new TestUser(email, token);
    }

    protected MockHttpServletRequestBuilder getAs(TestUser user, String url) {
        return get(url).header("Authorization", user.bearer());
    }

    protected MockHttpServletRequestBuilder deleteAs(TestUser user, String url) {
        return delete(url).header("Authorization", user.bearer());
    }

    // POST / PUT / PATCH with a JSON body
    protected MockHttpServletRequestBuilder sendAs(HttpMethod method, TestUser user,
                                                   String url, String json) {
        return request(method, url)
                .header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    protected long createProject(TestUser user, String name) throws Exception {
        return postForId("/api/projects", user, "{\"name\":\"" + name + "\"}");
    }

    protected long createTask(TestUser user, long projectId, String json) throws Exception {
        return postForId("/api/projects/" + projectId + "/tasks", user, json);
    }

    protected long addComment(TestUser user, long taskId, String content) throws Exception {
        return postForId("/api/tasks/" + taskId + "/comments", user,
                "{\"content\":\"" + content + "\"}");
    }

    private long postForId(String url, TestUser user, String json) throws Exception {
        String response = mockMvc.perform(sendAs(HttpMethod.POST, user, url, json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(response, "$.id");
        return id.longValue();
    }
}