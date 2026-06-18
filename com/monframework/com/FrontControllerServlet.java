package com.monframework.com;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.monframework.com.utils.*;

import java.util.ArrayList;
import java.util.List;
public class FrontControllerServlet extends HttpServlet{

    List<Class<?>> listeControllers = new ArrayList<>();
    @Override
    public void init() throws ServletException{
        try{
            String controllerPackage = getServletConfig().getInitParameter("controller");
            if(controllerPackage != null){
                listeControllers = ControllerUtils.getController(controllerPackage);
            }
        }catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du FrontControllerServlet", e);
        }
    }



    public void processRequest(HttpServletRequest request,HttpServletResponse response)
    throws ServletException , IOException{

        String path = request.getPathInfo();
        
        if (path == null || path.isEmpty()) {
        path = request.getServletPath();
    }
        response.setContentType("text/plain");
        response.getWriter().println("Route interceptee par framework :" + path);
    }
    @Override
    protected void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException , IOException{
        String uri = request.getRequestURI();
        if (uri.endsWith(".html")
                || uri.endsWith(".css")
                || uri.endsWith(".js")
                || uri.endsWith(".png")
                || uri.endsWith(".jpg")
                || uri.endsWith(".gif")
                || uri.endsWith(".ico")
                || uri.endsWith(".svg")
                || uri.endsWith(".jsp")) {

            request.getServletContext()
                .getNamedDispatcher("default")
                .forward(request, response);
            return;
        }

        processRequest(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException ,IOException{
        String uri = request.getRequestURI();
        if (uri.endsWith(".html")
                || uri.endsWith(".css")
                || uri.endsWith(".js")
                || uri.endsWith(".png")
                || uri.endsWith(".jpg")
                || uri.endsWith(".gif")
                || uri.endsWith(".ico")
                || uri.endsWith(".svg")
                || uri.endsWith(".jsp")) {

            request.getServletContext()
                .getNamedDispatcher("default")
                .forward(request, response);
            return;
        }

        processRequest(request,response);
    }
}