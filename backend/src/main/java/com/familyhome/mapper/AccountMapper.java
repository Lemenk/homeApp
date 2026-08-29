package com.familyhome.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familyhome.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /**
     * 原子余额增减：balance = balance + delta，避免并发"读-改-写"丢失更新。
     */
    @Update("UPDATE t_account SET balance = balance + #{delta} WHERE id = #{id}")
    int addBalance(@Param("id") Long id, @Param("delta") BigDecimal delta);
}
