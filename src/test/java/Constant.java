import io.vertx.core.json.JsonObject;

/**
 * 常量
 * Created by acer01 on 2018/8/12/012.
 */
public class Constant {

    public static final int PORT = 8081;
    public static final String HOST = "127.0.0.1";
    public static final String URI = "/hello?id=1&name=abc";

    public static final JsonObject BODY = new JsonObject("{\"body1\":\"我是post内容\"}");
}
