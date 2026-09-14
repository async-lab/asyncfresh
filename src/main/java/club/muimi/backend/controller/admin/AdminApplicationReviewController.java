package club.muimi.backend.controller.admin;

import club.muimi.backend.common.api.ApiResponse;
import club.muimi.backend.common.api.PageResult;
import club.muimi.backend.common.enums.ApplicationStatus;
import club.muimi.backend.common.enums.Grade;
import club.muimi.backend.dto.admin.RejectApplicationRequest;
import club.muimi.backend.service.admin.AdminApplicationService;
import club.muimi.backend.service.application.ApplicationReviewService;
import club.muimi.backend.vo.admin.AdminApplicationVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApplicationReviewController {

    private final ApplicationReviewService applicationReviewService;
    private final AdminApplicationService adminApplicationService;

    public AdminApplicationReviewController(
            ApplicationReviewService applicationReviewService,
            AdminApplicationService adminApplicationService
    ) {
        this.applicationReviewService = applicationReviewService;
        this.adminApplicationService = adminApplicationService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminApplicationVo>> listApplications(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于 1")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于 1")
            @Max(value = 50, message = "每页条数不能超过 50")
            int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long directionLevel1Id,
            @RequestParam(required = false) Long directionLevel2Id,
            @RequestParam(required = false) Grade grade,
            @RequestParam(required = false) Integer admissionYear
    ) {
        return ApiResponse.success(
                adminApplicationService.listApplications(
                        page,
                        size,
                        keyword,
                        status,
                        directionLevel1Id,
                        directionLevel2Id,
                        grade,
                        admissionYear
                ),
                "ok"
        );
    }

    @PostMapping("/{applicationId}/reject")
    public ApiResponse<Void> rejectApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody RejectApplicationRequest request
    ) {
        applicationReviewService.rejectApplication(applicationId, request);
        return ApiResponse.success(null, "报名申请已拒绝");
    }
}
