package com.jiangsonglin.pojoutils;

import java.util.List;

public interface Converter {
    Converter setName(String formName, String destName);

    <T, V> V converter(T fromObject, V destObject);

    <T, V> V converterByOrder(T fromObject, V destObject);

    <T, V> List<V> listConverter(List<T> formList, List<V> destList, Class<?> destClazz);

    void clear();
    void clearNameMap();
    void clearIgnoreNameSet();

    Converter setIgnoreName(String ignoreName);
}
