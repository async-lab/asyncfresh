package club.muimi.backend.controller.admin;

import club.muimi.backend.common.api.ApiResponse;
import club.muimi.backend.service.dashboard.DashboardService;
import club.muimi.backend.vo.dashboard.DashboardOverviewVo;
import club.muimi.backend.vo.dashboard.GroupDashboardDetailVo;
import club.muimi.backend.vo.dashboard.GroupDashboardSummaryVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVo> getOverview() {
        return ApiResponse.success(dashboardService.getAdminOverview(), "ok");
    }

    @GetMapping("/groups")
    public ApiResponse<List<GroupDashboardSummaryVo>> listGroups() {
        return ApiResponse.success(dashboardService.listManageableGroupSummaries(), "ok");
    }

    @GetMapping("/groups/{groupId}")
    @PreAuthorize("@authzService.canManageGroup(authentication, #groupId)")
    public ApiResponse<GroupDashboardDetailVo> getGroupDetail(@PathVariable Long groupId) {
        return ApiResponse.success(dashboardService.getManageableGroupDetail(groupId), "ok");
    }
}
