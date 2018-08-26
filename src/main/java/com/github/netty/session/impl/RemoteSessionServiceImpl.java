package com.github.netty.session.impl;

import com.github.netty.core.rpc.RpcClient;
import com.github.netty.core.rpc.exception.RpcDecodeException;
import com.github.netty.core.support.Optimize;
import com.github.netty.session.Session;
import com.github.netty.session.SessionService;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Supplier;

/**
 * 远程会话服务
 * @author acer01
 * 2018/8/19/019
 */
public class RemoteSessionServiceImpl extends RpcClient implements SessionService {

    private static final byte[] EMPTY = new byte[0];

    public RemoteSessionServiceImpl(InetSocketAddress remoteSessionServerAddress) {
        super("Session",remoteSessionServerAddress, Optimize.getSessionClientSocketChannelCount());
        if(Optimize.isSessionClientEnableAutoReconnect()) {
            enableAutoReconnect(rpcClient -> {});
        }
    }

    @Override
    public void saveSession(Session session) {
        byte[] bytes = encode(session);
        long expireSecond = (session.getMaxInactiveInterval() * 1000 + session.getCreationTime() - System.currentTimeMillis()) / 1000;

        if(Optimize.isEnableExecuteHold()) {
            Optimize.holdExecute(new Runnable() {
                @Override
                public void run() {
                    if (expireSecond > 0) {
                        getRpcDBService().put(session.getId(), bytes, (int) expireSecond);
                    } else {
                        getRpcDBService().remove(session.getId());
                    }
                };
            });
            return;
        }


        if (expireSecond > 0) {
            getRpcDBService().put(session.getId(), bytes, (int) expireSecond);
        } else {
            getRpcDBService().remove(session.getId());
        }
    }

    @Override
    public void removeSession(String sessionId) {
        getRpcDBService().remove(sessionId);
    }

    @Override
    public void removeSessionBatch(List<String> sessionIdList) {
        getRpcDBService().remove(sessionIdList);
    }

    @Override
    public Session getSession(String sessionId) {
        if(Optimize.isEnableExecuteHold()) {
            return Optimize.holdExecute(new Supplier<Session>() {
                @Override
                public Session get() {
                    byte[] bytes = getRpcDBService().get(sessionId);
                    Session session = decode(bytes);
                    return session;
                }
            });
        }

        byte[] bytes = getRpcDBService().get(sessionId);
        Session session = decode(bytes);
        return session;
    }

    @Override
    public void changeSessionId(String oldSessionId, String newSessionId) {
        getRpcDBService().changeKey(oldSessionId,newSessionId);;
    }


    /**
     * 解码
     * @param bytes
     * @param <T>
     * @return
     */
    private <T>T decode(byte[] bytes){
        if(bytes == null || bytes.length == 0){
            return null;
        }

        ObjectInputStream ois = null;
        ByteBufInputStream bfi = null;
        try {
            bfi = new ByteBufInputStream(Unpooled.wrappedBuffer(bytes), true);
            ois = new ObjectInputStream(bfi);
            T data = (T) ois.readObject();
            return data;
        } catch (IOException | ClassNotFoundException e) {
            throw new RpcDecodeException(e.getMessage(),e);
        } finally {
            try {
                if(ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if(bfi != null){
                    bfi.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 编码
     * @param object
     * @return
     */
    private byte[] encode(Object object){
        if(object == null){
            return EMPTY;
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream(64);
//        ByteBufOutputStream bout = new ByteBufOutputStream(out);
        ObjectOutputStream oout = null;
        try {
            oout = new ObjectOutputStream(bout);
            oout.writeObject(object);
            oout.flush();
            return bout.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (oout != null) {
                try {
                    oout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    bout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

}
