package com.github.netty.core;

import com.github.netty.core.util.NamespaceUtil;
import io.netty.channel.ChannelException;

/**
 * @author 84215
 */
public class NioServerChannelFactory implements io.netty.bootstrap.ChannelFactory<NioServerSocketChannel>  {

    private String name = NamespaceUtil.newIdName(getClass());

    @Override
    public NioServerSocketChannel newChannel() {
        try {
            NioServerSocketChannel myNioServerSocketChannel = new NioServerSocketChannel();
//            NioServerSocketChannel myNioServerSocketChannel = ProxyUtil.newProxyByCglib(NioServerSocketChannel.class,
//                    toString() + "-"+ NamespaceUtil.newIdName(this,"serverSocketChannel"),
//                    true);

            return myNioServerSocketChannel;
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class nioServerSocketChannel", t);
        }
    }

    @Override
    public String toString() {
        return name;
    }

}