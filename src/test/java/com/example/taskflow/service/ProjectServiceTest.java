package com.example.taskflow.service;

import com.example.taskflow.dto.ProjectRequest;
import com.example.taskflow.entity.Project;
import com.example.taskflow.entity.Role;
import com.example.taskflow.entity.User;
import com.example.taskflow.exception.ConflictException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.ProjectRepository;
import com.example.taskflow.repository.UserRepository;
import com.example.taskflow.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService service;

    private final AuthUser ann = new AuthUser(1L, "ann@example.com", Role.USER);

    private static Project project(long id, long ownerId) {
        User owner = new User();
        owner.setId(ownerId);
        Project p = new Project();
        p.setId(id);
        p.setName("Existing");
        p.setOwner(owner);
        return p;
    }

    @Test
    void createTrimsTheNameAndAssignsTheCallerAsOwner() {
        User reference = new User();
        reference.setId(1L);
        when(projectRepository.existsByOwnerIdAndNameIgnoreCase(1L, "Alpha")).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(reference);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(ann, new ProjectRequest("  Alpha  ", "desc"));

        ArgumentCaptor<Project> saved = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Alpha");
        assertThat(saved.getValue().getOwner()).isSameAs(reference);
    }

    @Test
    void createRejectsADuplicateNameAndNeverSaves() {
        when(projectRepository.existsByOwnerIdAndNameIgnoreCase(1L, "Alpha")).thenReturn(true);

        assertThatThrownBy(() -> service.create(ann, new ProjectRequest("Alpha", null)))
                .isInstanceOf(ConflictException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void someoneElsesProjectLooksExactlyLikeAMissingOne() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project(10L, 2L))); // user 2's
        when(projectRepository.findById(11L)).thenReturn(Optional.empty());

        Throwable notYours = catchThrowable(() -> service.getOwned(10L, ann));
        Throwable missing = catchThrowable(() -> service.getOwned(11L, ann));

        assertThat(notYours).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id 10");
        assertThat(missing).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id 11");
    }

    @Test
    void theOwnerCanOpenTheirOwnProject() {
        Project mine = project(10L, 1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mine));

        assertThat(service.getOwned(10L, ann)).isSameAs(mine);
    }

    @Test
    void aProjectWithNoOwnerBelongsToNobody() {
        Project legacy = new Project();
        legacy.setId(3L);

        assertThat(service.isOwner(legacy, ann)).isFalse();
    }

    @Test
    void updateRejectsANameUsedByAnotherProject() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project(5L, 1L)));
        when(projectRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(1L, "Taken", 5L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, ann, new ProjectRequest("Taken", null)))
                .isInstanceOf(ConflictException.class);

        verify(projectRepository, never()).save(any());
    }
}