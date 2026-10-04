package com.example.taskflow.service;

import com.example.taskflow.dto.ProjectRequest;
import com.example.taskflow.dto.ProjectResponse;
import com.example.taskflow.entity.Project;
import com.example.taskflow.entity.User;
import com.example.taskflow.exception.ConflictException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.ProjectRepository;
import com.example.taskflow.repository.UserRepository;
import com.example.taskflow.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAll(AuthUser user) {
        return projectRepository.findAllWithCounts(user.id());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> searchByName(AuthUser user, String name) {
        return projectRepository.searchWithCounts(user.id(), name);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getResponseById(Long id, AuthUser user) {
        getOwned(id, user); // 404 for missing OR someone else's
        return projectRepository.findOneWithCounts(id).orElseThrow(() -> notFound(id));
    }

    // Entity for internal use (TaskService calls it). Never return this from a controller.
    @Transactional(readOnly = true)
    public Project getOwned(Long id, AuthUser user) {
        Project project = projectRepository.findById(id).orElseThrow(() -> notFound(id));
        if (!isOwner(project, user)) {
            throw notFound(id); // 404, not 403: do not confirm that other people's projects exist
        }
        return project;
    }

    public boolean isOwner(Project project, AuthUser user) {
        return project.getOwner() != null && project.getOwner().getId().equals(user.id());
    }

    @Transactional
    public ProjectResponse create(AuthUser user, ProjectRequest request) {
        String name = request.name().trim();
        if (projectRepository.existsByOwnerIdAndNameIgnoreCase(user.id(), name)) {
            throw duplicateName(name);
        }
        User owner = userRepository.getReferenceById(user.id());

        Project project = new Project();
        project.setName(name);
        project.setDescription(request.description());
        project.setOwner(owner);
        return ProjectResponse.forNewProject(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, AuthUser user, ProjectRequest request) {
        Project project = getOwned(id, user);
        String name = request.name().trim();
        if (projectRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(user.id(), name, id)) {
            throw duplicateName(name);
        }
        project.setName(name);
        project.setDescription(request.description());
        projectRepository.save(project);
        return projectRepository.findOneWithCounts(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public void delete(Long id, AuthUser user) {
        projectRepository.delete(getOwned(id, user));
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Project not found with id " + id);
    }

    private static ConflictException duplicateName(String name) {
        return new ConflictException("name", "A project named \"" + name + "\" already exists");
    }
}