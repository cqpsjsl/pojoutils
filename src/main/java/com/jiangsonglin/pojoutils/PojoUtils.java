package com.jiangsonglin.pojoutils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author:jiangsonglin.com
 */

public class PojoUtils {
    // 日志门面
    final static Logger logger = LoggerFactory.getLogger(PojoUtils.class);

    private PojoUtils() {
    }

    public static Converter getConverter() {
        return new PojoConverter();
    }

    public static <T, V> Converter converter(T fromObject, V destObject) {
        PojoConverter pojoConverter = new PojoConverter();
        pojoConverter.converter(fromObject, destObject);
        return pojoConverter;
    }
    public static <T, V> Converter converterByOrder(T fromObject, V destObject) {
        PojoConverter pojoConverter = new PojoConverter();
        pojoConverter.converterByOrder(fromObject, destObject);
        return pojoConverter;
    }
    public static <T, V> Converter listConverter(List<T> formList, List<V> toList, Class<?> toClazz) {
        PojoConverter pojoConverter = new PojoConverter();
        pojoConverter.listConverter(formList,  toList, toClazz);
        return pojoConverter;
    }
    private static class PojoConverter implements Converter {

        // 装不一样的字段
        private HashMap<String, String> nameMap = new HashMap<>();
        // 设置忽略字段
        private HashSet<String> ignoreNameSet = new HashSet<>();

        private boolean reserve = false;


        /**
         * 单个bean转换
         *
         * @param fromObject 来源bean
         * @param destObject 转到目标bean
         * @param <T>
         * @param <V>
         * @return to
         */
        @Override
        public <T, V> V converter(T fromObject, V destObject) {
            // key  class
            HashMap<String, Class> map = new HashMap<>();

            Class<?> fromClass = fromObject.getClass();
            Field[] fromFiles = fromClass.getDeclaredFields();
            Class<?> toClass = destObject.getClass();
            Field[] destFields = toClass.getDeclaredFields();
            Field[] fields = destFields;
            HashMap<String, String> newNameMap = new HashMap<>();
            if (fromFiles.length < destFields.length) {
                // 扫描from,
                fields = fromFiles;
                // nameMap反转
                reserve = true;
                Iterator<String> iterator = nameMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    String value = nameMap.get(key);
                    newNameMap.put(value, key);
                }

            }

            for (Field field : fields) {
                field.setAccessible(true);
                String name = field.getName();
                // 获取替换名字
                String repName = reserve ? newNameMap.get(name) : name;
                name = repName == null ? name : repName;
                Class<?> type = field.getType();
                map.put(captureName(name), type);
            }
            // all fields name
            Set<String> set = map.keySet();
            Iterator<String> iterator = set.iterator();
            while (iterator.hasNext()) {
                String destMethodName = iterator.next(); // UserIds
                Object invoke = null;
                String lowerName = lowerName(destMethodName);
                // 忽略赋值
                if (ignoreNameSet.contains(lowerName)) {
                    //System.out.println(lowerName+"已忽略");
                    logger.info("【{}】已设置忽略", lowerName);
                    continue;
                }

                // isReg
                String formName = nameMap.get(lowerName);
                String finalMethodName = formName == null
                        ? destMethodName : captureName(formName);
                try {
                    String getName = "get" + finalMethodName;
                    try {
                        Method getMethod = fromClass.getDeclaredMethod(getName);
                        invoke = getMethod.invoke(fromObject);
                    } catch (NoSuchMethodException e) {

                        logger.info("{}这个方法没找到", getName);
                        String typeName = map.get(destMethodName).getName();
                        boolean aBoolean = isaBoolean(typeName);
                        if (aBoolean) {
                            String isName = "is" + finalMethodName;

                            logger.info("正在尝试寻找{}方法",isName);
                            try {
                                Method getMethod = fromClass.getDeclaredMethod(isName);
                                invoke = getMethod.invoke(fromObject);
                            }catch (NoSuchMethodException e2) {
                                logger.error("{}这个方法没找到", isName);
                            }

                        }
                    }
                    if (invoke != null) {
                        // get到值 开始注入vo
                        Class aClass = map.get(destMethodName);
                        String setName = "set" + destMethodName;
                        try {
                            Method setMethod = toClass.getDeclaredMethod(setName, aClass);
                            setMethod.invoke(destObject, new Object[]{invoke});
                        } catch (NoSuchMethodException e) {
                            // 看看是不是基本数据类型 再找一遍
                            aClass = basicDataType(map.get(destMethodName));
                            if (aClass != null) {
                                try {
                                    Method setMethod = toClass.getDeclaredMethod(setName, aClass);
                                    setMethod.invoke(destObject, new Object[]{invoke});
                                }catch (NoSuchMethodException e2) {
                                    logger.error("{}方法，参数类型是{}未成功，请检查此方法是否存在！！！",setName,aClass.getName());
                                }
                            }
                        }

                    }
                } catch ( IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                    // System.err.println(lowerName(finalMethodName)+"赋值给"+lowerName(destMethodName)+"过程失败!");
                    // e.printStackTrace();

                }
            }
            return destObject;
        }

        private boolean isaBoolean(String typeName) {
            return typeName.equals("boolean") || typeName.equals(Boolean.class.getName());
        }

        /**
         * 按照顺序从from给dest赋值，将从成员变量少的对象扫描 默认先从from遍历
         *
         * @param fromObject
         * @param destObject
         * @param <T>
         * @param <V>
         * @return
         */
        @Override
        public <T, V> V converterByOrder(T fromObject, V destObject) {
            LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();
            Class<?> fromObjectClass = fromObject.getClass();
            Field[] fromFields = fromObjectClass.getDeclaredFields();
            Class<?> destObjectClass = destObject.getClass();
            Field[] declaredFields = destObjectClass.getDeclaredFields();

            Field[] fields = fromFields;
            if (fromFields.length > declaredFields.length) {
                // 从成员变量少的开始遍历 避免NullPoint
                fields = declaredFields;
            }
            Class<?> getClazz = fromObjectClass; // 从from为标准 get
            Class<?> setClazz = destObjectClass; // 给 dest set
            for (int i = 0; i < fields.length; i++) {
                Field fromField = fromFields[i]; // get
                String captureName = getCaptureName(fromField);
                String getName = "get" + captureName;
                Object invoke = getMethodInvoke(fromObject, getClazz, fromField, captureName, getName);

                if (invoke != null) {
                    // 获取值成功 开始set

                    Field declaredField = declaredFields[i];
                    String captureDestName = getCaptureName(declaredField);
                    String setName = "set" + captureDestName;
                    try {
                        Method setMethod = setClazz.getDeclaredMethod(setName, declaredField.getType());
                        setMethod.setAccessible(true);
                        try {
                            setMethod.invoke(destObject, new Object[]{invoke});
                        } catch (IllegalAccessException e) {
                            logger.info("{}没有权限",setName);
                            //System.out.println(setName + "没有权限");
                        } catch (InvocationTargetException e) {
                            logger.info("{}: InvocationTargetException",setName);
                            //System.out.println(setName + "InvocationTargetException");
                        }
                    } catch (NoSuchMethodException e) {
                        logger.error("{}: 方法未找到",setName);
                        //System.err.println(setName + "方法未找到");
                    }

                }

            }

            return destObject;
        }

        /**
         * 获取调用方法的返回值
         *
         * @param fromObject
         * @param getClazz
         * @param fromField
         * @param captureName
         * @param getName
         * @param <T>
         * @return
         */
        private <T> Object getMethodInvoke(T fromObject, Class<?> getClazz, Field fromField, String captureName, String getName) {
            Object invoke = null;
            try {
                Method fromMethod = getClazz.getDeclaredMethod(getName);
                fromMethod.setAccessible(true);
                invoke = fromMethod.invoke(fromObject);
            } catch (NoSuchMethodException e) {
                // 方法没找到 判断是否是布尔
                if (isaBoolean(fromField.getType().getName())) {
                    String isGetName = "is" + captureName;
                    try {
                        Method fromMethod = getClazz.getDeclaredMethod(isGetName);
                        fromMethod.setAccessible(true);
                        invoke = fromMethod.invoke(fromObject);
                    } catch (NoSuchMethodException noSuchMethodException) {
                        logger.info("{} 或者 {} 方法没有找到",getName,isGetName);
                        //System.err.println(getName + "或者" + isGetName + "方法没有找到");
                        // noSuchMethodException.printStackTrace();
                    } catch (IllegalAccessException | InvocationTargetException illegalAccessException) {
                        // illegalAccessException.printStackTrace();
                    }
                }
                //e.printStackTrace();
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("调用{} 获取返回值异常",getName);
                //System.err.println("调用" + getName + "获取返回值异常");
                // e.printStackTrace();
            }
            return invoke;
        }

        private String getCaptureName(Field field) {
            Class<?> type = field.getType();
            String name = field.getName();
            String captureName = captureName(name);
            return captureName;
        }


        /**
         * List<User> ---> List<UserVo
         *
         * @param formList List<User>
         * @param toList   List<UserVo
         * @param toClazz  UserVo.class
         * @param <T>
         * @param <V>
         * @return
         */
        @Override
        public <T, V> List<V> listConverter(List<T> formList, List<V> toList, Class<?> toClazz) {

            for (T item : formList) {
                try {
                    Object o = toClazz.newInstance();
                    V v = (V) o;
                    converter(item, v);
                    toList.add(v);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    logger.error("{} 实例化失败！",toClazz.getName());
                    //System.err.println("实例化失败！");
                }

            }
            return toList;
        }

        /**
         * 清理nameMap
         */
        @Override
        public void clear() {
            clearNameMap();
            clearIgnoreNameSet();
        }

        @Override
        public void clearNameMap() {
            nameMap = new HashMap<>();

        }

        @Override
        public void clearIgnoreNameSet() {
            ignoreNameSet = new HashSet<>();
        }

        /**
         * @param formName id
         * @param destName userId
         * @return
         */
        @Override
        public Converter setName(String formName, String destName) {
            nameMap.put(destName, formName);
            return this;
        }

        /**
         * 按顺序注入该方法无效
         *
         * @param ignoreName
         * @return
         */
        @Override
        public Converter setIgnoreName(String ignoreName) {
            ignoreNameSet.add(ignoreName);
            return this;
        }

        private Class<?> basicDataType(Class clazz) {
            String name = clazz.getName();
            if (name.equals("boolean")) return Boolean.class;
            if (name.equals("byte")) return Byte.class;
            if (name.equals("char")) return Character.class;
            if (name.equals("short")) return Short.class;
            if (name.equals("int")) return Integer.class;
            if (name.equals("long")) return Long.class;
            if (name.equals("float")) return Float.class;
            if (name.equals("double")) return Double.class;

            if (name.equals(Boolean.class.getName())) return boolean.class;
            if (name.equals(Byte.class.getName())) return byte.class;
            if (name.equals(Character.class.getName())) return char.class;
            if (name.equals(Short.class.getName())) return short.class;
            if (name.equals(Integer.class.getName())) return int.class;
            if (name.equals(Long.class.getName())) return long.class;
            if (name.equals(Float.class.getName())) return float.class;
            if (name.equals(Double.class.getName())) return double.class;
            return null;
        }

        /**
         * 将字符串的首字母转大写
         *
         * @param str 需要转换的字符串
         * @return
         */
        private String captureName(String str) {
            // 进行字母的ascii编码前移，效率要高于截取字符串进行转换的操作
            char[] cs = str.toCharArray();
            cs[0] -= 32;
            return String.valueOf(cs);
        }

        private String lowerName(String str) {
            // 进行字母的ascii编码前移，效率要高于截取字符串进行转换的操作
            char[] cs = str.toCharArray();
            cs[0] += 32;
            return String.valueOf(cs);
        }


        private <V> Type getGenericType(List<V> toList) {
            Class<? extends List> listClass = toList.getClass();
            Field[] fields = listClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Type genericType = field.getGenericType();
                if (genericType == null) continue;
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType type = (ParameterizedType) genericType;
                    return type.getActualTypeArguments()[0];
                }
            }
            return null;
        }

    }
}
