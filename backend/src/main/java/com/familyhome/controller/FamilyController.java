package com.familyhome.controller;

import com.familyhome.common.Result;
import com.familyhome.dto.CreateFamilyRequest;
import com.familyhome.dto.FamilyVO;
import com.familyhome.dto.JoinFamilyRequest;
import com.familyhome.dto.MemberVO;
import com.familyhome.security.UserContext;
import com.familyhome.service.FamilyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/families")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @PostMapping
    public Result<FamilyVO> create(@Valid @RequestBody CreateFamilyRequest req) {
        return Result.ok(familyService.createFamily(UserContext.require(), req.getName()));
    }

    @GetMapping("/me")
    public Result<FamilyVO> me() {
        return Result.ok(familyService.myFamily(UserContext.require()));
    }

    @GetMapping("/{id}/members")
    public Result<List<MemberVO>> members(@PathVariable Long id) {
        return Result.ok(familyService.members(UserContext.require(), id));
    }

    @PostMapping("/{id}/invite")
    public Result<String> refreshInviteCode(@PathVariable Long id) {
        return Result.ok(familyService.refreshInviteCode(UserContext.require(), id));
    }

    @PostMapping("/join")
    public Result<FamilyVO> join(@Valid @RequestBody JoinFamilyRequest req) {
        return Result.ok(familyService.joinByCode(UserContext.require(), req.getInviteCode()));
    }
}
