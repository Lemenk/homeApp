package com.familyhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyhome.entity.FamilyMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FamilyMemberMapper extends BaseMapper<FamilyMember> {
}
