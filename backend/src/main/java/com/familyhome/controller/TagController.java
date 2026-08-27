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

@RestController
@RequestMapping("/api")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/ledgers/{ledgerId}/tags")
    public Result<List<Tag>> list(@PathVariable Long ledgerId) {
        return Result.ok(tagService.list(UserContext.require(), ledgerId));
    }

    @PostMapping("/ledgers/{ledgerId}/tags")
    public Result<Tag> create(@PathVariable Long ledgerId, @Valid @RequestBody TagRequest req) {
        return Result.ok(tagService.create(UserContext.require(), ledgerId, req));
    }

    @PutMapping("/tags/{id}")
    public Result<Tag> update(@PathVariable Long id, @Valid @RequestBody TagRequest req) {
        return Result.ok(tagService.update(UserContext.require(), id, req));
    }

    @DeleteMapping("/tags/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(UserContext.require(), id);
        return Result.ok();
    }
}
