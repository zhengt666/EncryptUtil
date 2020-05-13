package com.encrypt.mysql.base.aspect;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.encrypt.mysql.base.annotation.EncryptMethod;
import com.encrypt.mysql.base.dealwith.DealWithService;
import com.encrypt.mysql.base.utils.CmUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

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
public class EncryptFieldAop {

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
     * 加解密处理
     */
    private DealWithService dealWithService = DealWithService.getInstance();

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

            Method realMethod = clazz.getMethod(msig.getName(), msig.getParameterTypes());

            //判断是否需要进行处理，不需要放过
            EncryptMethod annotation = realMethod.getAnnotation(EncryptMethod.class);
            if (Objects.isNull(annotation)) {
                return realMethod.invoke(target, requestObjs);
            }

            //处理特殊
            for (int i = 0; i < requestObjs.length; i++) {
                if (requestObjs[i] instanceof Wrapper) {
                    Method currentMethod = target.getClass().getMethod(msig.getName(), msig.getParameterTypes());
                    responseObj = currentMethod.invoke(target, requestObjs);
                    responseObj = dealWithService.handleDecrypt(responseObj, currentMethod, secretKey, strategy);
                    return responseObj;
                }
            }

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
                        objs[i] = dealWithService.changeObject(requestObjs[i]);
                    }
                }
                if (flag) {
                    dealWithService.handleEncrypt(realMethod, objs, secretKey, strategy);
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
                                    dealWithService.setObjectId(source.get(j), list.get(j));
                                }
                            }
                        } else {
                            //保存id
                            dealWithService.setObjectId(objs[i], requestObjs[i]);
                        }
                    }
                }
            }
            //entityManagerFactory.unwrap(SessionFactory.class).getCurrentSession().flush();

            //修改response
            responseObj = dealWithService.changeObject(result);

            responseObj = dealWithService.handleDecrypt(responseObj, realMethod, secretKey, strategy);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseObj;

    }

}
