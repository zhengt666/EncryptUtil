package com.encrypt.mysql.base.utils;

import java.util.Date;
import java.util.List;

/**
* @Description:    判断空值工具
* @Author:         zhengt
* @CreateDate:     2020/4/20 15:35
* @UpdateUser:     zhengt
* @UpdateDate:     2020/4/20 15:35
* @UpdateRemark:   修改内容
* @Version:        1.0
*/
public class CmUtil {
    /**
     * 是否有值 : hasValue
     *
     * @param rs
     * @return
     * @add HC
     */
    public static boolean hv(String rs) {

        return rs != null && rs.trim().length() > 0;
    }
    /**
     * 是否有值 : hasValue
     *
     * @param rs
     * @return
     * @add HC
     */
    public static boolean hv(Integer rs)
    {
        return rs != null && rs != 0;
    }
    /**
     * 是否有值 : hasValue
     *
     * @param rs
     * @return
     * @add HC
     */
    public static boolean hv(Double rs)
    {
        return rs != null && rs != 0d;
    }
    /**
     * 是否有值 : hasValue
     *
     * @param rs
     * @return
     * @add HC
     */
    public static boolean hv(Date rs)
    {
        return rs != null;
    }
    /**
     * 是否有值 : hasValue
     *
     * @param rs
     * @return
     * @add HC
     */
    public static boolean hv(Long rs)
    {
        return rs != null;
    }
    /**
     * 是否有值 : hasValue
     *
     * @param str
     * @return
     * @add HC
     */
    public static boolean hv(String[] str)
    {
        return str != null && str.length > 0;
    }
    /**
     * 是否有值 : hasValue
     *
     * <h1>注意：如果list的第一个元素是null那么返回false</h1>
     *
     * @param list
     * @return
     * @add HC
     */
    public static boolean hv(List list)
    {
        if (list != null && list.size() > 0)
        {
            return hv(list.get(0));
        }
        return false;
    }
    /**
     * 是否有值 : hasValue
     *
     * @param obj
     * @return
     * @add HC
     */
    public static boolean hv(Object obj)
    {
        return obj != null;
    }
    /**
     * 是否有值 : hasValue
     *
     * <h1>注意：该方法主要用于判断多个参数同时不为null时才用</h1>
     * <h2> 用法:Scm.hv(obj1,obj2,obj3,...,args)</h2>
     *
     * @param obj
     *            参数1
     * @param args
     *            参数列表
     * @return
     * @add HC
     */
    public static boolean hv(Object obj, Object... args)
    {
        if (!hv(obj))
        {
            return false;
        }
        for (Object arg : args)
        {
            if (!hv(arg))
            {
                return false;
            }
        }
        return true;
    }
}
