package org.nuxeo.core.mapping;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

public class DocumentModelMapper {

    protected static String getDocType(Object bean) throws Exception {

        try {
            Method getType = bean.getClass().getMethod("getType");
            return (String) getType.invoke(bean);
        } catch (NoSuchMethodException e) {
            try {
                Method get = bean.getClass().getMethod("get", String.class);
                if (get != null) {
                    return (String) get.invoke(bean, "type");
                }
            } catch (NoSuchMethodException e1) {
                return null;
            }
        }
        return null;
    }

    public static DocumentModel toDocumentModel(CoreSession session, Object bean)
            throws Exception {

        String docType = getDocType(bean);
        if (docType != null) {
            DocumentModel doc = session.createDocumentModel(docType);
            fillDocumentModel(bean, doc);
            return doc;
        }

        return null;
    }

    public static <T> T toBean(DocumentModel doc, Class<T> beanClass) {
        try {
            T bean = beanClass.newInstance();
            fillBean(doc, bean);
            return bean;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static Object buildScalar(Type fieldType, Object value) {
        if (value instanceof String) {
            return fieldType.decode((String) value);
        } else {
            return value;
        }
    }

    protected static List<Object> buildList(ListType listType, Object value) {

        List<Object> result = new ArrayList<Object>();

        if (value instanceof String[]) {
            for (String v : (String[]) value) {
                result.add(v);
            }
        } else if (value instanceof Object[]) {
            for (Object v : (Object[]) value) {
                result.add(buildField(listType.getField().getType(), v));
            }
        } else if (value instanceof List<?>) {
            for (Object v : (List<?>) value) {
                result.add(buildField(listType.getField().getType(), v));
            }
        } else if (value instanceof Iterable<?>) {
            Iterator<?> it = ((Iterable<?>) value).iterator();
            while (it.hasNext()) {
                Object v = it.next();
                result.add(buildField(listType.getField().getType(), v));
            }
        }
        return result;
    }

    protected static Map<String, Object> buildComplex(ComplexType complexType,
            Object value) {

        Map<String, Object> map = new HashMap<String, Object>();

        if (value instanceof Map<?, ?>) {
            for (Field field : complexType.getFields()) {
                Object v = ((Map<String, Object>) value).get(field.getName().getLocalName());
                map.put(field.getName().getLocalName(), v);
            }
        } else {
            for (Field field : complexType.getFields()) {
                try {
                    Method m = value.getClass().getMethod(
                            "get"
                                    + BeanHelper.capitalize(field.getName().getLocalName()));
                    Object v = m.invoke(value);
                    map.put(field.getName().getLocalName(), v);
                } catch (NoSuchMethodException e) {
                    // NOP
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    protected static Object buildField(Type fieldType, Object v) {
        if (v instanceof Blob) {
            return v;
        }
        if (fieldType.isSimpleType()) {
            return buildScalar(fieldType, v);
        } else if (fieldType.isListType()) {
            return buildList((ListType) fieldType, v);
        } else if (fieldType.isComplexType()) {
            return buildComplex((ComplexType) fieldType, v);
        } else {
            System.out.println("!! unknow type " + fieldType);
            return v;
        }
    }

    public static void fillDocumentModel(Object bean, DocumentModel doc)
            throws Exception {

        SchemaManager sm = Framework.getLocalService(SchemaManager.class);

        for (DataModel dataModel : doc.getDataModelsCollection()) {

            String schemaName = dataModel.getSchema();
            Schema schema = sm.getSchema(schemaName);
            String prefix = schema.getNamespace().prefix;

            for (Field field : schema.getFields()) {

                Object value = BeanHelper.getBeanValue(bean, schemaName,
                        prefix, field);

                dataModel.setValue(field.getName().getLocalName(),
                        buildField(field.getType(), value));
            }
        }
        // system properties

    }

    public static void fillBean(DocumentModel doc, Object bean)
            throws Exception {

        SchemaManager sm = Framework.getLocalService(SchemaManager.class);

        for (DataModel dataModel : doc.getDataModelsCollection()) {

            String schemaName = dataModel.getSchema();
            Schema schema = sm.getSchema(schemaName);

            for (Field field : schema.getFields()) {
                Serializable value = doc.getPropertyValue(field.getName().getPrefixedName());
                BeanHelper.applyValueToBean(bean, schemaName, field, value);
            }
        }

    }

}
