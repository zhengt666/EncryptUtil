package com.encrypt.mysql.base.algorithm;

import com.encrypt.mysql.base.algorithm.aesencrypt.AESEncryptStrategy;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 策略选择器
 * @Author: zhengt
 * @CreateDate: 2020/4/20 15:32
 * @UpdateUser: zhengt
 * @UpdateDate: 2020/4/20 15:32
 * @UpdateRemark: 修改内容
 * @Version: 1.0
 */
public class EncrypyContext {

    /**
     * 单例
     */
    private volatile static EncrypyContext encrypyContext;

    /**
     * 还没有实例化过
     */
    private static boolean flag = true;

    /**
     * 私有构造
     */
    private EncrypyContext() {
        synchronized (EncrypyContext.class) {
            if (flag) {
                //已经实例化过了
                flag = false;
            } else {
                throw new RuntimeException("请不要试图用反射破解单例模式");
            }
        }
    }

    /**
     * 单例获取
     * @return
     */
    public static EncrypyContext getInstance(){
        if (ObjectUtils.isEmpty(encrypyContext)){
            synchronized (EncrypyContext.class){
                if (ObjectUtils.isEmpty(encrypyContext)){
                    encrypyContext = new EncrypyContext();
                }
            }
        }
        return encrypyContext;
    }

    /**
     * 策略模式保存集合
     */
    private static Map<String, IEncryptStrategy> strategyMap = new HashMap<>();

    /**
     * 添加策略模式
     */
    static {
        strategyMap.put("AES", new AESEncryptStrategy());
    }

    /**
     * 根据类型获取模式
     *
     * @param type 类型
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/17 15:32
     */
    public IEncryptStrategy getStrategy(String type) {
        return strategyMap.get(type);
    }

    /**
     * 加密
     *
     * @param content  加密内容
     * @param password 盐值
     * @param strategy 策略
     * @return
     */
    public String encrypt(String content, String password, String strategy) {
        IEncryptStrategy encryptStrategy = strategyMap.get(strategy);
        return encryptStrategy.encrypt(content, password);
    }

    /**
     * 解密
     *
     * @param content  解密内容
     * @param password 盐值
     * @param strategy 策略
     * @return
     */
    public String decrypt(String content, String password, String strategy) {
        IEncryptStrategy encryptStrategy = strategyMap.get(strategy);
        return encryptStrategy.decrypt(content, password);
    }
}
