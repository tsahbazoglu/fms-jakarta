package tr.org.tspb.datamodel.pojo;

import java.io.Serializable;

public class FmsWorkbookColumnConverter implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String uri;
    private String op;

    // Default constructor required by JSON-B deserialization layers
    public FmsWorkbookColumnConverter() {
    }

    // --- Private Constructor for Builder Pattern ---
    private FmsWorkbookColumnConverter(Builder builder) {
        this.type = builder.type;
        this.uri = builder.uri;
        this.op = builder.op;
    }

    // --- Nested Fluent Builder Class ---
    public static class Builder {
        private String type;
        private String uri;
        private String op;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder op(String op) {
            this.op = op;
            return this;
        }

        public FmsWorkbookColumnConverter build() {
            return new FmsWorkbookColumnConverter(this);
        }
    }

    // --- Standard Getters ---
    public String getType() { return type; }
    public String getUri() { return uri; }
    public String getOp() { return op; }
}