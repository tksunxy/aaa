package com.github.netty.session;

import com.github.netty.core.support.LoggerFactoryX;
import com.github.netty.core.support.LoggerX;
import com.github.netty.core.util.NamespaceUtil;

import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author acer01
 * 2018/8/19/019
 */
public class LocalSessionServiceImpl implements SessionService {

    private String name = NamespaceUtil.newIdName(getClass());

    private Map<String,Session> sessionMap = new ConcurrentHashMap<>(128);

    public LocalSessionServiceImpl() {
        //20秒检查一次过期session
        new SessionInvalidThread(20 * 1000).start();
    }

    @Override
    public void saveSession(Session session) {
        if(session == null){
            return;
        }
        sessionMap.put(session.getId(),session);
    }

    @Override
    public void removeSession(String sessionId) {
        sessionMap.remove(sessionId);
    }

    @Override
    public void removeSessionBatch(List<String> sessionIdList) {
        if(sessionIdList == null || sessionIdList.isEmpty()){
            return;
        }

        //减少创建迭代器
        if(sessionIdList instanceof RandomAccess){
            int size = sessionIdList.size();
            for(int i=0; i<size; i++){
                String id = sessionIdList.get(i);
                sessionMap.remove(id);
            }
        }else {
            for(String id : sessionIdList){
                sessionMap.remove(id);
            }
        }
    }

    @Override
    public Session getSession(String sessionId) {
        Session session = sessionMap.get(sessionId);
        if(session != null && session.isValid()){
            return session;
        }
        sessionMap.remove(sessionId);
        return null;
    }

    @Override
    public void changeSessionId(String oldSessionId, String newSessionId) {
        Session session = sessionMap.remove(oldSessionId);
        if(session != null && session.isValid()){
            sessionMap.put(newSessionId,session);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * 超时的Session无效化，定期执行
     */
    private class SessionInvalidThread extends Thread {
        LoggerX logger = LoggerFactoryX.getLogger(getClass());

        private final long sessionLifeCheckInter;

        private SessionInvalidThread(long sessionLifeCheckInter) {
            super(NamespaceUtil.newIdName(SessionInvalidThread.class));
            this.sessionLifeCheckInter = sessionLifeCheckInter;
        }

        @Override
        public void run() {
            logger.info("Session Manager CheckInvalidSessionThread has been started...");
            while(true){
                try {
                    Thread.sleep(sessionLifeCheckInter);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(Session session : sessionMap.values()){
                    if(!session.isValid()){
                        String id = session.getId();
                        logger.info("Session(ID="+id+") is invalidated by Session Manager");
                        sessionMap.remove(id);
                    }
                }
            }
        }
    }
}
