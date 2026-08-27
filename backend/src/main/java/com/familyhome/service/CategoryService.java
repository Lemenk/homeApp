package com.familyhome.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.familyhome.common.BizException;
import com.familyhome.dto.CategoryRequest;
import com.familyhome.entity.Category;
import com.familyhome.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final LedgerService ledgerService;

    public CategoryService(CategoryMapper categoryMapper, LedgerService ledgerService) {
        this.categoryMapper = categoryMapper;
        this.ledgerService = ledgerService;
    }

    public List<Category> list(Long userId, Long ledgerId) {
        ledgerService.requireMember(ledgerId, userId);
        return categoryMapper.selectList(
            Wrappers.<Category>lambdaQuery()
                .eq(Category::getLedgerId, ledgerId)
                .orderByAsc(Category::getSort, Category::getId));
    }

    public Category create(Long userId, Long ledgerId, CategoryRequest req) {
        ledgerService.requireCreator(ledgerId, userId);
        Category cat = new Category();
        cat.setLedgerId(ledgerId);
        cat.setType(req.getType());
        cat.setName(req.getName());
        cat.setIcon(req.getIcon());
        cat.setSort(req.getSort() == null ? 0 : req.getSort());
        cat.setEnabled(1);
        cat.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(cat);
        return cat;
    }

    public Category update(Long userId, Long categoryId, CategoryRequest req) {
        Category cat = getOwned(categoryId);
        ledgerService.requireCreator(cat.getLedgerId(), userId);
        cat.setName(req.getName());
        cat.setIcon(req.getIcon());
        if (req.getSort() != null) {
            cat.setSort(req.getSort());
        }
        categoryMapper.updateById(cat);
        return cat;
    }

    /** 停用/启用 */
    public Category toggle(Long userId, Long categoryId, Boolean enabled) {
        Category cat = getOwned(categoryId);
        ledgerService.requireCreator(cat.getLedgerId(), userId);
        cat.setEnabled(enabled == null || enabled ? 1 : 0);
        categoryMapper.updateById(cat);
        return cat;
    }

    public void delete(Long userId, Long categoryId) {
        Category cat = getOwned(categoryId);
        ledgerService.requireCreator(cat.getLedgerId(), userId);
        cat.setEnabled(0);
        categoryMapper.updateById(cat);
    }

    private Category getOwned(Long categoryId) {
        Category cat = categoryMapper.selectById(categoryId);
        if (cat == null) {
            throw BizException.notFound("分类不存在");
        }
        return cat;
    }
}
