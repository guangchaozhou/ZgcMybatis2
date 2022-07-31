package com.zgc.mybatis;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public class MapperProxyFactory {

    private static Map<Class, TypeHandler> typeHandlerMap = new HashMap<>();

    static {
        typeHandlerMap.put(String.class, new StringTypeHandler());
        typeHandlerMap.put(Integer.class, new IntegerTypeHandler());

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> T getMapper(Class<T> mapper) {
        return (T) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{mapper}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                // 结果支持List或单个User对象
                Object result = null;

                // 参数名：参数值
                // 相当于 变量名：变量值，替换到sql中
                Map<String, Object> paramValueMapping = new HashMap<>();
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    paramValueMapping.put(parameter.getName(), args[i]);
                    paramValueMapping.put(parameter.getAnnotation(Param.class).value(), args[i]);
                }

                String sql = method.getAnnotation(Select.class).value();

                // #{}--->?
               ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler();
                GenericTokenParser parser = new GenericTokenParser("#{", "}", tokenHandler);
                String parseSql = parser.parse(sql);

                // 创建数据库连接
                Connection connection = getConnection();

                PreparedStatement statement = connection.prepareStatement(parseSql);

                for (int i = 0; i < tokenHandler.getParameterMappings().size(); i++) {
                    // sql中的#{}变量名
                    String property = tokenHandler.getParameterMappings().get(i).getProperty();
                    Object value = paramValueMapping.get(property); // 变量值
                   TypeHandler typeHandler = typeHandlerMap.get(value.getClass()); // 根据值类型找TypeHandler
                    typeHandler.setParameter(statement, i + 1, value);
                }

                // 3、执行PreparedStatement
                statement.execute();

                // 4、封装结果
                List<Object> list = new ArrayList<>();
                ResultSet resultSet = statement.getResultSet();

                Class resultType = null;
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof Class) {
                    // 不是泛型
                    resultType = (Class) genericReturnType;
                } else if (genericReturnType instanceof ParameterizedType) {
                    // 是泛型
                    Type[] actualTypeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
                    resultType = (Class) actualTypeArguments[0];
                }

                // 根据setter方法记录 属性名：Method对象
                Map<String, Method> setterMethodMapping = new HashMap<>();
                for (Method declaredMethod : resultType.getDeclaredMethods()) {
                    if (declaredMethod.getName().startsWith("set")) {
                        String propertyName = declaredMethod.getName().substring(3);
                        propertyName = propertyName.substring(0, 1).toLowerCase(Locale.ROOT) + propertyName.substring(1);
                        setterMethodMapping.put(propertyName, declaredMethod);
                    }
                }

                // 记录sql返回的所有字段名
                ResultSetMetaData metaData = resultSet.getMetaData();
                List<String> columnList = new ArrayList<>();
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    columnList.add(metaData.getColumnName(i + 1));
                }

                while (resultSet.next()) {
                    Object instance = resultType.newInstance();

                    for (String column : columnList) {
                        Method setterMethod = setterMethodMapping.get(column);
                        // 根据setter方法参数类型找到TypeHandler
                       TypeHandler typeHandler = typeHandlerMap.get(setterMethod.getParameterTypes()[0]);
                        setterMethod.invoke(instance, typeHandler.getResult(resultSet, column));
                    }

                    list.add(instance);
                }

                // 5、关闭数据库连接
                connection.close();

                if (method.getReturnType().equals(List.class)) {
                    result = list;
                } else {
                    result = list.get(0);
                }

                return result;
            }
        });
    }

    private static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/tuling?characterEncoding=utf-8&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai",
                "root", "Zhouyu123456***");
        return connection;
    }
}
