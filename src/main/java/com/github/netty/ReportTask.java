package com.github.netty;

import com.github.netty.core.support.LoggerFactoryX;
import com.github.netty.core.support.LoggerX;
import com.github.netty.rpc.RpcClientHandler;
import com.github.netty.servlet.ServletFilterChain;
import com.github.netty.springboot.NettyServletHandler;

import javax.servlet.Filter;
import java.math.BigDecimal;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统计服务器信息的任务
 */
public class ReportTask implements Runnable{

    private LoggerX logger = LoggerFactoryX.getLogger(getClass());
    private AtomicInteger reportCount = new AtomicInteger();
    private long beginTime = System.currentTimeMillis();

    @Override
    public void run() {
        try {
            String timeoutApis = RpcClientHandler.getTimeoutApis();
            long spinResponseCount = RpcClientHandler.RpcLock.TOTAL_SPIN_RESPONSE_COUNT.get();
            long totalCount = RpcClientHandler.getTotalInvokeCount();
            long timeoutCount = RpcClientHandler.getTotalTimeoutCount();
            long successCount = totalCount - timeoutCount;
            double rate = totalCount == 0? 0:(double) successCount/(double) totalCount * 100;
            double rateSpinResponseCount = totalCount==0?0:(double) spinResponseCount/(double) totalCount * 100;

            long totalTime = System.currentTimeMillis() - beginTime;

            long servletQueryCount = NettyServletHandler.SERVLET_QUERY_COUNT.get();
            long servletAndFilterTime = NettyServletHandler.SERVLET_AND_FILTER_TIME.get();
            long servletTime = ServletFilterChain.SERVLET_TIME.get();
            long filterTime = ServletFilterChain.FILTER_TIME.get();

            double servletAndFilterAvgRuntime = servletQueryCount == 0? 0:(double)servletAndFilterTime/(double)servletQueryCount;
            double servletAvgRuntime = servletQueryCount ==0? 0:(double)servletTime/(double)servletQueryCount;
            double filterAvgRuntime = servletQueryCount ==0? 0:(double)filterTime/(double)servletQueryCount;

            StringJoiner filterJoin = new StringJoiner(", ");
            for(Map.Entry<Filter,AtomicLong> e : ServletFilterChain.FILTER_TIME_MAP.entrySet()){
//                    double filterAvgTime = (double)e.getValue().get() / (double)servletQueryCount;
                filterJoin.add(
                        e.getKey().getClass().getSimpleName()
                );
            }

            StringJoiner joiner = new StringJoiner(", ");
            joiner.add("\r\n第"+reportCount.incrementAndGet()+"次统计 ");
            joiner.add("时间="+(totalTime/60000)+"分"+((totalTime % 60000 ) / 1000)+"秒 ");
            joiner.add("rpc调用次数=" + successCount);
            joiner.add("超时次数=" + timeoutCount);
            joiner.add("自旋成功数=" + spinResponseCount);
            joiner.add("自旋成功率=" + new BigDecimal(rateSpinResponseCount).setScale(2,BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toPlainString() + "%, ");
            joiner.add("调用成功率=" + new BigDecimal(rate).setScale(2,BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toPlainString()+"%, ");
            joiner.add("超时api="+timeoutApis);
            joiner.add("servlet执行次数="+servletQueryCount);
            joiner.add("servlet+filter平均时间="+new BigDecimal(servletAndFilterAvgRuntime).setScale(4,BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toString()+"ms,");
            joiner.add("servlet平均时间="+new BigDecimal(servletAvgRuntime).setScale(4,BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toString()+"ms, ");
            joiner.add("filter平均时间="+new BigDecimal(filterAvgRuntime).setScale(4,BigDecimal.ROUND_HALF_DOWN).stripTrailingZeros().toString()+"ms, ");
//            joiner.add("\r\n "+filterJoin.toString());

            addMessage(joiner);

            logger.info(joiner.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected void addMessage(StringJoiner messageJoiner){

    }

}