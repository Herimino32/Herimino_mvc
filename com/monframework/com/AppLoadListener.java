package com.monframework.com;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import com.monframework.com.utils.*;
import java.util.HashMap;

@WebListener // N'oublie pas l'annotation si tu ne la mets pas dans le web.xml
public class AppLoadListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[INFO] Tomcat démarre : Initialisation du dictionnaire de routes et configuration...");
        
        ServletContext context = sce.getServletContext();

        String packageToScan = context.getInitParameter("scan-package");
        String key_user = context.getInitParameter("user_key");

        // Vérification de la clé de session
        if (key_user == null || key_user.trim().isEmpty()) {
            System.err.println("[ERREUR] Paramètre 'user_key' non trouvé ou vide dans le web.xml !");
            return;
        }

        // Vérification du package à scanner
        if (packageToScan == null || packageToScan.trim().isEmpty()) {
            System.err.println("[ERREUR] Aucun package à scanner n'a été spécifié dans le web.xml ! (paramètre 'scan-package')");
            return;
        }

        HashMap<UrlMethod, Mapping> registry = new HashMap<>();

        try {
            ControllerUtils.getAnnotedMethods(packageToScan, registry);
        } catch (RuntimeException e) {
            System.err.println("[ERREUR CRITIQUE] Échec du scan des contrôleurs : " + e.getMessage());
            throw e;
        }

        // 🔑 Stockage dans le ServletContext pour un accès global unique
        context.setAttribute("urlRegistry", registry);
        context.setAttribute("user_key", key_user);

        System.out.println("[SUCCESS] Scan du package '" + packageToScan + "' | Routes chargées : " + registry.size());
        System.out.println("[SUCCESS] Clé de session enregistrée : '" + key_user + "'");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[INFO] Arrêt de l'application.");
    }
}