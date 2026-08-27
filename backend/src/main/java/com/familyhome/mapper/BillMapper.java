package com.familyhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyhome.entity.Bill;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BillMapper extends BaseMapper<Bill> {
}
