package com.example.taskflow.service;

import com.example.taskflow.dto.CommentRequest;
import com.example.taskflow.dto.CommentResponse;
import com.example.taskflow.entity.Comment;
import com.example.taskflow.entity.Task;
import com.example.taskflow.entity.User;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.CommentRepository;
import com.example.taskflow.repository.UserRepository;
import com.example.taskflow.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final ProjectService projectService;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository,
                          TaskService taskService, ProjectService projectService) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.projectService = projectService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long taskId, AuthUser user) {
        taskService.getOwnedTask(taskId, user); // 404 if the task is missing or not yours
        return commentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(taskId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse add(Long taskId, AuthUser user, CommentRequest request) {
        Task task = taskService.getOwnedTask(taskId, user);
        User author = userRepository.getReferenceById(user.id());

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setContent(request.content().trim());
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void delete(Long id, AuthUser user) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        // A comment is reachable only through its project, so ownership is checked there
        if (!projectService.isOwner(comment.getTask().getProject(), user)) {
            throw notFound(id);
        }
        commentRepository.delete(comment);
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Comment not found with id " + id);
    }
}