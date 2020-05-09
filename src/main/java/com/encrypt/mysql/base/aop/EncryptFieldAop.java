package com.encrypt.mysql.base.aop;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.encrypt.mysql.base.annotation.EncryptClass;
import com.encrypt.mysql.base.annotation.EncryptField;
import com.encrypt.mysql.base.annotation.EncryptMethod;
import com.encrypt.mysql.base.encrypt.EncryptDecrypt;
import com.encrypt.mysql.base.utils.CmUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @Description: aop统一处理程序下dao文件夹下
 * @Author: zhengt
 * @CreateDate: 2020/4/20 15:22
 * @UpdateUser: zhengt
 * @UpdateDate: 2020/4/20 15:22
 * @UpdateRemark: 修改内容
 * @Version: 1.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Aspect
public class EncryptFieldAop  {

    /**
     * 加解密盐值
     */
    @Value("${encyptUtil.encrypt.key}")
    private String secretKey;

    /**
     * 加解密处理模式
     */
    @Value("${encyptUtil.encrypt.strategy}")
    private String strategy;

    /**
     * dao切面
     */
    @Pointcut("execution(public * *..*.dao..*.*(..))")
    public void annotationPointCut() {
    }

    /**
     * 环绕处理
     *
     * @param joinPoint
     * @return
     */
    @Around("annotationPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) {

        Object responseObj = null;

        try {
            //获取dao参数
            Object[] requestObjs = joinPoint.getArgs();
            //通过切点获取当前运行得方法，为使节点得
            Signature sig = joinPoint.getSignature();
            MethodSignature msig = null;
            if (!(sig instanceof MethodSignature)) {
                throw new IllegalArgumentException("该注解只能用于方法");
            }
            msig = (MethodSignature) sig;
            Object target = joinPoint.getTarget();

            Class clazz = null;
            Class[] classes = target.getClass().getInterfaces();
            for (Class currentClass : classes) {
                if (currentClass.getName().contains("dao")) {
                    clazz = currentClass;
                    break;
                }
            }
            for (int i = 0; i < requestObjs.length; i++) {
                if (requestObjs[i] instanceof Wrapper) {
                    Method currentMethod = target.getClass().getMethod(msig.getName(), msig.getParameterTypes());
                    responseObj = currentMethod.invoke(target, requestObjs);
                    responseObj = handleDecrypt(responseObj, currentMethod);
                    return responseObj;
                }
            }
            Method realMethod = clazz.getMethod(msig.getName(), msig.getParameterTypes());

            Object[] objs = null;
            //不能排除数组有空值
            if (requestObjs.length > 0) {
                //不操作原对象
                objs = new Object[requestObjs.length];
                //判断是否有参数需要转换，排除null值
                boolean flag = false;
                for (int i = 0; i < requestObjs.length; i++) {
                    if (CmUtil.hv(requestObjs[i])) {
                        flag = true;
                        if (requestObjs[i] instanceof Wrapper) {
                            objs = requestObjs;
                            break;
                        }
                        objs[i] = changeObject(requestObjs[i]);
                    }
                }
                if (flag) {
                    handleEncrypt(realMethod, objs);
                }
            }

            Object result = realMethod.invoke(target, objs);

            //如果是jpa保存，需要对元对象得id进行保存
            if (clazz.getName().contains("repository")) {
                if ("save".equals(realMethod.getName()) || "saveAll".equals(realMethod.getName())) {
                    //id移转
                    for (int i = 0; i < requestObjs.length; i++) {
                        if (requestObjs[i] instanceof List) {
                            //保存集合id
                            List list = (List) requestObjs[i];
                            List source = (List) objs[i];
                            if (CmUtil.hv(list)) {
                                for (int j = 0; j < list.size(); j++) {
                                    setObjectId(source.get(j), list.get(j));
                                }
                            }
                        } else {
                            //保存id
                            setObjectId(objs[i], requestObjs[i]);
                        }
                    }
                }
            }
            //entityManagerFactory.unwrap(SessionFactory.class).getCurrentSession().flush();

            //修改response
            responseObj = changeObject(result);

            responseObj = handleDecrypt(responseObj, realMethod);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseObj;

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
    private Object changeObject(Object result) throws Exception {
        Object responseObj = null;
        if (CmUtil.hv(result)) {
            if (result instanceof Integer) {
                responseObj = result;
            } else if (result instanceof Double) {
                responseObj = result;
            } else if (result instanceof String) {
                responseObj = "" + result.toString();
            } else  if (result instanceof List) {
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
    private Object[] handleEncrypt(Method method, Object[] o) throws IllegalAccessException {
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
                final Object encrypt = EncryptDecrypt.encrypt(declaredFields, o[i], strategy);
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
    private Object handleDecrypt(Object responseObj, Method method) throws IllegalAccessException {
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
            decryptToList(resultList);
        } else if (responseObj instanceof Page) {
            Page page = (Page) responseObj;
            List resultList = page.getRecords();
            decryptToList(resultList);
        } else {
            if (needToDecrypt(responseObj)) {
                EncryptDecrypt.decrypt(responseObj, strategy);
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
    private void decryptToList(List resultList) throws IllegalAccessException {
        if (CmUtil.hv(resultList) && needToDecrypt(resultList.get(0))) {
            for (int i = 0; i < resultList.size(); i++) {
                EncryptDecrypt.decrypt(resultList.get(i), strategy);
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
