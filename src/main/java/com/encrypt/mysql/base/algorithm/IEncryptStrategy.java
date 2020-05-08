package com.encrypt.mysql.base.algorithm;

/**
* @Description:    加解密策略接口
* @Author:         zhengt
* @CreateDate:     2020/4/20 15:33
* @UpdateUser:     zhengt
* @UpdateDate:     2020/4/20 15:33
* @UpdateRemark:   修改内容
* @Version:        1.0
*/
public interface IEncryptStrategy {

     /**
      * 加密
      * @param content 加密内容
      * @param password 盐值
      * @return
      */
     String encrypt(String content, String password);

     /**
      * 解密
      * @param content 解密内容
      * @param password 盐值
      * @return
      */
     String decrypt(String content, String password);

}
