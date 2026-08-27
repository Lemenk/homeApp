package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.common.BizException;
import com.familyhome.dto.FamilyVO;
import com.familyhome.dto.MemberVO;
import com.familyhome.entity.Family;
import com.familyhome.entity.FamilyMember;
import com.familyhome.entity.User;
import com.familyhome.mapper.FamilyMapper;
import com.familyhome.mapper.FamilyMemberMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FamilyService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final FamilyMapper familyMapper;
    private final FamilyMemberMapper familyMemberMapper;
    private final UserService userService;
    private final LedgerService ledgerService;

    public FamilyService(FamilyMapper familyMapper, FamilyMemberMapper familyMemberMapper,
                         UserService userService, @Lazy LedgerService ledgerService) {
        this.familyMapper = familyMapper;
        this.familyMemberMapper = familyMemberMapper;
        this.userService = userService;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public FamilyVO createFamily(Long userId, String name) {
        if (findMyFamilyId(userId) != null) {
            throw BizException.conflict("您已在家庭中");
        }
        Family family = new Family();
        family.setName(name);
        family.setCreatorId(userId);
        family.setInviteCode(generateUniqueCode());
        family.setCreatedAt(LocalDateTime.now());
        familyMapper.insert(family);
        insertMember(family.getId(), userId, "creator");
        return toVO(family, userId);
    }

    public Long findMyFamilyId(Long userId) {
        FamilyMember fm = familyMemberMapper.selectOne(
            Wrappers.<FamilyMember>lambdaQuery().eq(FamilyMember::getUserId, userId).last("limit 1"));
        return fm == null ? null : fm.getFamilyId();
    }

    public FamilyVO myFamily(Long userId) {
        Long familyId = findMyFamilyId(userId);
        if (familyId == null) {
            return null;
        }
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            return null;
        }
        return toVO(family, userId);
    }

    public List<MemberVO> members(Long userId, Long familyId) {
        requireMember(familyId, userId);
        List<FamilyMember> fms = familyMemberMapper.selectList(
            Wrappers.<FamilyMember>lambdaQuery()
                .eq(FamilyMember::getFamilyId, familyId)
                .orderByAsc(FamilyMember::getJoinedAt));
        return fms.stream().map(fm -> {
            User u = userService.getById(fm.getUserId());
            return new MemberVO(u.getId(), u.getNickname(), u.getAvatar(), fm.getRole(), fm.getJoinedAt());
        }).toList();
    }

    public String refreshInviteCode(Long userId, Long familyId) {
        requireCreator(familyId, userId);
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw BizException.notFound("家庭不存在");
        }
        family.setInviteCode(generateUniqueCode());
        familyMapper.updateById(family);
        return family.getInviteCode();
    }

    @Transactional
    public FamilyVO joinByCode(Long userId, String code) {
        if (findMyFamilyId(userId) != null) {
            throw BizException.conflict("您已在家庭中");
        }
        Family family = familyMapper.selectOne(
            Wrappers.<Family>lambdaQuery().eq(Family::getInviteCode, code.trim().toUpperCase()));
        if (family == null) {
            throw BizException.notFound("邀请码无效");
        }
        insertMember(family.getId(), userId, "member");
        // 新成员自动加入该家庭的全部公共账本
        ledgerService.addFamilyMemberToPublicLedgers(family.getId(), userId);
        return toVO(family, userId);
    }

    public void requireMember(Long familyId, Long userId) {
        Long count = familyMemberMapper.selectCount(
            Wrappers.<FamilyMember>lambdaQuery()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, userId));
        if (count == null || count == 0) {
            throw BizException.forbidden("您不是该家庭成员");
        }
    }

    public void requireCreator(Long familyId, Long userId) {
        FamilyMember fm = familyMemberMapper.selectOne(
            Wrappers.<FamilyMember>lambdaQuery()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, userId)
                .last("limit 1"));
        if (fm == null) {
            throw BizException.forbidden("您不是该家庭成员");
        }
        if (!"creator".equals(fm.getRole())) {
            throw BizException.forbidden("仅创建者可执行此操作");
        }
    }

    private void insertMember(Long familyId, Long userId, String role) {
        FamilyMember fm = new FamilyMember();
        fm.setFamilyId(familyId);
        fm.setUserId(userId);
        fm.setRole(role);
        fm.setJoinedAt(LocalDateTime.now());
        familyMemberMapper.insert(fm);
    }

    private String generateUniqueCode() {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            Long count = familyMapper.selectCount(
                Wrappers.<Family>lambdaQuery().eq(Family::getInviteCode, code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw BizException.server("邀请码生成失败，请重试");
    }

    private FamilyVO toVO(Family family, Long userId) {
        FamilyMember self = familyMemberMapper.selectOne(
            Wrappers.<FamilyMember>lambdaQuery()
                .eq(FamilyMember::getFamilyId, family.getId())
                .eq(FamilyMember::getUserId, userId)
                .last("limit 1"));
        return new FamilyVO(family.getId(), family.getName(), family.getCreatorId(),
            family.getInviteCode(), self == null ? "member" : self.getRole(), members(userId, family.getId()));
    }
}
