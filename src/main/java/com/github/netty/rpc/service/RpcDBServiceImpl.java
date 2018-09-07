package com.github.netty.rpc.service;

import com.github.netty.core.support.LoggerFactoryX;
import com.github.netty.core.support.LoggerX;

import java.util.*;

/**
 * 数据存储服务
 * @author 84215
 */
public class RpcDBServiceImpl implements RpcDBService {

    private LoggerX logger = LoggerFactoryX.getLogger(getClass());

    private ExpiryMap<String,byte[]> memExpiryMap = new ExpiryMap<>(-1);

    @Override
    public boolean exist(String key) {
        return memExpiryMap.containsKey(key);
    }

    @Override
    public void put(String key, byte[] data) {
        memExpiryMap.put(key,data);
    }

    @Override
    public void put(String key, byte[] data, int expireSecond) {
        logger.info("保存数据 : key="+key);
        memExpiryMap.put(key,data,expireSecond);
    }

    @Override
    public byte[] get(String key) {
        return memExpiryMap.get(key);
    }

    @Override
    public void changeKey(String oldKey, String newKey) {
        memExpiryMap.changeKey(oldKey,newKey);
    }

    @Override
    public void remove(String key) {
        memExpiryMap.remove(key);
    }

    @Override
    public void remove(List<String> keys) {
        if(keys == null || keys.isEmpty()){
            return;
        }

        if(keys instanceof RandomAccess) {
            int size = keys.size();
            for (int i=0; i<size; i++){
                String key = keys.get(i);
                memExpiryMap.remove(key);
            }
        }else {
            for (String key : keys) {
                memExpiryMap.remove(key);
            }
        }
    }


    /**
     * 定时过期Map 会自动过期删除
     * 常用场景 ： localCache
     */
    public class ExpiryMap <K, V> extends HashMap<K, V> {

        private final Object lock;
        private Map<K, Long> expiryMap;

        /**
         * 默认过期时间 2分钟
         */
        private long defaultExpiryTime;


        public ExpiryMap(){
            this(1000 * 60 * 2);
        }

        public ExpiryMap(long defaultExpiryTime){
            this(16, defaultExpiryTime);
        }

        public ExpiryMap(int initialCapacity, long defaultExpiryTime){
            super(initialCapacity);
            this.defaultExpiryTime = defaultExpiryTime < 0 ? -1 : defaultExpiryTime;
            this.expiryMap = new HashMap<K,Long>(initialCapacity);
            this.lock = new Object();
        }
        @Override
        public V put(K key, V value) {
            return put(key,value,defaultExpiryTime);
        }

        public void changeKey(K oldKey, K newKey) {
            Long expiry = expiryMap.remove(oldKey);
            //如果已经过期
            if(expiry == null || expiry - System.currentTimeMillis() <= 0){
                return;
            }
            put(newKey,super.remove(oldKey),expiry);
        }

        /**
         * @param key
         * @param value
         * @param expiryTime 键值对有效期 毫秒
         * @return
         */
        public V put(K key, V value, long expiryTime) {
            expiryMap.put(key, System.currentTimeMillis() + expiryTime);
            return super.put(key, value);
        }
        @Override
        public boolean containsKey(Object key) {
            return !checkExpiry( key) && super.containsKey(key);
        }
        @Override
        public int size() {
            checkExpiry();
            return super.size();
        }
        @Override
        public boolean isEmpty() {
            return size() == 0;
        }
        @Override
        public boolean containsValue(Object value) {
            Iterator<Entry<K, V>> iterator = super.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, V> entry = iterator.next();
                K key = entry.getKey();
                V cValue = entry.getValue();
                if (cValue != value || !value.equals(cValue)) {
                    continue;
                }

                if (!checkExpiry(key, false)) {
                    return true;
                }

                remove(iterator, key);
                return false;
            }

            return false;
        }

        private void remove(Iterator<Map.Entry<K,V>> it, K key){
            expiryMap.remove(key);
            it.remove();
        }

        @Override
        public V remove(Object key) {
            expiryMap.remove(key);
            V v = super.remove(key);
            return v;
        }

        @Override
        public V get(Object key) {
            if (key == null) {
                return null;
            }
            if(checkExpiry(key)) {
                return null;
            }
            return super.get(key);
        }
        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                expiryMap.put(e.getKey(), System.currentTimeMillis() + defaultExpiryTime);
            }
            super.putAll(m);
        }

        @Override
        public Collection<V> values() {
            checkExpiry();
            return super.values();
        }

        @Override
        public Set<K> keySet() {
            checkExpiry();
            return super.keySet();
        }

        @Override
        public Set<Map.Entry<K,V>> entrySet() {
            synchronized (lock) {
                Set<Map.Entry<K, V>> set = super.entrySet();
                Iterator<Map.Entry<K, V>> iterator = set.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<K, V> entry = iterator.next();
                    K key = entry.getKey();
                    if (checkExpiry(key, false)) {
                        remove(iterator, key);
                    }
                }
                return set;
            }
        }

        public Long getExpiry(K key) {
            return expiryMap.get(key);
        }

        private void checkExpiry(){
            entrySet();
        }
        /**
         *
         * @Description: 是否过期
         * @param key true 过期
         * @param isRemoveSuper true super删除
         * @return
         */
        private boolean checkExpiry(Object key, boolean isRemoveSuper){
            Long expiryTime = expiryMap.get(key);
            if(expiryTime == null) {
                return true;
            }

            long currentTime = System.currentTimeMillis();
            boolean disable = currentTime > expiryTime;

//        System.out.println( key + " 过期时间"+expiryTime);
//        System.out.println( key + " 当前时间"+currentTime);

            if(disable){
                if(isRemoveSuper) {
                    remove(key);
                }
            }
            return disable;
        }

        private boolean checkExpiry(Object key){
            return checkExpiry(key,true);
        }

        public long getDefaultExpiryTime() {
            return defaultExpiryTime;
        }

        public void setDefaultExpiryTime(long defaultExpiryTime) {
            this.defaultExpiryTime = defaultExpiryTime;
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

}
