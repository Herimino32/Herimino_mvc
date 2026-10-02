package com.monframework.com.utils;

import java.lang.reflect.Field;

public class JsonUtils {

    public static String toJson(Object obj) throws Exception {
        if (obj == null) return "null";

        Class<?> clazz = obj.getClass();
        StringBuilder json = new StringBuilder("{");

        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);

            String key = field.getName();
            Object value = field.get(obj);

            json.append("\"").append(key).append("\":");

            if (value == null) {
                json.append("null");
            } else if (value instanceof String) {
                // Pour du texte, on ajoute des guillemets
                json.append("\"").append(value).append("\"");
            } else {
                // Pour les nombres, booléens, etc.
                json.append(value);
            }

            // Ajoute une virgule sauf pour le dernier champ
            if (i < fields.length - 1) {
                json.append(",");
            }
        }

        json.append("}");
        return json.toString();
    }
}