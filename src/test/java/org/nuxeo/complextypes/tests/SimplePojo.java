package org.nuxeo.complextypes.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

public class SimplePojo {

    protected String type = "DataSet";

    protected String title;

    protected String dcDescription;

    protected String[] subjects;

    protected List<String> contributors;

    protected List<Field> dsFields;

    protected Calendar dcModified = Calendar.getInstance();

    protected String dublincoreSource;

    protected Blob fileContent;

    public SimplePojo() {
    }

    public void initField() {
        dsFields = new ArrayList<SimplePojo.Field>();
        for (int i = 0; i < 5; i++) {
            SimplePojo.Field field = new SimplePojo.Field();
            field.name = "field" + i;
            field.description = "description" + i;
            field.fieldType = "fieldType" + i;
            field.columnName = "col" + i;
            field.sqlTypeHint = "hint" + i;
            dsFields.add(field);
        }
        subjects = new String[] { "foo", "bar" };
        title = "simplePojo";
        dcDescription = "a simple Pojo";
        contributors = Arrays.asList(new String[] { "toto", "titi" });
        dublincoreSource = "tests";
        fileContent = new StringBlob("SomeFakeContent");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDcDescription() {
        return dcDescription;
    }

    public void setDcDescription(String dcDescription) {
        this.dcDescription = dcDescription;
    }

    public String[] getSubjects() {
        return subjects;
    }

    public void setSubjects(String[] subjects) {
        this.subjects = subjects;
    }

    public Calendar getDcModified() {
        return dcModified;
    }

    public void setDcModified(Calendar dcModified) {
        this.dcModified = dcModified;
    }

    public Blob getFileContent() {
        return fileContent;
    }

    public void setFileContent(Blob fileContent) {
        this.fileContent = fileContent;
    }

    public String getDublincoreSource() {
        return dublincoreSource;
    }

    public void setDublincoreSource(String dublincoreSource) {
        this.dublincoreSource = dublincoreSource;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getContributors() {
        return contributors;
    }

    public void setContributors(List<String> contributors) {
        this.contributors = contributors;
    }

    public List<Field> getDsFields() {
        return dsFields;
    }

    public void setDsFields(List<Field> dsFields) {
        this.dsFields = dsFields;
    }

    public class Field {

        protected String name;

        protected String fieldType;

        protected String description;

        protected String columnName;

        protected String sqlTypeHint;

        protected String[] roles = { "roleA", "roleB" };

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFieldType() {
            return fieldType;
        }

        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getSqlTypeHint() {
            return sqlTypeHint;
        }

        public void setSqlTypeHint(String sqlTypeHint) {
            this.sqlTypeHint = sqlTypeHint;
        }

        public String[] getRoles() {
            return roles;
        }

        public void setRoles(String[] roles) {
            this.roles = roles;
        }

    }
}
