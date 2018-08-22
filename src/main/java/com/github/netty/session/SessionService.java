package com.github.netty.session;

import com.github.netty.core.rpc.RpcInterface;

import java.util.List;

/**
 * Created by acer01 on 2018/8/19/019.
 */
@RpcInterface(value = "/SessionService",timeout = 500)
public interface SessionService {

    void saveSession(Session session);

    void removeSession(String sessionId);
    void removeSessionBatch(List<String> sessionIdList);

    Session getSession(String sessionId);

    void changeSessionId(String oldSessionId,String newSessionId);

}
