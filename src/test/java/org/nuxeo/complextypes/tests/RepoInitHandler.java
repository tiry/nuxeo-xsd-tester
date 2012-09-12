package org.nuxeo.complextypes.tests;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

public class RepoInitHandler extends DefaultRepositoryInit {

    @Override
    public void populate(CoreSession session) throws ClientException {
        super.populate(session);
        TestComplexTypesHandlingInJava.createDoc(session);
    }
}
