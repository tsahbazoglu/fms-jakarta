package tr.org.tspb.datamodel.dao;

import java.io.Serializable;
import java.util.List;

public class AdditionalRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String uri;
    private String method;
    private String db;
    private String op;
    private Description desc;

    // Package-private or public constructor accepting the Builder state
    private AdditionalRow(Builder builder) {
        this.type = builder.type;
        this.uri = builder.uri;
        this.method = builder.method;
        this.db = builder.db;
        this.op = builder.op;
        this.desc = builder.desc;
    }

    // Default constructor kept for standard JSON-B/Jackson deserialization requirements
    public AdditionalRow() {
    }

    // --- Nested Builder Class ---
    public static class Builder {
        private String type;
        private String uri;
        private String method;
        private String db;
        private String op;
        private Description desc;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder db(String db) {
            this.db = db;
            return this;
        }

        public Builder op(String op) {
            this.op = op;
            return this;
        }

        public Builder desc(List<String> methods) {
            if (methods != null) {
                Description description = new Description();
                description.setMethod(methods);
                this.desc = description;
            }
            return this;
        }

        public AdditionalRow build() {
            return new AdditionalRow(this);
        }
    }

    // --- Inner Helper Class for Description ---
    public static class Description implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<String> method;

        public Description() {}

        public List<String> getMethod() { return method; }
        public void setMethod(List<String> method) { this.method = method; }
    }

    // --- Getters (Setters are no longer strictly needed if immutable, but can be kept) ---
    public String getType() { return type; }
    public String getUri() { return uri; }
    public String getMethod() { return method; }
    public String getDb() { return db; }
    public String getOp() { return op; }
    public Description getDesc() { return desc; }
}