package com.monframework.com;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import com.monframework.com.annotation.Restapi;
import com.monframework.com.utils.JsonUtils;
import com.monframework.com.utils.Mapping;
import com.monframework.com.utils.ModelView;
import com.monframework.com.utils.UrlMethod;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontControllerServlet extends HttpServlet {

    private HashMap<UrlMethod, Mapping> registry;

    @Override
    @SuppressWarnings("unchecked")
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

        // 1. Extraction et nettoyage du chemin de l'URL
        String path = request.getPathInfo();
        if (path == null || path.isEmpty()) {
            path = request.getServletPath();
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String clientMethod = request.getMethod();
        UrlMethod keyRecherche = new UrlMethod(path, clientMethod);

        // 2. Recherche du mapping correspondant à la route
        Mapping mapping = this.registry.get(keyRecherche);

        if (mapping != null) {
            try {
                // 3. Chargement de la classe et recherche de la méthode cible
                Class<?> clazz = Class.forName(mapping.getClassName());

                Method targetMethod = null;
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(mapping.getMethod())) {
                        targetMethod = m;
                        break;
                    }
                }

                if (targetMethod == null) {
                    throw new NoSuchMethodException(
                            "Méthode " + mapping.getMethod() + " introuvable dans " + mapping.getClassName());
                }

                // 4. Extraction et injection dynamique des paramètres 
                Class<?>[] paramTypes = targetMethod.getParameterTypes();
                // Pour récupérer les noms des paramètres
                Parameter[] parameters = targetMethod.getParameters(); 
                Object[] args = new Object[paramTypes.length];

                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> paramType = paramTypes[i];
                    String paramName = parameters[i].getName();
                    String paramValue = request.getParameter(paramName);

                    // Type String
                    if (paramType == String.class) {
                        args[i] = paramValue;
                    } 
                    // Types entiers (int / Integer)
                    else if (paramType == int.class || paramType == Integer.class) {
                        if (paramValue != null && !paramValue.trim().isEmpty()) {
                            try {
                                args[i] = Integer.parseInt(paramValue);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException(
                                        "Le paramètre '" + paramName + "' doit être un entier valide !");
                            }
                        } else {
                            args[i] = (paramType == int.class) ? 0 : null;
                        }
                    } 
                    // Objet complexe binding (POJO) avec préfixe (ex: e.nom, e.age)
                    else {
                        Object obj = paramType.getDeclaredConstructor().newInstance();

                        for (Field field : paramType.getDeclaredFields()) {
                            field.setAccessible(true);
                            String requestParamKey = paramName + "." + field.getName();
                            String fieldValue = request.getParameter(requestParamKey);

                            if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                                Class<?> fieldType = field.getType();

                                if (fieldType == int.class || fieldType == Integer.class) {
                                    field.set(obj, Integer.parseInt(fieldValue));
                                } else if (fieldType == double.class || fieldType == Double.class) {
                                    field.set(obj, Double.parseDouble(fieldValue));
                                } else if (fieldType == String.class) {
                                    field.set(obj, fieldValue);
                                }
                            }
                        }
                        args[i] = obj;
                    }
                }

                // 5. Instanciation du contrôleur et exécution de la méthode
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                Object result = targetMethod.invoke(controllerInstance, args);

                // 6. Traitement du résultat (REST JSON vs Vue JSP)
                boolean isRestApi = targetMethod.isAnnotationPresent(Restapi.class);

                // Option 1 : Réponse API REST (JSON)
                if (isRestApi) {
                    response.setContentType("application/json;charset=UTF-8");
                    PrintWriter out = response.getWriter();

                    Object dataToSerialize = result;
                    if (result instanceof ModelView) {
                        ModelView mv = (ModelView) result;
                        dataToSerialize = mv.getData();
                    }

                    String jsonResponse = JsonUtils.toJson(dataToSerialize);
                    out.print(jsonResponse);
                    out.flush();
                    return;
                } 
                // Option 2 : Redirection vers une Vue (JSP)
                else if (result != null && result instanceof ModelView) {
                    response.setContentType("text/html;charset=UTF-8");
                    ModelView mv = (ModelView) result;
                    String viewPath = "/WEB-INF/views/" + mv.getView() + ".jsp";

                    HashMap<String, Object> modelData = mv.getData();
                    for (Map.Entry<String, Object> entry : modelData.entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
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
                    + "Routes valides enregistrées : " + registry.keySet());
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
            String path = uri.substring(request.getContextPath().length());
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
            String path = uri.substring(request.getContextPath().length());
            RequestDispatcher dispatcher = request.getServletContext().getRequestDispatcher(path);
            if (dispatcher != null) {
                dispatcher.forward(request, response);
                return;
            }
        }
        processRequest(request, response);
    }
}