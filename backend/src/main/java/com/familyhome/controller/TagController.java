package com.familyhome.controller;

import com.familyhome.common.Result;
import com.familyhome.dto.TagRequest;
import com.familyhome.entity.Tag;
import com.familyhome.security.UserContext;
import com.familyhome.service.TagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 标签接口：标签列表、新增、更新、删除 */
@RestController
@RequestMapping("/api")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /** 查询账本下的标签列表 */
    @GetMapping("/ledgers/{ledgerId}/tags")
    public Result<List<Tag>> list(@PathVariable Long ledgerId) {
        return Result.ok(tagService.list(UserContext.require(), ledgerId));
    }

    /** 新增标签 */
    @PostMapping("/ledgers/{ledgerId}/tags")
    public Result<Tag> create(@PathVariable Long ledgerId, @Valid @RequestBody TagRequest req) {
        return Result.ok(tagService.create(UserContext.require(), ledgerId, req));
    }

    /** 更新标签（名称/颜色） */
    @PutMapping("/tags/{id}")
    public Result<Tag> update(@PathVariable Long id, @Valid @RequestBody TagRequest req) {
        return Result.ok(tagService.update(UserContext.require(), id, req));
    }

    /** 删除标签 */
    @DeleteMapping("/tags/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(UserContext.require(), id);
        return Result.ok();
    }
}
