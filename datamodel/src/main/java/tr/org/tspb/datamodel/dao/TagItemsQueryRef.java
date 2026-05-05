package tr.org.tspb.datamodel.dao;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import org.bson.Document;
import org.bson.types.ObjectId;
import tr.org.tspb.constants.ProjectConstants;
import static tr.org.tspb.constants.ProjectConstants.pattern_fms_crud;
import static tr.org.tspb.constants.ProjectConstants.pattern_fms_filter;
import tr.org.tspb.datamodel.expected.FmsScriptRunner;
import tr.org.tspb.datamodel.tags.FmsQuery;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class TagItemsQueryRef {

    /*
    
    "ref-value": {
        "db": "uysdb",
        "itemTable": "common",
        "query": {
            "_id": "fms_code{{filter_member}}"
        },
        "projection": "memberType"
    }
    
     */
    private String db;
    private String table;
    private String projection;
    private Document query;
    private FmsScriptRunner fmsScriptRunner;
    private MyField myField;

    public TagItemsQueryRef(Document ref, Map filter,
            FmsScriptRunner fmsScriptRunner, ObjectId loginMemberId,
            MyField myField, MyMap crudObject) {

        this.fmsScriptRunner = fmsScriptRunner;

        this.db = ref.get("db", String.class);
        this.table = ref.get("itemTable", String.class);
        this.projection = ref.get("projection", String.class);

        this.query = new Document();
        Document query_ = ref.get("query", Document.class);

        List<Document> listOfFilter = query_.get("list", List.class);

        if (listOfFilter != null) {
            this.query = FmsQuery.buildListQuery(listOfFilter, filter,
                    fmsScriptRunner, loginMemberId, myField, crudObject);
        } else {
            for (String key : query_.keySet()) {
                Object value = query_.get(key);
                if (value instanceof String) {
                    String resolvedValue = value.toString();
                    Matcher m;
                    if ((m = pattern_fms_crud.matcher(resolvedValue)).find()) {
                        throw new RuntimeException(resolvedValue.concat(
                                " is not supported"));
                    } else if ((m = pattern_fms_filter.matcher(resolvedValue)).
                            find()) {
                        value = filter == null ? null : filter.get(m.group(1));
                    } else {
                        switch (resolvedValue) {
                            case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_FILTER_MEMBER:
                                value = filter.get("member");
                                break;
                            default:
                        }
                    }
                }
                this.query.put(key, value == null ? "no result" : value);
            }
        }
    }

    public Object value() {
        Document ref = fmsScriptRunner.findOne(db, table, query);
        return ref == null ? "no result" : ref.get(projection);
    }

    public List<ObjectId> values() {
        return fmsScriptRunner.findObjectIds(db, table, query, projection);
    }

}
