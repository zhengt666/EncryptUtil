package com.encrypt.mysql.base.encrypt;

import com.encrypt.mysql.base.algorithm.EncrypyContext;
import com.encrypt.mysql.base.annotation.EncryptField;
import com.encrypt.mysql.base.utils.CmUtil;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @Description: 加解密
 * @Author: zhengt
 * @CreateDate: 2019/10/21 10:15
 * @UpdateUser: zhengt
 * @UpdateDate: 2019/10/21 10:15
 * @UpdateRemark: 修改内容
 * @Version: 1.0
 */
public class EncryptDecrypt {

    private static EncrypyContext encrypyContext = EncrypyContext.getInstance();

    /**
     * 多field加密方法
     *
     * @param declaredFields
     * @param parameterObject
     * @param <T>
     * @return
     * @throws IllegalAccessException
     */

    public static <T> T encrypt(Field[] declaredFields, T parameterObject, String strategy) throws IllegalAccessException {
        for (Field field : declaredFields) {
            EncryptField annotation = field.getAnnotation(EncryptField.class);
            if (Objects.isNull(annotation)) {
                continue;
            }
            encrypt(field, parameterObject, annotation.value(), strategy);
        }
        return parameterObject;

    }

    /**
     * 单个field加密方法
     *
     * @param field
     * @param parameterObject
     * @param <T>
     * @return
     * @throws IllegalAccessException
     */
    private static <T> T encrypt(Field field, T parameterObject, String strKey, String strategy) throws IllegalAccessException {
        field.setAccessible(true);
        Object object = field.get(parameterObject);
        if (object instanceof String) {
            String value = (String) object;
            value = encrypyContext.encrypt(value, strKey, strategy);
            field.set(parameterObject, value);
        }
        return parameterObject;

    }

    /**
     * 解密方法
     *
     * @param result
     * @param <T>
     * @return
     * @throws IllegalAccessException
     */

    public static <T> T decrypt(T result, String stategy) throws IllegalAccessException {
        Class<?> parameterObjectClass = result.getClass();
        Field[] declaredFields = parameterObjectClass.getDeclaredFields();
        decrypt(declaredFields, result, stategy);
        return result;
    }

    /**
     * 多个field解密方法
     *
     * @param declaredFields
     * @param result
     * @throws IllegalAccessException
     */
    public static void decrypt(Field[] declaredFields, Object result, String stategy) throws IllegalAccessException {
        for (Field field : declaredFields) {
            EncryptField annotation = field.getAnnotation(EncryptField.class);
            if (Objects.isNull(annotation)) {
                continue;
            }
            decrypt(field, result, annotation.value(), stategy);
        }
    }

    /**
     * 单个field解密方法
     *
     * @param field
     * @param result
     * @throws IllegalAccessException
     */

    public static void decrypt(Field field, Object result, String strKey, String stategy) throws IllegalAccessException {
        field.setAccessible(true);
        Object object = field.get(result);
        if (object instanceof String) {
            String value = (String) object;
            value = encrypyContext.decrypt(value, strKey, stategy);
            field.set(result, value);
        }
    }

    /**
     * 加密字符串
     *
     * @param o
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/23 14:11
     */
    public static Object encryptString(Object o, String secretKey, String stategy) {
        String plaintextValue = (String) o;
        String encryptValue = encrypyContext.encrypt(plaintextValue, secretKey, stategy);
        return encryptValue;
    }

    /**
     * 加密map集合
     *
     * @param o
     * @param encryptMap
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/23 18:00
     */
    public static Object encryptMap(Object o, String[] encryptMap, String secretKey, String stategy) {
        Map<String, Object> plaintextValue = (Map<String, Object>) o;
        Set<String> setKey = plaintextValue.keySet();
        List<String> list = Arrays.asList(encryptMap);
        for (String object : setKey) {
            if (list.contains(object)) {
                Object result = plaintextValue.get(object);
                if (result instanceof String) {
                    result = encryptString(result, secretKey, stategy);
                } else if (result instanceof List) {
                    result = encryptList(result, secretKey, stategy);
                }
                plaintextValue.put(object, result);
            }
        }
        return plaintextValue;
    }

    /**
     * 加密list
     *
     * @param o
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/23 19:29
     */
    public static Object encryptList(Object o, String secretKey, String stategy) {
        List<Object> list = (List<Object>) o;
        for (Object str : list) {
            encryptString(str, secretKey, stategy);
        }
        return list;
    }

    /**
     * 对String解密
     *
     * @param o
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/23 19:45
     */
    public static Object decryptString(Object o, String secretKey, String stategy) {
        String string = (String) o;
        return encrypyContext.decrypt(string, secretKey, stategy);
    }

    /**
     * 解密list
     *
     * @param o
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/23 19:48
     */
    public static Object decryptList(Object o, String secretKey, String stategy) {
        List<Object> list = (List) o;
        if (CmUtil.hv(list)) {
            for (Object string : list) {
                decryptString(string, secretKey, stategy);
            }
        }
        return list;
    }

    /**
     * 对Map解密
     *
     * @param o
     * @param decryptMap
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/23 19:52
     */
    public static Object decryptMap(Object o, String[] decryptMap, String secretKey, String stategy) {
        Map<String, Object> map = (Map) o;
        List<String> list = Arrays.asList(decryptMap);
        Set<String> setKey = map.keySet();
        for (String str : setKey) {
            if (list.contains(str)) {
                Object result = map.get(str);
                if (result instanceof String) {
                    result = decryptString(result, secretKey, stategy);
                } else if (result instanceof List) {
                    result = decryptList(result, secretKey, stategy);
                }
                map.put(str, result);
            }
        }
        return map;
    }
}
