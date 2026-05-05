package tr.org.tspb.datamodel.dao;

import org.bson.Document;

/**
 *
 * @author telman
 */
public class CtrlItems {

    private final String configDefinitionKey;
    private final String db;
    private final String collection;
    private final Document query;
    private final String queryFunc;
    private final Document queryDoc;

    public CtrlItems(Document doc) {
        this.configDefinitionKey = doc.getString("config-definition-key");
        this.db = doc.getString("db");
        this.collection = doc.getString("collection");
        this.query = doc.get("query", Document.class);
        this.queryFunc = doc.getString("query-func");
        this.queryDoc = doc.get("query-doc", Document.class);

    }

    /**
     * @return the db
     */
    public String getDb() {
        return db;
    }

    /**
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @return the query
     */
    public Document getQuery() {
        return query;
    }

    /**
     * @return the queryFunc
     */
    public String getQueryFunc() {
        return queryFunc;
    }

    /**
     * @return the queryDoc
     */
    public Document getQueryDoc() {
        return queryDoc;
    }

    /**
     * @return the configDefinitionKey
     */
    public String getConfigDefinitionKey() {
        return configDefinitionKey;
    }

}
