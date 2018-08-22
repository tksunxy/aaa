package com.github.netty.session.impl;

import com.github.netty.core.rpc.RpcClient;
import com.github.netty.core.util.NamespaceUtil;
import com.github.netty.session.Session;
import com.github.netty.session.SessionService;

import java.net.InetSocketAddress;
import java.util.List;

/**
 *
 * @author acer01
 * 2018/8/19/019
 */
public class RemoteSessionServiceImpl implements SessionService {

    private String name = NamespaceUtil.newIdName(getClass());
    private RpcClient rpcClient;
    private SessionService rpcSessionService;

    public RemoteSessionServiceImpl(InetSocketAddress remoteSessionServerAddress) {
        rpcClient = new RpcClient("Session",remoteSessionServerAddress);
        rpcClient.enableAutoReconnect(socketChannel -> {

        });
        rpcSessionService = rpcClient.newInstance(SessionService.class);
    }

    @Override
    public void saveSession(Session session) {
        getRpcSessionService().saveSession(session);
    }

    @Override
    public void removeSession(String sessionId) {
        getRpcSessionService().removeSession(sessionId);
    }

    @Override
    public void removeSessionBatch(List<String> sessionIdList) {
        getRpcSessionService().removeSessionBatch(sessionIdList);
    }

    @Override
    public Session getSession(String sessionId) {
        return getRpcSessionService().getSession(sessionId);
    }

    @Override
    public void changeSessionId(String oldSessionId, String newSessionId) {
        getRpcSessionService().changeSessionId(oldSessionId,newSessionId);;
    }

    public SessionService getRpcSessionService() {
//        return rpcClient.newInstance(SessionService.class);
        return rpcSessionService;
    }

    @Override
    public String toString() {
        return name;
    }

}
