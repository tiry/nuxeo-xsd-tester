package org.nuxeo.complextypes.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.server.jaxrs.io.writers.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestComplexTypesHandlingInJava extends SQLRepositoryTestCase {

    protected static String[] attachements = { "att1", "att2", "att3" };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // deploy me !
        deployBundle("org.nuxeo.testxsd");
        fireFrameworkStarted();
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.waitForAsyncCompletion();
        closeSession();
        super.tearDown();
    }

    @Test
    public void verifySchemaDeployed() throws Exception {

        // check schema registration
        SchemaManager sm = Framework.getService(SchemaManager.class);

        Schema dataset = sm.getSchema("dataset");
        assertNotNull(dataset);

        assertEquals(4, dataset.getFieldsCount());

        // check fields

        // database
        Field databaseField = dataset.getField("database");
        assertNotNull(databaseField);
        assertEquals(StringType.ID, databaseField.getType().getName());

        // tableName
        Field tableNameField = dataset.getField("tableName");
        assertNotNull(tableNameField);
        assertEquals(StringType.ID, tableNameField.getType().getName());

        // attachments
        Field attachmentsField = dataset.getField("attachments");
        assertNotNull(attachmentsField);
        assertTrue(attachmentsField.getType().isListType());
        ListType listType = (ListType) attachmentsField.getType();
        assertEquals(StringType.ID, listType.getFieldType().getName());

        // fields
        Field fieldsField = dataset.getField("fields");
        assertNotNull(fieldsField);
        assertTrue(fieldsField.getType().isListType());
        listType = (ListType) fieldsField.getType();
        assertTrue(listType.getFieldType().isComplexType());

        ComplexType ct = (ComplexType) listType.getFieldType();
        assertEquals(6, ct.getFieldsCount());

        // check subfields
        assertNotNull(ct.getField("name"));
        assertEquals(StringType.ID, ct.getField("name").getType().getName());

        assertNotNull(ct.getField("description"));
        assertEquals(StringType.ID,
                ct.getField("description").getType().getName());

        // ...

        assertNotNull(ct.getField("fieldType"));
        assertFalse(ct.getField("fieldType").getType().isComplexType());
        assertFalse(ct.getField("fieldType").getType().isListType());
        // in 5.6 restrictions are not loaded in type def !
        assertEquals(StringType.ID,
                ct.getField("fieldType").getType().getSuperType().getName());

        assertNotNull(ct.getField("roles"));
        assertFalse(ct.getField("roles").getType().isComplexType());
        assertTrue(ct.getField("roles").getType().isListType());

        ListType ctlt = (ListType) ct.getField("roles").getType();
        Type subCT = ctlt.getFieldType();
        assertFalse(subCT.isComplexType());
        // in 5.6 restrictions are not loaded in type def !
        assertEquals(StringType.ID, subCT.getSuperType().getName());
    }

    @Test
    public void verifyTypeDeployed() throws Exception {

        // check schema registration
        SchemaManager sm = Framework.getService(SchemaManager.class);

        DocumentType docType = sm.getDocumentType("DataSet");
        assertNotNull(docType);
        Schema schema = docType.getSchema("dataset");
        assertEquals("ds", schema.getNamespace().prefix);

    }

    public static DocumentModel createDoc(CoreSession session)
            throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "testDoc",
                "DataSet");

        doc.setPropertyValue("dc:title", "testDoc");

        // check Scalar properties
        doc.setPropertyValue("ds:database", "MyDataBase");
        doc.setPropertyValue("ds:tableName", "MyTable");

        // check simple string list
        doc.setPropertyValue("ds:attachments", attachements);

        // init list of complex type
        List<Map<String, Serializable>> fields = new ArrayList<Map<String, Serializable>>();

        for (int i = 0; i < 5; i++) {
            Map<String, Serializable> field = new HashMap<String, Serializable>();

            field.put("name", "field" + i);
            field.put("description", "desc field" + i);
            field.put("fieldType", "string");
            field.put("columnName", "col" + i);
            field.put("sqlTypeHint", "whatever");

            ArrayList<String> roles = new ArrayList<String>();
            roles.add("Decision");
            roles.add("Score");

            field.put("roles", roles);

            fields.add(field);
        }

        doc.setPropertyValue("ds:fields", (Serializable) fields);

        // create the doc for real
        doc = session.createDocument(doc);

        return doc;
    }

    @Test
    public void createDocAndFeedFromJava() throws Exception {

        DocumentModel doc = createDoc(session);

        // verify content
        assertEquals("MyDataBase", doc.getPropertyValue("ds:database"));
        assertEquals("MyTable", doc.getPropertyValue("ds:tableName"));

        List<String> atts = (List<String>) doc.getPropertyValue("ds:attachments");
        for (String a : attachements) {
            assertTrue(atts.contains(a));
        }

        List<Map<String, Serializable>> cols = (List<Map<String, Serializable>>) doc.getPropertyValue("ds:fields");
        assertNotNull(cols);

        assertEquals(5, cols.size());

        Map<String, Serializable> col = cols.get(0);
        assertEquals("field0", col.get("name"));
        ArrayList<String> roles = (ArrayList<String>) col.get("roles");
        assertTrue(roles.contains("Decision"));
        assertTrue(roles.contains("Score"));

    }

    @Test
    public void createDocAndDumpAsJSON() throws Exception {

        DocumentModel doc = createDoc(session);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] schemas = { "dataset" };
        JsonDocumentWriter.writeDocument(out, doc, schemas);

        System.out.println(out.toString());
    }

    @Test
    public void createDocAndSetPropertyFromJSON() throws Exception {

        File json = FileUtils.getResourceFileFromContext("complex.json");
        assertNotNull(json);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String>[] fieldArray = mapper.readValue(json, Map[].class);
        List<Map<String, String>> fields = new ArrayList<Map<String, String>>();

        for (Map<String, String> field : fieldArray) {
            fields.add(field);
        }

        DocumentModel doc = session.createDocumentModel("DataSet");
        doc.setPropertyValue("dc:title", "testDoc");

        doc.setPropertyValue("ds:fields", (Serializable) fields);

        doc = session.createDocument(doc);

        List<Map<String, Serializable>> cols = (List<Map<String, Serializable>>) doc.getPropertyValue("ds:fields");
        assertNotNull(cols);

        assertEquals(5, cols.size());

        Map<String, Serializable> col = cols.get(0);
        assertEquals("field0", col.get("name"));
        ArrayList<String> roles = (ArrayList<String>) col.get("roles");
        assertTrue(roles.contains("Decision"));
        assertTrue(roles.contains("Score"));

    }

    @Test
    public void createDocAndSetSchemaFromJSON() throws Exception {

        File json = FileUtils.getResourceFileFromContext("schema.json");
        assertNotNull(json);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> schemaValues = mapper.readValue(json, Map.class);

        DocumentModel doc = session.createDocumentModel("DataSet");
        doc.setPropertyValue("dc:title", "testDoc");
        doc.getDataModel("dataset").setMap(schemaValues);
        doc = session.createDocument(doc);

        List<Map<String, Serializable>> cols = (List<Map<String, Serializable>>) doc.getPropertyValue("ds:fields");
        assertNotNull(cols);

        assertEquals(5, cols.size());

        Map<String, Serializable> col = cols.get(0);
        assertEquals("field0", col.get("name"));
        ArrayList<String> roles = (ArrayList<String>) col.get("roles");
        assertTrue(roles.contains("Decision"));
        assertTrue(roles.contains("Score"));
    }

    @Test
    public void checkSubPropertiesAccess() throws Exception {

        DocumentModel doc = createDoc(session);

        // verify content
        assertEquals("MyDataBase", doc.getPropertyValue("ds:database"));
        assertEquals("MyTable", doc.getPropertyValue("ds:tableName"));

        String att1 = (String) doc.getPropertyValue("ds:attachments/0");
        assertEquals("att1", att1);

        String att2 = (String) doc.getPropertyValue("ds:attachments/1");
        assertEquals("att2", att2);

        String att3 = (String) doc.getPropertyValue("ds:attachments/item[2]");
        assertEquals("att3", att3);

        String name0 = (String) doc.getPropertyValue("ds:fields/0/name");
        assertEquals("field0", name0);

        String role10 = (String) doc.getPropertyValue("ds:fields/1/roles/0");
        assertEquals("Decision", role10);

        // now update properties

        // scalar
        doc.setPropertyValue("ds:tableName", "SuperTable");

        // list item
        doc.setPropertyValue("ds:attachments/0", "newAttachment");

        // sub item in a list of complex
        doc.setPropertyValue("ds:fields/0/name", "newName");

        // save back DocumentModel in the Repo
        doc = session.saveDocument(doc);

        // verify
        assertEquals("SuperTable", doc.getPropertyValue("ds:tableName"));

        assertEquals(doc.getPropertyValue("ds:attachments/0"), "newAttachment");

        List<String> atts = (List<String>) doc.getPropertyValue("ds:attachments");
        assertEquals(3, atts.size());
        assertTrue(atts.contains("att2"));
        assertTrue(atts.contains("att3"));
        assertFalse(atts.contains("att1"));

        name0 = (String) doc.getPropertyValue("ds:fields/0/name");
        assertEquals("newName", name0);
    }
}
