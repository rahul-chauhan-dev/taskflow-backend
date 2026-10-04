package com.example.taskflow;

import com.example.taskflow.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiBehaviourTest extends IntegrationTestBase {

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void blankProjectNameGivesAFieldError() throws Exception {
        TestUser ann = registerUser("Ann");

        mockMvc.perform(sendAs(HttpMethod.POST, ann, "/api/projects", "{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"));
    }

    @Test
    void duplicateProjectNameIsAConflictOnTheNameField() throws Exception {
        TestUser ann = registerUser("Ann");
        createProject(ann, "Alpha");

        mockMvc.perform(sendAs(HttpMethod.POST, ann, "/api/projects", "{\"name\":\"alpha\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void brokenJsonAndBadValuesAreBadRequests() throws Exception {
        TestUser ann = registerUser("Ann");
        long project = createProject(ann, "Alpha");
        String url = "/api/projects/" + project + "/tasks";

        String[] badBodies = {
                "{\"title\":",                                    // broken JSON
                "{\"title\":\"x\",\"priority\":\"URGENT\"}",      // unknown enum value
                "{\"title\":\"x\",\"dueDate\":\"15-10-2026\"}"    // wrong date format
        };
        for (String body : badBodies) {
            mockMvc.perform(sendAs(HttpMethod.POST, ann, url, body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Malformed request body")));
        }
    }

    @Test
    void unknownUrlsAre404WithTheStandardErrorShape() throws Exception {
        TestUser ann = registerUser("Ann");

        mockMvc.perform(getAs(ann, "/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"));
    }

    @Test
    void badQueryParametersAre400() throws Exception {
        TestUser ann = registerUser("Ann");
        long project = createProject(ann, "Alpha");

        for (String query : new String[]{"sort=project.name,asc", "status=FOO", "priority=URGENT"}) {
            mockMvc.perform(getAs(ann, "/api/projects/" + project + "/tasks?" + query))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void paginationFiltersAndSearchWork() throws Exception {
        TestUser ann = registerUser("Ann");
        long project = createProject(ann, "Alpha");
        for (int i = 1; i <= 7; i++) {
            String priority = i <= 3 ? "HIGH" : "LOW";
            String title = i == 7 ? "Find the NEEDLE" : "Task " + i;
            createTask(ann, project,
                    "{\"title\":\"" + title + "\",\"priority\":\"" + priority + "\"}");
        }
        String url = "/api/projects/" + project + "/tasks";

        mockMvc.perform(getAs(ann, url + "?size=5"))
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.totalItems").value(7))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(getAs(ann, url + "?size=5&page=1"))
                .andExpect(jsonPath("$.items", hasSize(2)));

        mockMvc.perform(getAs(ann, url + "?priority=HIGH"))
                .andExpect(jsonPath("$.totalItems").value(3));

        mockMvc.perform(getAs(ann, url + "?q=needle"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Find the NEEDLE"));
    }

    @Test
    void commentsAreCountedAndDeletedWithTheirTask() throws Exception {
        TestUser ann = registerUser("Ann");
        long project = createProject(ann, "Alpha");
        long task = createTask(ann, project, "{\"title\":\"Write docs\"}");
        addComment(ann, task, "First");
        addComment(ann, task, "Second");

        mockMvc.perform(getAs(ann, "/api/projects/" + project + "/tasks"))
                .andExpect(jsonPath("$.items[0].commentCount").value(2));

        mockMvc.perform(getAs(ann, "/api/tasks/" + task + "/comments"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("First"))
                .andExpect(jsonPath("$[0].authorName").value("Ann"));

        mockMvc.perform(deleteAs(ann, "/api/tasks/" + task))
                .andExpect(status().isNoContent());
        assertThat(commentRepository.countByTaskId(task)).isZero(); // cascade worked
    }

    @Test
    void emptyCommentsAreRejected() throws Exception {
        TestUser ann = registerUser("Ann");
        long project = createProject(ann, "Alpha");
        long task = createTask(ann, project, "{\"title\":\"Write docs\"}");

        mockMvc.perform(sendAs(HttpMethod.POST, ann, "/api/tasks/" + task + "/comments",
                        "{\"content\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").value("Comment cannot be empty"));
    }

    @Test
    void dashboardNumbersAreCorrect() throws Exception {
        TestUser ann = registerUser("Ann");
        long project = createProject(ann, "Alpha");
        LocalDate today = LocalDate.now();

        createTask(ann, project, "{\"title\":\"overdue\",\"dueDate\":\"" + today.minusDays(1) + "\"}");
        createTask(ann, project, "{\"title\":\"done late\",\"status\":\"DONE\",\"dueDate\":\""
                + today.minusDays(1) + "\"}");
        createTask(ann, project, "{\"title\":\"soon\",\"dueDate\":\"" + today.plusDays(3) + "\"}");
        createTask(ann, project, "{\"title\":\"urgent\",\"priority\":\"HIGH\"}");

        mockMvc.perform(getAs(ann, "/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCount").value(1))
                .andExpect(jsonPath("$.totalTasks").value(4))
                .andExpect(jsonPath("$.todoCount").value(3))
                .andExpect(jsonPath("$.inProgressCount").value(0))
                .andExpect(jsonPath("$.doneCount").value(1))
                .andExpect(jsonPath("$.overdueCount").value(1))        // a DONE task is never overdue
                .andExpect(jsonPath("$.dueSoonCount").value(1))
                .andExpect(jsonPath("$.highPriorityOpenCount").value(1))
                .andExpect(jsonPath("$.projects[0].taskCount").value(4));
    }
}