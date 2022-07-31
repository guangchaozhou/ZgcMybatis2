package com.zgc.test;



import com.zgc.mybatis.Param;
import com.zgc.mybatis.Select;

import java.util.List;

public interface UserMapper {

    @Select("select * from user where name = #{name}")
    public List<User> getUser(@Param("name") String name);

    @Select("select * from user where id = #{id}")
    public User getUserById(@Param("id") Integer id);
}
