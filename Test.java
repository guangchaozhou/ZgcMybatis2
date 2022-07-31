package com.zgc.test;


import com.zgc.mybatis.MapperProxyFactory;

import java.util.List;

public class Test {

    public static void main(String[] args) {

        UserMapper mapper = MapperProxyFactory.getMapper(UserMapper.class);
        List<User> zhouyu = mapper.getUser("zhouyu");
        System.out.println(zhouyu);

        User user = mapper.getUserById(1);
        System.out.println(user);

    }
}
