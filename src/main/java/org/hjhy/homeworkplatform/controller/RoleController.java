package org.hjhy.homeworkplatform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hjhy.homeworkplatform.generator.domain.Role;
import org.hjhy.homeworkplatform.generator.service.RoleService;
import org.hjhy.homeworkplatform.vo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author HJHY
 * @date 2024/4/1 16:44
 */
@Tag(name = "角色模块接口")
@RestController
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    //获取角色列表
    @Operation(summary = "获取角色列表", description = "获取角色列表接口描述")
    @GetMapping("/roles")
    public Result<List<Role>> getRoles() {
        return Result.ok(roleService.list());
    }
}
