package com.github.netty.session.impl;

import com.github.netty.core.support.LoggerFactoryX;
import com.github.netty.core.support.LoggerX;
import com.github.netty.core.util.NamespaceUtil;
import com.github.netty.core.util.TodoOptimize;
import com.github.netty.session.Session;
import com.github.netty.session.SessionService;

import java.net.InetSocketAddress;
import java.util.List;

/**
 *  组合会话服务
 * @author 84215
 */
@TodoOptimize("缺少自动切换功能")
public class CompositeSessionServiceImpl implements SessionService {

    private LoggerX logger = LoggerFactoryX.getLogger(getClass());

    private String name = NamespaceUtil.newIdName(getClass());
    private SessionService localSessionService;
    private SessionService remoteSessionService;

    public CompositeSessionServiceImpl() {
        this.localSessionService = new LocalSessionServiceImpl();
    }

    public void enableRemoteSession(InetSocketAddress remoteSessionServerAddress){
        if(remoteSessionServerAddress != null) {
            this.remoteSessionService = new RemoteSessionServiceImpl(remoteSessionServerAddress);
        }
    }

    @Override
    public void saveSession(Session session) {
        try {
            getSessionServiceImpl().saveSession(session);
        }catch (Throwable t){
            logger.error(t.toString());
        }
    }

    @Override
    public void removeSession(String sessionId) {
        getSessionServiceImpl().removeSession(sessionId);
    }

    @Override
    public void removeSessionBatch(List<String> sessionIdList) {
        getSessionServiceImpl().removeSessionBatch(sessionIdList);
    }

    @Override
    public Session getSession(String sessionId) {
        try {
            return getSessionServiceImpl().getSession(sessionId);
        }catch (Throwable t){
            logger.error(t.toString());
            return null;
        }
    }

    @Override
    public void changeSessionId(String oldSessionId, String newSessionId) {
        getSessionServiceImpl().changeSessionId(oldSessionId, newSessionId);
    }

    private SessionService getSessionServiceImpl() {
        if(remoteSessionService != null) {
            return remoteSessionService;
        }
        return localSessionService;
    }

    @Override
    public String toString() {
        return name;
    }

}
