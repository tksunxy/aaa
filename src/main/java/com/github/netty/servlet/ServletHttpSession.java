package com.github.netty.servlet;

import com.github.netty.servlet.support.ServletEventListenerManager;

import javax.servlet.http.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.netty.core.util.ObjectUtil.NULL;

/**
 * Created by acer01 on 2018/7/15/015.
 */
public class ServletHttpSession implements HttpSession{

    private ServletContext servletContext;
    private String id;

    private Map<String,Object> attributeMap;
    private long creationTime;
    private long currAccessedTime;
    private long lastAccessedTime;
    //单位 秒
    private volatile int maxInactiveInterval;
    private volatile boolean newSessionFlag;
    private transient AtomicInteger accessCount;

    private List<HttpSessionBindingListener> httpSessionBindingListenerList;

    ServletHttpSession(String id, ServletContext servletContext, ServletSessionCookieConfig sessionCookieConfig) {
        this.id = id;
        this.servletContext = servletContext;
        this.attributeMap = null;
        this.creationTime = System.currentTimeMillis();
        this.newSessionFlag = true;
        this.maxInactiveInterval = sessionCookieConfig.getSessionTimeout();
        this.accessCount = new AtomicInteger(0);
    }

    private Map<String, Object> getAttributeMap() {
        if(attributeMap == null){
            attributeMap = new ConcurrentHashMap<>(16);
        }
        return attributeMap;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        Object value = getAttributeMap().get(name);
        return value == NULL? null: value;
    }

    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(getAttributeMap().keySet());
    }

    @Override
    public String[] getValueNames() {
        return getAttributeMap().keySet().toArray(new String[getAttributeMap().size()]);
    }

    @Override
    public void setAttribute(String name, Object value) {
        Objects.requireNonNull(name);

        if(value == null){
            removeValue(name);
            return;
        }

        Object oldValue = getAttributeMap().put(name,value);

        if(value instanceof HttpSessionBindingListener){
            httpSessionBindingListenerList = new ArrayList<>();
            httpSessionBindingListenerList.add((HttpSessionBindingListener) value);
        }

        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasHttpSessionAttributeListener()){
            listenerManager.onHttpSessionAttributeAdded(new HttpSessionBindingEvent(this,name,value));
            if(oldValue != null){
                listenerManager.onHttpSessionAttributeReplaced(new HttpSessionBindingEvent(this,name,oldValue));
            }
        }

        if(httpSessionBindingListenerList != null){
            HttpSessionBindingEvent valueBoundEvent = new HttpSessionBindingEvent(this,name,value);
            for(HttpSessionBindingListener listener : httpSessionBindingListenerList){
                try {
                    listener.valueBound(valueBoundEvent);
                }catch (Throwable throwable){
                    throwable.printStackTrace();
                }
            }

            if(oldValue != null){
                HttpSessionBindingEvent valueUnboundEvent = new HttpSessionBindingEvent(this,name,oldValue);
                for(HttpSessionBindingListener listener : httpSessionBindingListenerList){
                    try {
                        listener.valueUnbound(valueUnboundEvent);
                    }catch (Throwable throwable){
                        throwable.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name,value);
    }

    @Override
    public void removeAttribute(String name) {
        Object oldValue = getAttributeMap().remove(name);

        if(oldValue instanceof HttpSessionBindingListener && httpSessionBindingListenerList != null){
            httpSessionBindingListenerList.remove(oldValue);
        }

        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasHttpSessionAttributeListener()){
            listenerManager.onHttpSessionAttributeRemoved(new HttpSessionBindingEvent(this,name,oldValue));
        }

        if(httpSessionBindingListenerList != null){
            HttpSessionBindingEvent valueUnboundEvent = new HttpSessionBindingEvent(this,name,oldValue);
            for(HttpSessionBindingListener listener : httpSessionBindingListenerList){
                listener.valueUnbound(valueUnboundEvent);
            }
        }
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void invalidate() {
        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasHttpSessionListener()){
            listenerManager.onHttpSessionDestroyed(new HttpSessionEvent(this));
        }

        servletContext.getHttpSessionMap().remove(id);
        if(attributeMap != null) {
            attributeMap.clear();
            attributeMap = null;
        }
        servletContext = null;
        maxInactiveInterval = -1;
    }

    public void init() {
        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasHttpSessionListener()){
            listenerManager.onHttpSessionCreated(new HttpSessionEvent(this));
        }
    }

    @Override
    public boolean isNew() {
        return newSessionFlag;
    }

    /**
     * 是否有效
     * @return true 有效, false无效
     */
    public boolean isValid() {
        return System.currentTimeMillis() < (creationTime + (maxInactiveInterval * 1000));
    }

    public void setNewSessionFlag(boolean newSessionFlag) {
        this.newSessionFlag = newSessionFlag;
    }

    public ServletHttpSession access(){
        lastAccessedTime = currAccessedTime = System.currentTimeMillis();
        accessCount.incrementAndGet();
        return this;
    }

}
