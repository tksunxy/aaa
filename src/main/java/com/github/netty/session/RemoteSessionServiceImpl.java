package com.github.netty.session;

import com.github.netty.ContainerConfig;
import com.github.netty.core.constants.CoreConstants;
import com.github.netty.core.util.NamespaceUtil;
import com.github.netty.rpc.RpcClient;
import com.github.netty.rpc.exception.RpcDecodeException;
import com.github.netty.rpc.service.RpcDBService;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.FastThreadLocal;

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
public class RemoteSessionServiceImpl implements SessionService {

    private String name = NamespaceUtil.newIdName(getClass());
    private static final byte[] EMPTY = new byte[0];
    private ContainerConfig config;
    private InetSocketAddress address;

    private static final String SESSION_GROUP = "/session";

    private FastThreadLocal<RpcClient> rpcClientThreadLocal = new FastThreadLocal<RpcClient>(){
        @Override
        protected RpcClient initialValue() throws Exception {
            RpcClient rpcClient = new RpcClient("Session",address);
            rpcClient.setIoRatio(config.getClientIoRatio());
            rpcClient.setWorkerCount(config.getClientWorkerCount());
            rpcClient.setSocketChannelCount(config.getSessionClientChannelCount());
            rpcClient.run();
            if(config.isEnablesSessionClientAutoReconnect()) {
                rpcClient.enableAutoReconnect();
            }
            return rpcClient;
        }
    };

    public RemoteSessionServiceImpl(InetSocketAddress address,ContainerConfig config) {
        this.address = address;
        this.config = config;
    }

    @Override
    public void saveSession(Session session) {
        byte[] bytes = encode(session);
        long expireSecond = (session.getMaxInactiveInterval() * 1000 + session.getCreationTime() - System.currentTimeMillis()) / 1000;

        if(CoreConstants.isEnableExecuteHold()) {
            CoreConstants.holdExecute(new Runnable() {
                @Override
                public void run() {
                    if (expireSecond > 0) {
                        getRpcDBService().put(session.getId(), bytes, (int) expireSecond,SESSION_GROUP);
                    } else {
                        getRpcDBService().remove(session.getId(),SESSION_GROUP);
                    }
                };
            });
            return;
        }


        if (expireSecond > 0) {
            getRpcDBService().put(session.getId(), bytes, (int) expireSecond,SESSION_GROUP);
        } else {
            getRpcDBService().remove(session.getId(),SESSION_GROUP);
        }
    }

    @Override
    public void removeSession(String sessionId) {
        getRpcDBService().remove(sessionId,SESSION_GROUP);
    }

    @Override
    public void removeSessionBatch(List<String> sessionIdList) {
        getRpcDBService().removeBatch(sessionIdList,SESSION_GROUP);
    }

    @Override
    public Session getSession(String sessionId) {
        if(CoreConstants.isEnableExecuteHold()) {
            return CoreConstants.holdExecute(new Supplier<Session>() {
                @Override
                public Session get() {
                    byte[] bytes = getRpcDBService().get(sessionId,SESSION_GROUP);
                    Session session = decode(bytes);
                    return session;
                }
            });
        }

        byte[] bytes = getRpcDBService().get(sessionId,SESSION_GROUP);
        Session session = decode(bytes);
        return session;
    }

    @Override
    public void changeSessionId(String oldSessionId, String newSessionId) {
        getRpcDBService().changeKey(oldSessionId,newSessionId,SESSION_GROUP);;
    }

    @Override
    public int count() {
        return getRpcDBService().count(SESSION_GROUP);
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

    public RpcClient getRpcClient() {
        return rpcClientThreadLocal.get();
    }

    public RpcDBService getRpcDBService() {
        return getRpcClient().getRpcDBService();
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

        ByteArrayOutputStream bout = new ByteArrayOutputStream(256);
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
        return name;
    }

}
