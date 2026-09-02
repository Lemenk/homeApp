package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.common.BizException;
import com.familyhome.dto.TagRequest;
import com.familyhome.entity.Tag;
import com.familyhome.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 标签服务：标签的增删改查（仅账本创建者可管理） */
@Service
public class TagService {

    private final TagMapper tagMapper;
    private final LedgerService ledgerService;

    public TagService(TagMapper tagMapper, LedgerService ledgerService) {
        this.tagMapper = tagMapper;
        this.ledgerService = ledgerService;
    }

    /** 查询账本标签列表 */
    public List<Tag> list(Long userId, Long ledgerId) {
        ledgerService.requireMember(ledgerId, userId);
        return tagMapper.selectList(
            Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getLedgerId, ledgerId)
                .orderByAsc(Tag::getId));
    }

    /** 新增标签 */
    public Tag create(Long userId, Long ledgerId, TagRequest req) {
        ledgerService.requireCreator(ledgerId, userId);
        Tag tag = new Tag();
        tag.setLedgerId(ledgerId);
        tag.setName(req.getName());
        tag.setColor(req.getColor());
        tag.setCreatedBy(userId);
        tag.setCreatedAt(LocalDateTime.now());
        tagMapper.insert(tag);
        return tag;
    }

    /** 更新标签（名称/颜色） */
    public Tag update(Long userId, Long tagId, TagRequest req) {
        Tag tag = getOwned(tagId);
        ledgerService.requireCreator(tag.getLedgerId(), userId);
        tag.setName(req.getName());
        tag.setColor(req.getColor());
        tagMapper.updateById(tag);
        return tag;
    }

    /** 删除标签 */
    public void delete(Long userId, Long tagId) {
        Tag tag = getOwned(tagId);
        ledgerService.requireCreator(tag.getLedgerId(), userId);
        tagMapper.deleteById(tagId);
    }

    private Tag getOwned(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw BizException.notFound("标签不存在");
        }
        return tag;
    }
}
