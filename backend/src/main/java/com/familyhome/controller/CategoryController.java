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

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/ledgers/{ledgerId}/categories")
    public Result<List<Category>> list(@PathVariable Long ledgerId) {
        return Result.ok(categoryService.list(UserContext.require(), ledgerId));
    }

    @PostMapping("/ledgers/{ledgerId}/categories")
    public Result<Category> create(@PathVariable Long ledgerId,
                                   @Valid @RequestBody CategoryRequest req) {
        return Result.ok(categoryService.create(UserContext.require(), ledgerId, req));
    }

    @PutMapping("/categories/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return Result.ok(categoryService.update(UserContext.require(), id, req));
    }

    @PutMapping("/categories/{id}/toggle")
    public Result<Category> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        return Result.ok(categoryService.toggle(UserContext.require(), id, enabled));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(UserContext.require(), id);
        return Result.ok();
    }
}
