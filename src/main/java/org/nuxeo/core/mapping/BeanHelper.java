package org.nuxeo.core.mapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;

public class BeanHelper {

    protected static String capitalize(String name) {
        // name = name.toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    protected static List<String> getMethodNames(String start,
            String schemaName, String prefix, Field field) {

        List<String> possibleNames = new ArrayList<String>();

        possibleNames.add(start + capitalize(schemaName)
                + capitalize(field.getName().getLocalName()));
        if (prefix != null && !prefix.isEmpty() && !prefix.equals(schemaName)) {
            possibleNames.add(start + capitalize(prefix)
                    + capitalize(field.getName().getLocalName()));
        }
        possibleNames.add(start + capitalize(field.getName().getLocalName()));

        return possibleNames;
    }

    protected static Object getBeanValue(Object bean, String schemaName,
            String prefix, Field field) throws Exception {

        for (String methodName : getMethodNames("get", schemaName, prefix,
                field)) {
            try {
                Method getter = bean.getClass().getMethod(methodName);
                if (getter != null) {
                    return getter.invoke(bean);
                }
            } catch (NoSuchMethodException e) {
                // NOP
            }
        }
        return null;
    }

    public static void applyValueToBean(Object bean, String schemaName,
            Field field, Object value) {

        List<Method> methods = Arrays.asList(bean.getClass().getMethods());

        List<Method> possibleMethods = new ArrayList<Method>();

        for (String methodName : getMethodNames("set", schemaName,
                field.getName().getPrefix(), field)) {

            for (Method method : methods) {
                if (method.getName().equalsIgnoreCase(methodName)) {
                    possibleMethods.add(method);
                }
            }
        }

        if (possibleMethods.size() > 0) {
            // XXX should do better
            Method targetMethod = possibleMethods.get(0);

            if (value == null) {
                try {
                    targetMethod.invoke(bean, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            if (targetMethod.getParameterTypes().length == 1) {
                Class<?> valueType = targetMethod.getParameterTypes()[0];
                if (valueType.getName().equals(value.getClass().getName())) {
                    try {
                        targetMethod.invoke(bean, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // need conversion / mapping
                    if (field.getType().isSimpleType()) {
                        if (valueType.equals(String.class)) {
                            try {
                                targetMethod.invoke(bean,
                                        field.getType().encode(value));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (valueType.equals(Date.class)
                                && value instanceof Calendar) {
                            try {
                                targetMethod.invoke(bean,
                                        ((Calendar) value).getTime());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    } else if (field.getType().isListType()) {

                        if (valueType.equals(List.class)) {
                            List list = new ArrayList();
                        }

                        ListType ltype = (ListType) (field.getType());
                        if (ltype.isComplexType()) {

                        }

                    } else if (field.getType().isComplexType()) {
                        if (valueType.isAssignableFrom(Map.class)) {
                            try {
                                targetMethod.invoke(bean,
                                        field.getType().encode(value));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {

                        }
                    }
                }
            }
        }

    }
}
