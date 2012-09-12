/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.complextypes.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.IdRef;
import org.nuxeo.ecm.automation.client.model.PathRef;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(RestFeature.class)
@Jetty(port = 18080)
@RepositoryConfig(type = BackendType.H2, init = RepoInitHandler.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.testxsd", })
public class TestAutomationAPIRemoteRest {

    @Inject
    protected Session session;

    @Inject
    protected HttpAutomationClient client;

    @Test
    public void sampleAutomationRemoteAccess() throws Exception {

        // the repository init handler sould have created a sample doc in the
        // repo

        // check that we see it

        Document testDoc = (Document) session.newRequest(
                DocumentService.GetDocumentChild).setInput(new PathRef("/")).set(
                "name", "testDoc").execute();

        assertNotNull(testDoc);

        // try to see what's in it

        // check dublincore.title
        assertEquals("testDoc", testDoc.getTitle());
        assertEquals("testDoc", testDoc.getProperties().get("dc:title"));

        // schema only a subset of the properties are serialized with default
        // configuration (common and dublincore only)
        // see @Constants.HEADER_NX_SCHEMAS

        assertNull(testDoc.getProperties().get("ds:tableName"));
        assertNull(testDoc.getProperties().get("ds:fields"));

        // refetch the doc, but with the correct header
        testDoc = (Document) session.newRequest(
                DocumentService.GetDocumentChild).setHeader(
                "X-NXDocumentProperties", "*").setInput(new PathRef("/")).set(
                "name", "testDoc").execute();

        assertNotNull(testDoc);

        assertEquals("testDoc", testDoc.getTitle());
        assertEquals("MyTable", testDoc.getProperties().get("ds:tableName"));
        assertNotNull(testDoc.getProperties().get("ds:fields"));

        PropertyList dbFields = testDoc.getProperties().getList("ds:fields");
        assertEquals(5, dbFields.size());

        PropertyMap dbField0 = dbFields.getMap(0);
        assertNotNull(dbField0);
        assertEquals("field0", dbField0.getString("name"));

        assertEquals("Decision", dbField0.getList("roles").getString(0));
        assertEquals("Score", dbField0.getList("roles").getString(1));

        // now update the doc
        Map<String, Object> updateProps = new HashMap<String, Object>();

        updateProps.put("ds:tableName", "newTableName");
        updateProps.put("ds:attachments", "new1,new2,new3,new4");

        // send the fields representation as json
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("fields.json");
        assertNotNull(fieldAsJsonFile);
        String fieldsDataAsJSon = FileUtils.readFile(fieldAsJsonFile);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        updateProps.put("ds:fields", fieldsDataAsJSon);

        // XXX this should be directly managed by PropertyMap
        // => will be fixed in 5.7

        // if fields contains a roles[], unmarshaling will fail on the server
        // side in ComplexTypeJSONDecoding because the roles[] item fields is
        // wrongly considered as complex because of the restriction
        // => to be fixed in 5.7 too

        testDoc = (Document) session.newRequest(UpdateDocument.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").setInput(
                new IdRef(testDoc.getId())).set("properties",
                new PropertyMap(updateProps).toString()).execute();

        // check the returned doc

        assertEquals("testDoc", testDoc.getTitle());
        assertEquals("newTableName",
                testDoc.getProperties().get("ds:tableName"));

        PropertyList atts = testDoc.getProperties().getList("ds:attachments");
        assertNotNull(atts);
        assertEquals(4, atts.size());
        assertEquals("new1", atts.getString(0));
        assertEquals("new4", atts.getString(3));

        dbFields = testDoc.getProperties().getList("ds:fields");
        assertEquals(2, dbFields.size());

        PropertyMap dbFieldA = dbFields.getMap(0);
        assertNotNull(dbFieldA);
        assertEquals("fieldA", dbFieldA.getString("name"));

    }
}
