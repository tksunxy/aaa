package com.github.netty.rpc.service;

import com.github.netty.rpc.RpcInterface;

import java.util.List;

/**
 * 数据存储服务
 * @author acer01
 * 2018/8/20/020
 */
@RpcInterface(value = "/_inner/db",timeout = 1000)
public interface RpcDBService {

    /**
     * 存在key
     * @param key
     * @return
     */
    boolean exist(String key);

    /**
     * 存入数据
     * @param key
     * @param data
     */
    void put(String key,byte[] data);

    /**
     * 存入数据
     * @param key
     * @param data
     * @param expireSecond 过期时间(秒)
     */
    void put(String key,byte[] data,int expireSecond);

    /**
     * 获取数据
     * @param key
     * @return
     */
    byte[] get(String key);

    /**
     * 改变key
     * @param oldKey
     * @param newKey
     */
    void changeKey(String oldKey,String newKey);

    /**
     * 删除数据
     * @param key
     */
    void remove(String key);

    /**
     * 删除多条数据
     * @param keys
     */
    void remove(List<String> keys);

}
