package org.nuxeo.complextypes.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.core.mapping.DocumentModelMapper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.testxsd")
public class ObjectMappingTests {

    @Inject
    CoreSession session;

    protected String mkString(String[] values) {
        StringBuffer sb = new StringBuffer();
        for (String value : values) {
            sb.append(value);
            sb.append("|");
        }
        return sb.toString();
    }

    protected String mkString(List<String> values) {
        StringBuffer sb = new StringBuffer();
        for (String value : values) {
            sb.append(value);
            sb.append("|");
        }
        return sb.toString();
    }

    @Test
    public void shouldCreateDocumentFromPojo() throws Exception {

        SimplePojo pojo = new SimplePojo();
        pojo.initField();

        // create a Document from a Pojo
        DocumentModel doc = DocumentModelMapper.toDocumentModel(session, pojo);

        assertNotNull(doc);
        // check type
        assertEquals(pojo.getType(), doc.getType());

        // check scalar properties
        assertEquals(pojo.getTitle(), doc.getTitle());
        assertEquals(pojo.getDcDescription(),
                doc.getPropertyValue("dc:description"));
        assertEquals(pojo.getDcModified(), doc.getPropertyValue("dc:modified"));

        // check lists
        assertEquals(mkString(pojo.getSubjects()),
                mkString((String[]) doc.getPropertyValue("dc:subjects")));
        assertEquals(mkString(pojo.getContributors()),
                mkString((String[]) doc.getPropertyValue("dc:contributors")));

        // check complex
        List<Map<String, Object>> fields = (List<Map<String, Object>>) doc.getPropertyValue("ds:fields");
        assertNotNull(fields);

        assertEquals(5, fields.size());

        Map<String, Object> field = fields.get(0);
        assertNotNull(field);

        assertEquals("field0", field.get("name"));
        assertEquals("description0", field.get("description"));
        assertEquals("col0", field.get("columnName"));

        // persists doc to be sure
        doc = session.createDocument(doc);

        // re-run checks
        assertEquals(pojo.getTitle(), doc.getTitle());
        assertEquals(pojo.getDcDescription(),
                doc.getPropertyValue("dc:description"));
        assertEquals(pojo.getDcModified(), doc.getPropertyValue("dc:modified"));

        // check lists
        assertEquals(mkString(pojo.getSubjects()),
                mkString((String[]) doc.getPropertyValue("dc:subjects")));
        assertEquals(mkString(pojo.getContributors()),
                mkString((String[]) doc.getPropertyValue("dc:contributors")));

        // check complex
        fields = (List<Map<String, Object>>) doc.getPropertyValue("ds:fields");
        assertNotNull(fields);

        assertEquals(5, fields.size());

        field = fields.get(0);
        assertNotNull(field);

        assertEquals("field0", field.get("name"));
        assertEquals("description0", field.get("description"));
        assertEquals("col0", field.get("columnName"));

    }

    @Test
    public void shouldCreatePojoFromDocument() throws Exception {

        SimplePojo pojo = new SimplePojo();
        pojo.initField();

        // create a Document from a Pojo
        DocumentModel doc = DocumentModelMapper.toDocumentModel(session, pojo);
        assertNotNull(doc);

        // persists doc to be sure
        doc = session.createDocument(doc);

        SimplePojo reconstructedPojo = DocumentModelMapper.toBean(doc,
                SimplePojo.class);
        assertNotNull(reconstructedPojo);

        assertEquals(pojo.getTitle(), reconstructedPojo.getTitle());

        assertEquals(pojo.getDcDescription(),
                reconstructedPojo.getDcDescription());

        assertEquals(mkString(pojo.getSubjects()),
                mkString(reconstructedPojo.getSubjects()));

        // assertEquals(mkString(pojo.getContributors()),
        // mkString(reconstructedPojo.getContributors()));

    }
}
