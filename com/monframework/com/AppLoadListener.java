package com.monframework.com;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import com.monframework.com.utils.*;
import java.util.HashMap;

@WebListener
public class AppLoadListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[INFO] Tomcat démarre : Initialisation du dictionnaire de routes...");
        
        ServletContext context = sce.getServletContext();

        String packageToScan = context.getInitParameter("scan-package");

        if(packageToScan == null || packageToScan.trim().isEmpty()){
            System.err.println("[ERREUR] Aucun package à scanner n'a été spécifié dans le web.xml ! (paramètre 'scan-package')");
            return;
        }

        HashMap<UrlMethod, Mapping> registry = new HashMap<>();

        ControllerUtils.getAnnotedMethods(packageToScan, registry);

        context.setAttribute("urlRegistry", registry);

        System.out.println("[SUCCESS] scan du '" + packageToScan +  "' Nombre de routes chargées : " + registry.size());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[INFO] Arrêt de l'application.");
    }
}