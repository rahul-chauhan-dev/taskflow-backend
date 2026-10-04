package com.example.taskflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessControlTest extends IntegrationTestBase {

    private TestUser ann;
    private TestUser ben;
    private long annProject;
    private long annTask;
    private long annComment;

    @BeforeEach
    void setUp() throws Exception {
        ann = registerUser("Ann");
        ben = registerUser("Ben");
        annProject = createProject(ann, "Ann private project");
        annTask = createTask(ann, annProject, "{\"title\":\"Ann secret task\"}");
        annComment = addComment(ann, annTask, "Ann secret comment");
    }

    @Test
    void benCannotSeeOrChangeAnnsProject() throws Exception {
        mockMvc.perform(getAs(ben, "/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(getAs(ben, "/api/projects/search?name=Ann"))
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(getAs(ben, "/api/projects/" + annProject))
                .andExpect(status().isNotFound());

        mockMvc.perform(sendAs(HttpMethod.PUT, ben, "/api/projects/" + annProject,
                        "{\"name\":\"hacked\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(deleteAs(ben, "/api/projects/" + annProject))
                .andExpect(status().isNotFound());

        // Ann's project is untouched
        mockMvc.perform(getAs(ann, "/api/projects/" + annProject))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ann private project"));
    }

    @Test
    void benCannotTouchAnnsTasks() throws Exception {
        String tasks = "/api/projects/" + annProject + "/tasks";

        mockMvc.perform(getAs(ben, tasks)).andExpect(status().isNotFound());
        mockMvc.perform(sendAs(HttpMethod.POST, ben, tasks, "{\"title\":\"planted\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(sendAs(HttpMethod.PUT, ben, "/api/tasks/" + annTask,
                        "{\"title\":\"hacked\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(sendAs(HttpMethod.PATCH, ben, "/api/tasks/" + annTask + "/status",
                        "{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(deleteAs(ben, "/api/tasks/" + annTask))
                .andExpect(status().isNotFound());

        // Ann's task is exactly as she left it: no side effects from Ben's attempts
        mockMvc.perform(getAs(ann, tasks))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Ann secret task"))
                .andExpect(jsonPath("$.items[0].status").value("TODO"));
    }

    @Test
    void benCannotReadWriteOrDeleteAnnsComments() throws Exception {
        String comments = "/api/tasks/" + annTask + "/comments";

        mockMvc.perform(getAs(ben, comments)).andExpect(status().isNotFound());
        mockMvc.perform(sendAs(HttpMethod.POST, ben, comments, "{\"content\":\"planted\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(deleteAs(ben, "/api/comments/" + annComment))
                .andExpect(status().isNotFound());

        mockMvc.perform(getAs(ann, comments))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Ann secret comment"));
    }

    @Test
    void dashboardsContainOnlyTheCallersData() throws Exception {
        mockMvc.perform(getAs(ben, "/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCount").value(0))
                .andExpect(jsonPath("$.totalTasks").value(0))
                .andExpect(jsonPath("$.recentActivity", hasSize(0)));

        // sanity check, so the test cannot pass vacuously: Ann does see her own data
        mockMvc.perform(getAs(ann, "/api/dashboard"))
                .andExpect(jsonPath("$.projectCount").value(1))
                .andExpect(jsonPath("$.totalTasks").value(1))
                .andExpect(jsonPath("$.recentActivity", hasSize(1)));
    }

    @Test
    void projectNamesAreUniquePerOwnerNotGlobally() throws Exception {
        createProject(ben, "Ann private project"); // same name as Ann's: allowed

        mockMvc.perform(sendAs(HttpMethod.POST, ben, "/api/projects",
                        "{\"name\":\"ann PRIVATE project\"}"))
                .andExpect(status().isConflict()); // but not twice in the same account
    }
}