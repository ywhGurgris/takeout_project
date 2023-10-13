package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {


    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getUserByOpenid(String openid);


    /**
     * 插入用户
     * @param user
     */
    void insertUser(User user);

    /**
     * 用户数查询
     * @param map
     * @return
     */
    Integer getByMap(Map map);
}
