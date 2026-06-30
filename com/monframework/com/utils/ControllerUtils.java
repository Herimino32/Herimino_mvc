package com.monframework.com.utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.lang.reflect.Method;

import com.monframework.com.annotation.Controller;
import com.monframework.com.annotation.MethodAnnotation;

public class ControllerUtils {

    private static List<Class<?>> findClasses(String packageName) {
        List<Class<?>> classes = new ArrayList<>();

        String path = packageName.replace(".", "/");

        try {
            ClassLoader classLoader = ControllerUtils.class.getClassLoader();
            URL resource = classLoader.getResource(path);

            if (resource == null) {
                return classes;
            }

            File directory = new File(resource.toURI());

            if (directory.exists() && directory.isDirectory()) {
                for (File file : directory.listFiles()) {

                    if (file.getName().endsWith(".class")) {
                        String className = packageName + "."
                                + file.getName().substring(0, file.getName().length() - 6);

                        classes.add(Class.forName(className));
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Erreur lors du scan : " + packageName,
                    e);
        }

        return classes;
    }

    public static List<Class<?>> getController(String packageName) {
        List<Class<?>> classes = findClasses(packageName);
        List<Class<?>> controllerNames = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                controllerNames.add(clazz);
            }
        }
        return controllerNames;
    }

    public static HashMap<UrlMethod, Mapping> getAnnotedMethods(String packageName) {
        HashMap<UrlMethod, Mapping> registry = new HashMap<>();
        List<Class<?>> classes = findClasses(packageName);
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(MethodAnnotation.class)) {
                    MethodAnnotation annotation = method.getAnnotation(MethodAnnotation.class);
                    String url = annotation.url();
                    String methodHttp = annotation.method();
                    UrlMethod key = new UrlMethod(url, methodHttp);
                    Mapping value = new Mapping(clazz.getName(), method.getName());
                    if (registry.containsKey(key)) {
                        throw new RuntimeException("Erreur : La route [" + methodHttp.toUpperCase() + " /" + url
                                + "] est déjà enregistrée par une autre méthode ! Conflit détecté au démarrage.");
                    }
                    registry.put(key, value);
                }
            }
        }
        return registry;
    }
}