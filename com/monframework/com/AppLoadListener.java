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
    
        HashMap<UrlMethod, Mapping> registry = new HashMap<>();

        ControllerUtils.getAnnotedMethods("mg.itu.controller", registry);

        ServletContext context = sce.getServletContext();
        context.setAttribute("urlRegistry", registry);

        System.out.println("[SUCCESS] Scan terminé. Nombre de routes chargées : " + registry.size());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[INFO] Arrêt de l'application.");
    }
}