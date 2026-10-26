package com.monframework.com;
public class MySession {
    private jakarta.servlet.http.HttpSession session;

    public MySession(jakarta.servlet.http.HttpSession session) {
        this.session = session;
    }

    public void set(String key, Object value) {
        session.setAttribute(key, value);
    }

    public Object get(String key) {
        return session.getAttribute(key);
    }
    
    public void delete(String key) {
        session.removeAttribute(key);
    }
}