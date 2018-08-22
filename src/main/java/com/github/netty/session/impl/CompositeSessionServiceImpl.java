package com.github.netty.session.impl;

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
        getSessionServiceImpl().saveSession(session);
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
        return getSessionServiceImpl().getSession(sessionId);
    }

    @Override
    public void changeSessionId(String oldSessionId, String newSessionId) {
        getSessionServiceImpl().changeSessionId(oldSessionId, newSessionId);
    }

    public SessionService getSessionServiceImpl() {
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
