package com.sky.anno;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)//加载方法上
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    OperationType value();
}
