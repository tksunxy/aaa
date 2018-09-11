package com.github.netty.rpc.service;

import com.github.netty.rpc.RpcInterface;

import java.util.List;

/**
 * 数据存储服务
 *
 * @author acer01
 * 2018/8/20/020
 */
@RpcInterface(value = "/_inner/db",timeout = 1000)
public interface RpcDBService {

    /**
     * 存在key
     * @param key
     * @param group 分组
     * @return
     */
    boolean exist(String key, String group);
    boolean exist(String key);

    /**
     * 存入数据
     * @param key
     * @param data
     * @param expireSecond 过期时间(秒)
     * @param group 分组
     */
    void put(String key,byte[] data,int expireSecond,String group);
    void put(String key,byte[] data,int expireSecond);
    void put(String key,byte[] data);

    /**
     * 获取某个组的数量
     * @param group 分组
     */
    int count(String group);

    /**
     * 获取数据
     * @param key
     * @param group 分组
     * @return
     */
    byte[] get(String key, String group);
    byte[] get(String key);

    /**
     * 改变key
     * @param oldKey
     * @param newKey
     * @param group 分组
     */
    void changeKey(String oldKey, String newKey, String group);
    void changeKey(String oldKey,String newKey);

    /**
     * 删除数据
     * @param key

     */
    void remove(String key, String group);
    void remove(String key);

    /**
     * 删除多条数据
     * @param keys
     * @param group 分组
     */
    void removeBatch(List<String> keys, String group);
    void removeBatch(List<String> keys);

}
