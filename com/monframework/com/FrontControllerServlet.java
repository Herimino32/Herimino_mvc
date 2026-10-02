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
import com.monframework.com.annotation.Restapi;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;

public class FrontControllerServlet extends HttpServlet {

    HashMap<UrlMethod, Mapping> registry;

    public void init() throws ServletException {
        this.registry = (HashMap<UrlMethod, Mapping>) this.getServletContext().getAttribute("urlRegistry");
        if (this.registry == null) {
            throw new ServletException(
                    "[ERREUR] Le dictionnaire 'urlRegistry' n'a pas été trouvé dans le ServletContext !");
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

        String clientMethod = request.getMethod();
        UrlMethod keyRecherche = new UrlMethod(path, clientMethod);

        Mapping mapping = this.registry.get(keyRecherche);

        if (mapping != null) {
            try {
                //  Récupération de la classe cible
                Class<?> clazz = Class.forName(mapping.getClassName());

                //  Recherche de la méthode cible par son nom 
                Method targetMethod = null;
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(mapping.getMethod())) {
                        targetMethod = m;
                        break;
                    }
                }

                if (targetMethod == null) {
                    throw new NoSuchMethodException("Méthode " + mapping.getMethod() + " introuvable dans " + mapping.getClassName());
                }

                // Extraction dynamique des paramètres depuis la requête HTTP 
                Parameter[] parameters = targetMethod.getParameters();
                Object[] args = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++) {
                    String paramName = parameters[i].getName();
                    String paramValue = request.getParameter(paramName);
                    args[i] = paramValue;
                }

                // Instanciation du contrôleur et invocation avec les arguments 
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                Object result = targetMethod.invoke(controllerInstance, args);

                //  Vérification de la présence de l'annotation @Restapi 
                boolean isRestApi = targetMethod.isAnnotationPresent(Restapi.class);

                // renvoi json
                if (isRestApi) {
                    response.setContentType("application/json;charset=UTF-8");
                    PrintWriter out = response.getWriter();

                    Object dataToSerialize = result;

                    // Si la méthode REST renvoie un ModelView, on extrait uniquement ses données
                    if (result instanceof ModelView) {
                        ModelView mv = (ModelView) result;
                        dataToSerialize = mv.getData();
                    }

                    // Conversion de l'objet en chaîne JSON
                    String jsonResponse = JsonUtils.toJson(dataToSerialize);

                    out.print(jsonResponse);
                    out.flush();
                    return; // Fin du traitement !
                } 
                
                // Traitement classique avec vue JSP
                else if (result != null && result instanceof ModelView) {
                    response.setContentType("text/html;charset=UTF-8");
                    ModelView mv = (ModelView) result;
                    String prefixe = "/WEB-INF/views/";
                    String suffixe = ".jsp";
                    String viewPath = prefixe + mv.getView() + suffixe;

                    HashMap<String, Object> modelData = mv.getData();
                    for (java.util.Map.Entry<String, Object> entry : modelData.entrySet()) {
                        String cle = entry.getKey();
                        Object valeur = entry.getValue();
                        request.setAttribute(cle, valeur);
                    }
                    RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
                    dispatcher.forward(request, response);

                    return;
                }
            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'invocation de la méthode", e);
            }
        } else {
            throw new ServletException("Clé recherchée : [Methode=" + clientMethod + ", Path='" + path + "'] | "
                    + "Routes valides enregistrées dans la Map : " + registry.keySet());
        }
    }

    private boolean isStaticResource(String uri) {
        return uri.endsWith(".html") || uri.endsWith(".css") || uri.endsWith(".js")
                || uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".gif")
                || uri.endsWith(".ico") || uri.endsWith(".svg");
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