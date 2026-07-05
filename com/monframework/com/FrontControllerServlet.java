package com.monframework.com;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import com.monframework.com.utils.*;
import java.util.HashMap;

public class FrontControllerServlet extends HttpServlet {

    HashMap<UrlMethod, Mapping> registry;

    public void init() throws ServletException {        
        this.registry=(HashMap<UrlMethod, Mapping>)this.getServletContext().getAttribute("urlRegistry");
        if (this.registry == null) {
            throw new ServletException("[ERREUR] Le dictionnaire 'urlRegistry' n'a pas été trouvé dans le ServletContext !");
        }
        System.out.println("[INFO] FrontControllerServlet liée avec succès au dictionnaire de routes.");
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

        String clientMethod = request.getMethod();
        UrlMethod keyRecherche = new UrlMethod(path, clientMethod);

        Mapping mapping = this.registry.get(keyRecherche);

        if (mapping != null) {
            out.println("==================================================");
            out.println("   ROUTE INTERCEPTÉE AVEC SUCCÈS (Sprint 4) ");
            out.println("==================================================");
            out.println("URL demandée   : /" + path);
            out.println("Méthode HTTP   : " + clientMethod);
            out.println("Classe cible   : " + mapping.getClassName());
            out.println("Méthode cible  : " + mapping.getMethod());
            try {
                Object result = mapping.invoke();
                out.println("Methode exuted: " + result);
            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'invocation de la méthode", e);
            }
        } else {
            throw new ServletException("DÉBOGAGE SPRINT 4 -> "
                    + "Clé recherchée : [Methode=" + clientMethod + ", Path='" + path + "'] | "
                    + "Routes valides enregistrées dans la Map : " + registry.keySet());
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