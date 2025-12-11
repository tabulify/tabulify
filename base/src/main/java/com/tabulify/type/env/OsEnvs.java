package com.tabulify.type.env;

import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

/**
 * Class to add and remove environment variable
 * for test purpose only
 *
 * @deprecated since java 9, you can't inject env anymore in the jdk,
 * you should have a map in your app that represents the env and inject it as construct time
 */
@Deprecated
public class OsEnvs {


    /**
     * <a href="https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java">...</a>
     *
     * @param key   - the new environment set
     * @param value - the value
     */
    public static void add(String key, String value) {
        try {

            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");

            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);

            // Get the value
            Map<String, String> envField;
            try {
                envField = Casts.castToNewMap(theEnvironmentField.get(null), String.class, String.class);
            } catch (CastException e) {
                throw new InternalException("Should not throw as every object as a string representation", e);
            }
            envField.put(key, value);

            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> caseInsensitiveEnv;
            try {
                caseInsensitiveEnv = Casts.castToSameMap(theCaseInsensitiveEnvironmentField.get(null), String.class, String.class);
            } catch (CastException e) {
                throw new InternalException("Should not throw as every object as a string representation", e);
            }
            caseInsensitiveEnv.put(key, value);

        } catch (NoSuchFieldException e) {

            Class<?>[] classes = Collections.class.getDeclaredClasses();

            Map<String, String> sysEnv = System.getenv();
            for (Class<?> cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field;
                    try {
                        field = cl.getDeclaredField("m");
                    } catch (NoSuchFieldException e1) {
                        throw new RuntimeException(e);
                    }
                    field.setAccessible(true);
                    Object obj;
                    try {
                        obj = field.get(sysEnv);
                    } catch (IllegalAccessException e1) {
                        throw new RuntimeException(e);
                    }
                    Map<String, String> map;
                    try {
                        map = Casts.castToSameMap(obj, String.class, String.class);
                    } catch (CastException ex) {
                        throw new InternalException("Should not throw as every object as a string representation", e);
                    }
                    map.clear();
                    map.put(key, value);
                }
            }

        } catch (ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <a href="https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java">...</a>
     *
     * @param key - the new environment set
     */
    public static void remove(String key) {
        try {

            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");

            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);

            // Get the value
            Map<String, String> envField;
            try {
                envField = Casts.castToNewMap(theEnvironmentField.get(null), String.class, String.class);
            } catch (CastException e) {
                throw new InternalException("Should not throw as every object as a string representation", e);
            }
            envField.remove(key);

            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> caseInsensitiveEnv;
            try {
                caseInsensitiveEnv = Casts.castToSameMap(theCaseInsensitiveEnvironmentField.get(null), String.class, String.class);
            } catch (CastException e) {
                throw new InternalException("Should not throw as every object as a string representation", e);
            }
            caseInsensitiveEnv.remove(key);

        } catch (NoSuchFieldException e) {

            Class<?>[] classes = Collections.class.getDeclaredClasses();

            Map<String, String> sysEnv = System.getenv();
            for (Class<?> cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field;
                    try {
                        field = cl.getDeclaredField("m");
                    } catch (NoSuchFieldException e1) {
                        throw new RuntimeException(e);
                    }
                    field.setAccessible(true);
                    Object obj;
                    try {
                        obj = field.get(sysEnv);
                    } catch (IllegalAccessException e1) {
                        throw new RuntimeException(e);
                    }
                    Map<String, String> map;
                    try {
                        map = Casts.castToNewMap(obj, String.class, String.class);
                    } catch (CastException ex) {
                        // string, string cast should not be a problem
                        throw new RuntimeException(ex.getMessage(), e);
                    }
                    map.remove(key);
                }
            }

        } catch (ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getAll() {
        return System.getenv();
    }


    /**
     * A wrapper around {@link System#getenv(String)}
     *
     * @param name the name of the environment variable
     * @return the string value of the variable, or <code>null</code>
     * if the variable is not defined in the system environment
     */
    @SuppressWarnings("unused")
    public static String getEnv(String name) {
        return System.getenv(name);
    }

    @SuppressWarnings("unused")
    public static String getEnvOrDefault(String name, String def) {
        String value = System.getenv(name);
        if (value != null) {
            return value;
        }
        return def;
    }


}
