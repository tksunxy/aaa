import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

/**
 * 基于netty实现的server框架
 * Created by acer01 on 2018/8/12/012.
 */
public class VertxServer {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions());

        httpServer.requestHandler(request -> {
            request.response().end("你好");

        }).listen(Constant.PORT,Constant.HOST);
    }
}
