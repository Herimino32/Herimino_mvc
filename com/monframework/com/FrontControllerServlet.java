package com.monframework.com;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import com.monframework.com.utils.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FrontControllerServlet extends HttpServlet {

    HashMap<String, Mapping> mappingUrls = new HashMap<>();

    @Override
    public void init() throws ServletException {
        try {
            String controllerPackage = getServletConfig().getInitParameter("controller");
            if (controllerPackage != null) {
                mappingUrls = ControllerUtils.getAnnotedMethods(controllerPackage);
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation", e);
        }
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getPathInfo();
        if (path == null || path.isEmpty()) {
            path = request.getServletPath();
        }
        if (path.startsWith("/")) {
        path = path.substring(1);
        }
        
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Mapping mapping = mappingUrls.get(path);

        if (mapping != null) {
        out.println("==================================================");
        out.println("   ROUTE INTERCEPTÉE AVEC SUCCÈS ");
        out.println("==================================================");
        out.println("Classe cible  : " + mapping.getClassName());
        out.println("Méthode cible : " + mapping.getMethod());
    } else {
        throw new ServletException("Erreur : L'URL '" + path + "' n'est pas supportée par le framework. "
                + "Routes valides : " + mappingUrls.keySet());
    }
}


    private boolean isStaticResource(String uri) {
        return uri.endsWith(".html") || uri.endsWith(".css") || uri.endsWith(".js")
                || uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".gif")
                || uri.endsWith(".ico") || uri.endsWith(".svg") || uri.endsWith(".jsp");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isStaticResource(uri)) {
            String contextPath = request.getContextPath();
            String path = uri.substring(contextPath.length());
            RequestDispatcher dispatcher = request.getServletContext().getRequestDispatcher(path);
            if (dispatcher != null) {
                dispatcher.forward(request, response);
                return;
            }
        }
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isStaticResource(uri)) {
            String contextPath = request.getContextPath();
            String path = uri.substring(contextPath.length());
            RequestDispatcher dispatcher = request.getServletContext().getRequestDispatcher(path);
            if (dispatcher != null) {
                dispatcher.forward(request, response);
                return;
            }
        }
        processRequest(request, response);
    }
}