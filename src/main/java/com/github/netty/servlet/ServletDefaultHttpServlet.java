package com.github.netty.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * @author acer01
 *  2018/7/15/015
 */
public class ServletDefaultHttpServlet extends HttpServlet {

    public static final ServletDefaultHttpServlet INSTANCE = new ServletDefaultHttpServlet();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        byte[] bytes = "ok".getBytes();
        resp.getOutputStream().write(bytes);
    }
}
