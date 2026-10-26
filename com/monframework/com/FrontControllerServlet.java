package com.monframework.com;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.monframework.com.annotation.Restapi;
import com.monframework.com.utils.File;
import com.monframework.com.utils.JsonUtils;
import com.monframework.com.utils.Mapping;
import com.monframework.com.utils.ModelView;
import com.monframework.com.utils.UrlMethod;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

public class FrontControllerServlet extends HttpServlet {

    private HashMap<UrlMethod, Mapping> registry;
    private String uploadDirectory;

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        this.registry = (HashMap<UrlMethod, Mapping>) this.getServletContext().getAttribute("urlRegistry");
        if (this.registry == null) {
            throw new ServletException(
                    "[ERREUR] Le dictionnaire 'urlRegistry' n'a pas été trouvé dans le ServletContext !");
        }

        // Chargement du fichier de configuration properties pour l'upload
        try {
            Properties props = new Properties();
            var input = getServletContext().getResourceAsStream("/WEB-INF/config.properties");
            if (input != null) {
                props.load(input);
                this.uploadDirectory = props.getProperty("upload.path", "/tmp/uploads/");
            } else {
                this.uploadDirectory = "/tmp/uploads/";
            }
        } catch (Exception e) {
            this.uploadDirectory = "/tmp/uploads/";
        }

        System.out.println("[INFO] FrontControllerServlet initialisé. Dossier d'upload : " + uploadDirectory);
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
                    // Gestion de la classe File (Sprint 8)
                    else if (isFileClass(paramType)) {
                        args[i] = extractAndSaveFile(request, paramName, paramType);
                    }
                    // 🔑 Gestion de la classe Session (MySession)
                    else if (isSessionClass(paramType)) {
                        args[i] = paramType.getDeclaredConstructor(HttpSession.class)
                                           .newInstance(request.getSession());
                    }
                    // Objets complexes et imbriqués (Sprint 7 & 7 bis)
                    else {
                        Object obj = paramType.getDeclaredConstructor().newInstance();
                        populateObjectRecursively(obj, paramName, request);
                        args[i] = obj;
                    }
                }

                // 5. Instanciation du contrôleur et exécution de la méthode
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                Object result = targetMethod.invoke(controllerInstance, args);

                // 6. Traitement du résultat (REST JSON vs Vue JSP)
                boolean isRestApi = targetMethod.isAnnotationPresent(Restapi.class);

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

    // --- MÉTHODES UTILITAIRES ---

    private boolean isFileClass(Class<?> type) {
        return type.getSimpleName().equalsIgnoreCase("file") || type.getName().endsWith("File");
    }

    // 🔑 Méthode utilitaire pour détecter le type MySession
    private boolean isSessionClass(Class<?> type) {
        return type.getSimpleName().equalsIgnoreCase("MySession") || type.getName().endsWith("MySession");
    }

    private Object extractAndSaveFile(HttpServletRequest request, String paramName, Class<?> fileClass) {
        try {
            Part filePart = request.getPart(paramName);
            if (filePart != null && filePart.getSize() > 0) {
                Object fileObj = fileClass.getDeclaredConstructor().newInstance();
                byte[] fileBytes = filePart.getInputStream().readAllBytes();
                String contentType = filePart.getContentType();
                String fileName = filePart.getSubmittedFileName();

                // Sauvegarde physique sur le disque
                java.io.File uploadDir = new java.io.File(this.uploadDirectory);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                Path destinationPath = Paths.get(uploadDir.getAbsolutePath(), fileName);
                Files.write(destinationPath, fileBytes);

                // Remplissage des attributs de la classe File
                for (Field field : fileClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    String fname = field.getName();
                    if (fname.equals("data")) {
                        field.set(fileObj, fileBytes);
                    } else if (fname.equals("format") || fname.equals("contentType")) {
                        field.set(fileObj, contentType);
                    } else if (fname.equals("name") || fname.equals("filename")) {
                        field.set(fileObj, fileName);
                    }
                }
                return fileObj;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction du fichier : " + paramName, e);
        }
        return null;
    }

    private void populateObjectRecursively(Object obj, String prefix, HttpServletRequest request) throws Exception {
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            String fullKey = prefix + "." + field.getName();

            if (fieldType == String.class) {
                String val = request.getParameter(fullKey);
                if (val != null) {
                    field.set(obj, val);
                }
            } else if (fieldType == int.class || fieldType == Integer.class) {
                String val = request.getParameter(fullKey);
                if (val != null && !val.trim().isEmpty()) {
                    field.set(obj, Integer.parseInt(val));
                }
            } else if (fieldType == double.class || fieldType == Double.class) {
                String val = request.getParameter(fullKey);
                if (val != null && !val.trim().isEmpty()) {
                    field.set(obj, Double.parseDouble(val));
                }
            } else if (isFileClass(fieldType)) {
                Object fileObj = extractAndSaveFile(request, fullKey, fieldType);
                if (fileObj != null) {
                    field.set(obj, fileObj);
                }
            } else {
                // Objet imbriqué (ex: Adresse)
                Object nestedObj = fieldType.getDeclaredConstructor().newInstance();
                populateObjectRecursively(nestedObj, fullKey, request);
                field.set(obj, nestedObj);
            }
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