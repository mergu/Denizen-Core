package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ReflectionHelper {

    private static final Map<Class, Map<String, Field>> cachedFields = new HashMap<>();

    private static final Map<Class, Map<String, MethodHandle>> cachedFieldSetters = new HashMap<>();

    public static void setFieldValue(Class clazz, String fieldName, Object object, Object value) {
        try {
            getFields(clazz).get(fieldName).set(object, value);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }

    public static <T> T getFieldValue(Class clazz, String fieldName, Object object) {
        Map<String, Field> cache = getFields(clazz);
        try {
            Field field = cache.get(fieldName);
            if (field == null) {
                return null;
            }
            cache.put(fieldName, field);
            return (T) field.get(object);
        }
        catch (Exception ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    public static class CheckingFieldMap extends HashMap<String, Field> {

        public Class<?> clazz;

        public CheckingFieldMap(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Field get(Object name) {
            Field f = super.get(name);
            if (f == null) {
                String err = "Reflection field missing - Tried to read field '" + name + "' of class '" + clazz.getCanonicalName() + "'.";
                System.err.println("[Denizen] [ReflectionHelper]: " + err);
                Debug.echoError(err);
            }
            return f;
        }

        public Field getNoCheck(String name) {
            return super.get(name);
        }
    }

    public static Map<String, Field> getFields(Class clazz) {
        Map<String, Field> fields = cachedFields.get(clazz);
        if (fields != null) {
            return fields;
        }
        fields = new CheckingFieldMap(clazz);
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            fields.put(field.getName(), field);
        }
        cachedFields.put(clazz, fields);
        return fields;
    }

    public static Method getMethod(Class<?> clazz, String method, Class<?>... params) {
        Method f = null;
        try {
            f = clazz.getDeclaredMethod(method, params);
            f.setAccessible(true);
        }
        catch (Exception ex) {
            Debug.echoError(ex);
        }
        if (f == null) {
            String err = "Reflection method missing - Tried to read method '" + method + "' of class '" + clazz.getCanonicalName() + "'.";
            System.err.println("[Denizen] [ReflectionHelper]: " + err);
            Debug.echoError(err);
        }
        return f;
    }

    public static MethodHandle getMethodHandle(Class<?> clazz, String method, Class<?>... params) {
        try {
            return LOOKUP.unreflect(getMethod(clazz, method, params));
        }
        catch (Exception ex) {
            Debug.echoError(ex);
        }
        return null;
    }

    public static MethodHandle getFinalSetter(Class<?> clazz, String field) {
        Map<String, MethodHandle> map = cachedFieldSetters.computeIfAbsent(clazz, k -> new HashMap<>());
        MethodHandle result = map.get(field);
        if (result != null) {
            return result;
        }
        Field f = getFields(clazz).get(field);
        if (f == null) {
            return null;
        }
        int mod = f.getModifiers();
        try {
            if (MODIFIERS_FIELD == null) {
                validateUnsafe();
                boolean isStatic = Modifier.isStatic(mod);
                long offset = (long) (isStatic ? UNSAFE_STATIC_FIELD_OFFSET.invoke(f) : UNSAFE_FIELD_OFFSET.invoke(f));
                result = isStatic ? MethodHandles.insertArguments(UNSAFE_PUT_OBJECT, 0, clazz, offset)
                        : MethodHandles.insertArguments(UNSAFE_PUT_OBJECT, 1, offset);
            }
            else {
                if (Modifier.isFinal(mod)) {
                    MODIFIERS_FIELD.setInt(f, mod & ~Modifier.FINAL);
                }
                result = LOOKUP.unreflectSetter(f);
            }
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
        if (result == null) {
            return null;
        }
        cachedFieldSetters.get(clazz).put(field, result);
        return result;
    }

    private static void validateUnsafe() {
        if (UNSAFE == null) {
            try {
                UNSAFE = getFields(Class.forName("sun.misc.Unsafe")).get("theUnsafe");
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            UNSAFE_STATIC_FIELD_OFFSET = getMethodHandle(UNSAFE.getClass(), "staticFieldOffset", Field.class).bindTo(UNSAFE);
            UNSAFE_FIELD_OFFSET = getMethodHandle(UNSAFE.getClass(), "objectFieldOffset", Field.class).bindTo(UNSAFE);
            UNSAFE_PUT_OBJECT = getMethodHandle(UNSAFE.getClass(), "putObject", Object.class, long.class, Object.class).bindTo(UNSAFE);
        }
    }

    public static void giveReflectiveAccess(Class<?> from, Class<?> to) {
        try {
            if (GET_MODULE == null) {
                Class<?> module = Class.forName("java.lang.Module");
                GET_MODULE = Class.class.getMethod("getModule");
                ADD_OPENS = module.getMethod("addOpens", String.class, module);
            }
            ADD_OPENS.invoke(GET_MODULE.invoke(from), from.getPackage().getName(), GET_MODULE.invoke(to));
        }
        catch (Exception e) {
        }
    }

    static {
        giveReflectiveAccess(Field.class, ReflectionHelper.class);
        MODIFIERS_FIELD = ((CheckingFieldMap) getFields(Field.class)).getNoCheck("modifiers");
    }

    private static Method ADD_OPENS;
    private static Method GET_MODULE;
    private static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static Field MODIFIERS_FIELD;
    private static Object UNSAFE;
    private static MethodHandle UNSAFE_FIELD_OFFSET;
    private static MethodHandle UNSAFE_PUT_OBJECT;
    private static MethodHandle UNSAFE_STATIC_FIELD_OFFSET;
}
