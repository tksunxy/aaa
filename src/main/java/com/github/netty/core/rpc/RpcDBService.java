package com.github.netty.core.rpc;

import java.util.List;

/**
 * Created by acer01 on 2018/8/20/020.
 */
@RpcInterface(value = "/_inner/DBService",timeout = 1000)
public interface RpcDBService {

    boolean exist(String key);

    void put(String key,byte[] data);
    void put(String key,byte[] data,int expireSecond);

    byte[] get(String key);

    void changeKey(String oldKey,String newKey);

    void remove(String key);
    void remove(List<String> keys);
}
