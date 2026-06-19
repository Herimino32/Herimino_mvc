package com.monframework.com.annotation;
import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodAnnotation {
    String url();
}
