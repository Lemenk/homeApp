package com.familyhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyhome.entity.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
