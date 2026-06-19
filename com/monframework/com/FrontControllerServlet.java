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
import java.util.List;

public class FrontControllerServlet extends HttpServlet {

    List<Class<?>> listeControllers = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        try {
            String controllerPackage = getServletConfig().getInitParameter("controller");
            if (controllerPackage != null) {
                listeControllers = ControllerUtils.getController(controllerPackage);
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

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("Route interceptee par framework : " + path);
        out.println("--- Liste des contrôleurs scannés ---");
        
        if (listeControllers.isEmpty()) {
            out.println("Aucun contrôleur trouvé.");
        } else {
            for (Class<?> clazz : listeControllers) {
                out.println(" -> " + clazz.getName());
            }
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