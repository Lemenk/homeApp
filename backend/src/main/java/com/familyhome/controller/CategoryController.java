package com.familyhome.controller;

import com.familyhome.common.Result;
import com.familyhome.dto.CategoryRequest;
import com.familyhome.entity.Category;
import com.familyhome.security.UserContext;
import com.familyhome.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 分类接口：支出/收入分类的列表、新增、更新、启用/停用、删除 */
@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 查询账本下的分类列表 */
    @GetMapping("/ledgers/{ledgerId}/categories")
    public Result<List<Category>> list(@PathVariable Long ledgerId) {
        return Result.ok(categoryService.list(UserContext.require(), ledgerId));
    }

    /** 新增分类 */
    @PostMapping("/ledgers/{ledgerId}/categories")
    public Result<Category> create(@PathVariable Long ledgerId,
                                   @Valid @RequestBody CategoryRequest req) {
        return Result.ok(categoryService.create(UserContext.require(), ledgerId, req));
    }

    /** 更新分类（名称/图标/排序） */
    @PutMapping("/categories/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return Result.ok(categoryService.update(UserContext.require(), id, req));
    }

    /** 启用/停用分类 */
    @PutMapping("/categories/{id}/toggle")
    public Result<Category> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        return Result.ok(categoryService.toggle(UserContext.require(), id, enabled));
    }

    /** 删除分类 */
    @DeleteMapping("/categories/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(UserContext.require(), id);
        return Result.ok();
    }
}
