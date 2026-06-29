package club.muimi.backend.controller.task;

import club.muimi.backend.common.api.ApiResponse;
import club.muimi.backend.dto.task.SubmitTaskRequest;
import club.muimi.backend.entity.StoredFile;
import club.muimi.backend.service.file.FileStorageService;
import club.muimi.backend.service.task.TaskService;
import club.muimi.backend.vo.task.TaskDetailVo;
import club.muimi.backend.vo.task.TaskSubmissionVo;
import club.muimi.backend.vo.task.TaskSummaryVo;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;
    private final FileStorageService fileStorageService;

    public TaskController(TaskService taskService, FileStorageService fileStorageService) {
        this.taskService = taskService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ApiResponse<List<TaskSummaryVo>> listCurrentUserTasks() {
        return ApiResponse.success(taskService.listCurrentUserTasks(), "ok");
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("@authzService.canAccessTask(authentication, #taskId)")
    public ApiResponse<TaskDetailVo> getTaskDetail(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.getTaskDetail(taskId), "ok");
    }

    @GetMapping("/{taskId}/submission")
    @PreAuthorize("@authzService.canAccessTask(authentication, #taskId)")
    public ApiResponse<TaskSubmissionVo> getCurrentUserSubmission(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.getCurrentUserSubmission(taskId), "ok");
    }

    @PostMapping("/{taskId}/submission")
    @PreAuthorize("@authzService.canAccessTask(authentication, #taskId)")
    public ApiResponse<TaskSubmissionVo> submitTask(
            @PathVariable Long taskId,
            @Valid @RequestBody SubmitTaskRequest request
    ) {
        return ApiResponse.success(taskService.submitTask(taskId, request), "任务提交成功");
    }

    @GetMapping("/{taskId}/attachment")
    @PreAuthorize("@authzService.canAccessTask(authentication, #taskId)")
    public ResponseEntity<Resource> downloadTaskAttachment(@PathVariable Long taskId) {
        return buildDownloadResponse(taskService.getTaskAttachmentFile(taskId));
    }

    @GetMapping("/{taskId}/submission/attachment")
    @PreAuthorize("@authzService.canAccessTask(authentication, #taskId)")
    public ResponseEntity<Resource> downloadCurrentUserSubmissionAttachment(@PathVariable Long taskId) {
        return buildDownloadResponse(taskService.getCurrentUserSubmissionAttachmentFile(taskId));
    }

    private ResponseEntity<Resource> buildDownloadResponse(StoredFile storedFile) {
        Resource resource = fileStorageService.loadAsResource(storedFile);
        MediaType mediaType = storedFile.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(storedFile.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(storedFile.getSizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(storedFile.getOriginalFileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(resource);
    }
}
