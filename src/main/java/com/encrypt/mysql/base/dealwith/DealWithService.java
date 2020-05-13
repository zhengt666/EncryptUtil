package com.encrypt.mysql.base.dealwith;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.encrypt.mysql.base.annotation.EncryptClass;
import com.encrypt.mysql.base.annotation.EncryptField;
import com.encrypt.mysql.base.annotation.EncryptMethod;
import com.encrypt.mysql.base.encrypt.EncryptDecrypt;
import com.encrypt.mysql.base.utils.CmUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @Description: aop处理加解密
 * @Author: zhengt
 * @CreateDate: 2020/5/13 10:56
 * @UpdateUser: zhengt
 * @UpdateDate: 2020/5/13 10:56
 * @UpdateRemark: 修改内容
 * @Version: 1.0
 */
public class DealWithService {

    /**
     * 单例
     */
    private static volatile DealWithService dealWithService;

    /**
     * 还没有实例化过
     */
    private static boolean flag = true;

    /**
     * 构造
     */
    private DealWithService() {
        synchronized (DealWithService.class) {
            if (flag) {
                flag = false;
            } else {
                throw new RuntimeException("不要试图通过反射破环单例");
            }
        }
    }

    /**
     * 单例获取
     *
     * @return
     */
    public static DealWithService getInstance() {
        if (ObjectUtils.isEmpty(dealWithService)) {
            synchronized (DealWithService.class) {
                if (ObjectUtils.isEmpty(dealWithService)) {
                    return new DealWithService();
                }
            }
        }
        return dealWithService;
    }

    /**
     * 设置id
     *
     * @param source 来源
     * @param result 设置结果
     * @return
     * @throws
     * @author zhengt
     * @date 2019/11/8 10:54
     */
    public void setObjectId(Object source, Object result) throws Exception {
        Class clazz = result.getClass();
        Annotation classAnnotation = clazz.getAnnotation(Entity.class);
        if (classAnnotation != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                Annotation fieldAnnotation = field.getAnnotation(Id.class);
                if (fieldAnnotation != null) {
                    field.setAccessible(true);
                    field.set(result, field.get(source));
                }
            }
        }
    }

    /**
     * 替换参数和结果，加解密不影响程序
     *
     * @param result
     * @return
     * @throws
     * @author zhengt
     * @date 2019/11/8 10:51
     */
    public Object changeObject(Object result) throws Exception {
        Object responseObj = null;
        if (CmUtil.hv(result)) {
            if (result instanceof Integer) {
                responseObj = result;
            } else if (result instanceof Double) {
                responseObj = result;
            } else if (result instanceof String) {
                responseObj = "" + result.toString();
            } else if (result instanceof List) {
                responseObj = new ArrayList<>();
                List old = (List) result;
                ((ArrayList) responseObj).addAll(old);
            } else {
                responseObj = result.getClass().newInstance();
                BeanUtils.copyProperties(result, responseObj);
            }
        }
        return responseObj;
    }

    /**
     * 处理加密
     *
     * @param method
     */
    public Object[] handleEncrypt(Method method, Object[] o, String secretKey, String strategy) throws IllegalAccessException {
        if (Objects.isNull(o)) {
            return null;
        }
        //仅处理特殊授值（map,string,list）
        //获取带有注解得参数
        Map<Integer, String[]> map = getMethodParameterNamesByAnnotation(method);

        if (CmUtil.hv(map)) {
            if (CmUtil.hv(map.size())) {
                //遍历参数，对应类型，做对应处理
                Set<Integer> set = map.keySet();
                for (Integer integer : set) {
                    Object object = o[integer];
                    if (object instanceof String) {
                        o[integer] = EncryptDecrypt.encryptString(object, secretKey, strategy);
                    } else if (object instanceof Map) {
                        String[] encrypt = map.get(integer);
                        o[integer] = EncryptDecrypt.encryptMap(object, encrypt, secretKey, strategy);
                    } else if (object instanceof List) {
                        o[integer] = EncryptDecrypt.encryptList(object, secretKey, strategy);
                    }
                }
            }
        }
        //处理vo,domain
        Object[] objs = new Object[o.length];
        for (int i = 0; i < o.length; i++) {
            Class<?> parameterObjectClass = o[i].getClass();
            EncryptClass encryptDecryptClass = AnnotationUtils.findAnnotation(parameterObjectClass, EncryptClass.class);
            if (Objects.nonNull(encryptDecryptClass)) {
                Field[] declaredFields = parameterObjectClass.getDeclaredFields();
                final Object encrypt = EncryptDecrypt.encrypt(declaredFields, o[i], secretKey, strategy);
                objs[i] = encrypt;
            } else {
                objs[i] = o[i];
            }
        }
        return objs;
    }

    /**
     * 处理解密
     *
     * @param responseObj
     */
    public Object handleDecrypt(Object responseObj, Method method, String secretKey, String strategy) throws IllegalAccessException {
        if (Objects.isNull(responseObj)) {
            return null;
        }
        EncryptMethod annotation = method.getAnnotation(EncryptMethod.class);
        if (Objects.nonNull(annotation)) {
            if (responseObj instanceof String) {
                responseObj = EncryptDecrypt.decryptString(responseObj, secretKey, strategy);
            } else if (responseObj instanceof Map) {
                responseObj = EncryptDecrypt.decryptMap(responseObj, annotation.decrypts(), secretKey, strategy);
            } else if (responseObj instanceof List) {
                responseObj = EncryptDecrypt.decryptList(responseObj, secretKey, strategy);
            }
        }
        if (responseObj instanceof ArrayList) {
            ArrayList resultList = (ArrayList) responseObj;
            decryptToList(resultList, secretKey, strategy);
        } else if (responseObj instanceof Page) {
            Page page = (Page) responseObj;
            List resultList = page.getRecords();
            decryptToList(resultList, secretKey, strategy);
        } else {
            if (needToDecrypt(responseObj)) {
                EncryptDecrypt.decrypt(responseObj, secretKey, strategy);
            }
        }
        return responseObj;
    }

    /**
     * 处理解密List
     *
     * @param resultList
     * @throws IllegalAccessException
     */
    public void decryptToList(List resultList, String strKey, String strategy) throws IllegalAccessException {
        if (CmUtil.hv(resultList) && needToDecrypt(resultList.get(0))) {
            for (int i = 0; i < resultList.size(); i++) {
                EncryptDecrypt.decrypt(resultList.get(i), strKey, strategy);
            }
        }
    }

    /**
     * 处理是否需要加解密实体
     *
     * @param object
     * @return
     */
    public boolean needToDecrypt(Object object) {
        Class<?> objectClass = object.getClass();
        EncryptClass encryptDecryptClass = AnnotationUtils.findAnnotation(objectClass, EncryptClass.class);
        if (Objects.nonNull(encryptDecryptClass)) {
            return true;
        }
        return false;

    }

    /**
     * 获取给 "方法参数" 进行注解的值
     *
     * @param method 要获取参数名的方法
     * @return 按参数顺序排列的参数名列表
     */
    public static Map<Integer, String[]> getMethodParameterNamesByAnnotation(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations == null || parameterAnnotations.length == 0) {
            return null;
        }
        Map<Integer, String[]> map = new HashMap<>();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (annotation instanceof EncryptField) {
                    EncryptField param = (EncryptField) annotation;
                    map.put(i, param.encrypts());
                }
            }
        }
        return map;
    }

}
