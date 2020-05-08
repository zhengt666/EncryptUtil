package com.encrypt.mysql.base.algorithm.aesencrypt;

import com.encrypt.mysql.base.algorithm.IEncryptStrategy;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* @Description:    AES策略模式
* @Author:         zhengt
* @CreateDate:     2020/4/20 15:31
* @UpdateUser:     zhengt
* @UpdateDate:     2020/4/20 15:31
* @UpdateRemark:   修改内容
* @Version:        1.0
*/
public class AESEncryptStrategy implements IEncryptStrategy {

    /**
     * AES算法
     */
    private static final String KEY_ALGORITHM = "AES";
    /**
     * 默认的加密算法
     */
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    /**
     * AES 加密操作
     *
     * @param content  待加密内容
     * @param password
     * @return 返回Base64转码后的加密数据
     */
    @Override
    public String encrypt(String content, String password) {
        return aes_encrypt(content, password);
    }

    /**
     * AES 解密操作
     *
     * @param content
     * @param password
     * @return
     */
    @Override
    public String decrypt(String content, String password) {
        return aes_decrypt(content,password);
    }

    /**
     * 生成加密秘钥
     *
     * @return
     */
    private static SecretKeySpec getSecretKey(final String password) {
        //返回生成指定算法密钥生成器的 KeyGenerator 对象
        KeyGenerator kg = null;
        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);
            //AES 要求密钥长度为 128
            kg.init(128, new SecureRandom(password.getBytes()));
            //生成一个密钥
            SecretKey secretKey = kg.generateKey();
            // 转换为AES专用密钥
            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(AESEncryptStrategy.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * 将二进制转换成16进制
     *
     * @param buf
     * @return
     */
    public static String parseByte2HexStr(byte[] buf) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将16进制转换为二进制
     *
     * @param hexStr
     * @return
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /**
     * mysql aes_encrypt算法
     *
     * @param password 加密文
     * @param strKey   盐
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/21 9:52
     */
    public static String aes_encrypt(String password, String strKey) {
        try {
            SecretKey key = generateMySQLAESKey(strKey, "ASCII");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cleartext = password.getBytes("UTF-8");
            byte[] ciphertextBytes = cipher.doFinal(cleartext);
            return new String(Hex.encodeHex(ciphertextBytes)).toUpperCase();

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * mysql aes_decrypt算法
     *
     * @param content 加密文
     * @param aesKey  盐
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/21 9:53
     */
    public static String aes_decrypt(String content, String aesKey) {
        try {
            SecretKey key = generateMySQLAESKey(aesKey, "ASCII");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] cleartext = Hex.decodeHex(content.toCharArray());
            byte[] ciphertextBytes = cipher.doFinal(cleartext);
            return new String(ciphertextBytes, "UTF-8");

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | DecoderException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取盐加密
     *
     * @param key      盐
     * @param encoding 格式
     * @return
     * @throws
     * @author zhengt
     * @date 2019/10/21 9:54
     */
    private static SecretKeySpec generateMySQLAESKey(final String key, final String encoding) {
        try {
            final byte[] finalKey = new byte[16];
            int i = 0;
            for (byte b : key.getBytes(encoding)) {
                finalKey[i++ % 16] ^= b;
            }
            return new SecretKeySpec(finalKey, "AES");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
