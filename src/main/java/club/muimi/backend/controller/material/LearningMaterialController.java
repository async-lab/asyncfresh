package club.muimi.backend.controller.material;

import club.muimi.backend.common.api.ApiResponse;
import club.muimi.backend.entity.StoredFile;
import club.muimi.backend.service.file.FileStorageService;
import club.muimi.backend.service.material.LearningMaterialService;
import club.muimi.backend.vo.material.LearningMaterialVo;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/materials")
public class LearningMaterialController {

    private final LearningMaterialService learningMaterialService;
    private final FileStorageService fileStorageService;

    public LearningMaterialController(
            LearningMaterialService learningMaterialService,
            FileStorageService fileStorageService
    ) {
        this.learningMaterialService = learningMaterialService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ApiResponse<List<LearningMaterialVo>> listMaterials() {
        return ApiResponse.success(learningMaterialService.listVisibleMaterials(), "ok");
    }

    @GetMapping("/{materialId}")
    public ApiResponse<LearningMaterialVo> getMaterial(@PathVariable Long materialId) {
        return ApiResponse.success(learningMaterialService.getMaterial(materialId), "ok");
    }

    @GetMapping("/{materialId}/attachment")
    public ResponseEntity<Resource> downloadMaterialAttachment(@PathVariable Long materialId) {
        return buildDownloadResponse(learningMaterialService.getMaterialAttachmentFile(materialId));
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
