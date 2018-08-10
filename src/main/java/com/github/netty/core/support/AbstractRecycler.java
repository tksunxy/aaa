package com.github.netty.core.support;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author 84215
 */
public abstract class AbstractRecycler<T> extends io.netty.util.Recycler<T> {

    private static final List<AbstractRecycler> RECYCLER_LIST = new LinkedList<>();
    private static final List<Object> INSTANCE_LIST = new ArrayList<>(1024);

    protected AbstractRecycler() {
        super();
        register();
    }

    protected AbstractRecycler(int maxCapacityPerThread) {
        super(maxCapacityPerThread);
        register();
    }

    protected AbstractRecycler(int maxCapacityPerThread, int maxSharedCapacityFactor) {
        super(maxCapacityPerThread, maxSharedCapacityFactor);
        register();
    }

    protected AbstractRecycler(int maxCapacityPerThread, int maxSharedCapacityFactor, int ratio, int maxDelayedQueuesPerThread) {
        super(maxCapacityPerThread, maxSharedCapacityFactor, ratio, maxDelayedQueuesPerThread);
        register();
    }

    @Override
    protected final T newObject(Handle<T> handle) {
        T instance = newInstance(handle);
        INSTANCE_LIST.add(instance);
        return instance;
    }

    /**
     * 新建实例
     * @param handle
     * @return
     */
    protected abstract T newInstance(Handle<T> handle);
    
    private void register(){
        RECYCLER_LIST.add(this);
    }

    public static List<AbstractRecycler> getRecyclerList() {
        return RECYCLER_LIST;
    }

    public static List<Object> getInstanceList() {
        return INSTANCE_LIST;
    }
}
