package club.muimi.backend.service.file;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.StoredFilePurpose;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.config.FileStorageProperties;
import club.muimi.backend.config.TaskModuleProperties;
import club.muimi.backend.entity.StoredFile;
import club.muimi.backend.exception.ForbiddenException;
import club.muimi.backend.exception.ValidationException;
import club.muimi.backend.repository.FileUploadSessionRepository;
import club.muimi.backend.repository.StoredFileRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.user.CurrentUserService;
import club.muimi.backend.vo.file.StoredFileVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private StoredFileRepository storedFileRepository;
    @Mock
    private FileUploadSessionRepository fileUploadSessionRepository;
    @Mock
    private CurrentUserService currentUserService;

    private LocalFileStorageService localFileStorageService;

    @BeforeEach
    void setUp() {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        fileStorageProperties.setRootPath(tempDir.toString());
        fileStorageProperties.setChunkSize(DataSize.ofBytes(0));
        fileStorageProperties.setAllowedExtensions(java.util.List.of("pdf", "txt"));
        fileStorageProperties.setAllowedContentTypes(java.util.List.of("application/pdf", "text/plain"));
        fileStorageProperties.validate();

        TaskModuleProperties taskModuleProperties = new TaskModuleProperties();
        taskModuleProperties.setAttachmentMaxSize(DataSize.ofMegabytes(20));
        taskModuleProperties.validate();

        localFileStorageService = new LocalFileStorageService(
                fileStorageProperties,
                taskModuleProperties,
                storedFileRepository,
                fileUploadSessionRepository,
                currentUserService
        );
    }

    @Test
    void uploadDirectShouldRejectDisallowedExtension() {
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> localFileStorageService.uploadDirect(StoredFilePurpose.MATERIAL_ATTACHMENT, file))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("当前角色无权上传该类型附件");
    }

    @Test
    void uploadDirectShouldRejectDisallowedExtensionForAllowedPurpose() {
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> localFileStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("当前文件扩展名不受支持");
    }

    @Test
    void uploadDirectShouldRejectTaskAttachmentFromFreshman() {
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "task.txt",
                "text/plain",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> localFileStorageService.uploadDirect(StoredFilePurpose.TASK_ATTACHMENT, file))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("当前角色无权上传该类型附件");
    }

    @Test
    void uploadDirectShouldRejectDisallowedContentType() {
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.txt",
                "application/json",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> localFileStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("当前文件类型不受支持");
    }

    @Test
    void uploadDirectShouldRejectUnsafeExtensionEvenWhenExtensionWhitelistIsEmpty() {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        fileStorageProperties.setRootPath(tempDir.toString());
        fileStorageProperties.setChunkSize(DataSize.ofBytes(0));
        fileStorageProperties.setAllowedExtensions(java.util.List.of());
        fileStorageProperties.setAllowedContentTypes(java.util.List.of());
        fileStorageProperties.validate();

        TaskModuleProperties taskModuleProperties = new TaskModuleProperties();
        taskModuleProperties.setAttachmentMaxSize(DataSize.ofMegabytes(20));
        taskModuleProperties.validate();

        LocalFileStorageService permissiveStorageService = new LocalFileStorageService(
                fileStorageProperties,
                taskModuleProperties,
                storedFileRepository,
                fileUploadSessionRepository,
                currentUserService
        );
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.txt/../../../outside",
                "text/plain",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> permissiveStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("当前文件扩展名不受支持");
    }

    @Test
    void uploadDirectShouldRejectBlankOriginalFileName() {
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "   ",
                "text/plain",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> localFileStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("文件名不能为空");
    }

    @Test
    void uploadDirectShouldRejectTooLongOriginalFileNameBeforeWritingDatabaseRecord() {
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        String fileName = "a".repeat(252) + ".txt";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> localFileStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("文件名长度不能超过 255 个字符");
    }


    @Test
    void uploadDirectShouldAcceptMarkdownWithDeclaredMarkdownContentType() {
        localFileStorageService = createStorageService(
                java.util.List.of("pdf", "txt", "md", "markdown"),
                java.util.List.of("application/pdf", "text/plain", "text/markdown", "text/x-markdown")
        );
        stubSuccessfulSave();
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.md",
                "text/markdown",
                "# hello".getBytes()
        );

        StoredFileVo storedFile = localFileStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file);

        assertThat(storedFile.originalFileName()).isEqualTo("notes.md");
        assertThat(storedFile.contentType()).isEqualTo("text/markdown");
    }

    @Test
    void uploadDirectShouldAcceptMarkdownWhenBrowserSendsGenericOctetStream() {
        localFileStorageService = createStorageService(
                java.util.List.of("pdf", "txt", "md"),
                java.util.List.of("application/pdf", "text/plain", "text/markdown")
        );
        stubSuccessfulSave();
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "guide.md",
                "application/octet-stream",
                "# guide".getBytes()
        );

        StoredFileVo storedFile = localFileStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file);

        assertThat(storedFile.originalFileName()).isEqualTo("guide.md");
    }

    @Test
    void uploadDirectShouldAcceptMarkdownWhenWhitelistIsBoundAsSingleCommaSeparatedValue() {
        localFileStorageService = createStorageService(
                java.util.List.of("pdf,doc,docx,txt,md,markdown"),
                java.util.List.of("application/pdf,text/plain,text/markdown;charset=UTF-8,text/x-markdown")
        );
        stubSuccessfulSave();
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "readme.markdown",
                "text/markdown; charset=UTF-8",
                "# readme".getBytes()
        );

        StoredFileVo storedFile = localFileStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file);

        assertThat(storedFile.originalFileName()).isEqualTo("readme.markdown");
        assertThat(storedFile.contentType()).isEqualTo("text/markdown");
    }

    @Test
    void uploadDirectShouldAcceptMarkdownWhenBrowserSendsUnlistedContentType() {
        localFileStorageService = createStorageService(
                java.util.List.of("pdf", "txt", "md"),
                java.util.List.of("application/pdf", "text/plain", "text/markdown")
        );
        stubSuccessfulSave();
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.md",
                "application/json",
                "# hello".getBytes()
        );

        StoredFileVo storedFile = localFileStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file);

        assertThat(storedFile.originalFileName()).isEqualTo("notes.md");
    }

    @Test
    void uploadDirectShouldAcceptMarkdownEvenWhenConfigOmitsMarkdownExtension() {
        localFileStorageService = createStorageService(
                java.util.List.of("pdf", "txt"),
                java.util.List.of("application/pdf", "text/plain")
        );
        stubSuccessfulSave();
        when(currentUserService.requireCurrentUser()).thenReturn(buildLoginUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "group-notes.md",
                "text/x-web-markdown",
                "# group notes".getBytes()
        );

        StoredFileVo storedFile = localFileStorageService.uploadDirect(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT, file);

        assertThat(storedFile.originalFileName()).isEqualTo("group-notes.md");
    }

    @Test
    void uploadDirectShouldAcceptGroupMaterialMarkdownFromLeader() {
        localFileStorageService = createStorageService(
                java.util.List.of("pdf", "txt"),
                java.util.List.of("application/pdf", "text/plain")
        );
        stubSuccessfulSave();
        when(currentUserService.requireCurrentUser()).thenReturn(buildLeaderUser());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "group-material.md",
                "application/octet-stream",
                "# material".getBytes()
        );

        StoredFileVo storedFile = localFileStorageService.uploadDirect(StoredFilePurpose.MATERIAL_ATTACHMENT, file);

        assertThat(storedFile.originalFileName()).isEqualTo("group-material.md");
    }

    private LocalFileStorageService createStorageService(
            java.util.List<String> allowedExtensions,
            java.util.List<String> allowedContentTypes
    ) {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        fileStorageProperties.setRootPath(tempDir.toString());
        fileStorageProperties.setChunkSize(DataSize.ofBytes(0));
        fileStorageProperties.setAllowedExtensions(allowedExtensions);
        fileStorageProperties.setAllowedContentTypes(allowedContentTypes);
        fileStorageProperties.validate();

        TaskModuleProperties taskModuleProperties = new TaskModuleProperties();
        taskModuleProperties.setAttachmentMaxSize(DataSize.ofMegabytes(20));
        taskModuleProperties.validate();

        return new LocalFileStorageService(
                fileStorageProperties,
                taskModuleProperties,
                storedFileRepository,
                fileUploadSessionRepository,
                currentUserService
        );
    }

    private void stubSuccessfulSave() {
        when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile storedFile = invocation.getArgument(0);
            storedFile.setId(11L);
            return storedFile;
        });
    }

    private LoginUser buildLoginUser() {
        return new LoginUser(
                1L,
                "freshman",
                "freshman@example.com",
                "hashed",
                Role.FRESHMAN,
                UserStatus.ACTIVE,
                0L,
                "jti-1"
        );
    }

    private LoginUser buildLeaderUser() {
        return new LoginUser(
                2L,
                "leader",
                "leader@example.com",
                "hashed",
                Role.LEADER,
                UserStatus.ACTIVE,
                0L,
                "jti-2"
        );
    }
}
