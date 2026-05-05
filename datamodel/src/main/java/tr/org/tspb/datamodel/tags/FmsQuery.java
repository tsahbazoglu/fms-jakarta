package tr.org.tspb.datamodel.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.types.ObjectId;
import tr.org.tspb.constants.ProjectConstants;
import static tr.org.tspb.constants.ProjectConstants.DIEZ;
import static tr.org.tspb.constants.ProjectConstants.DOLAR;
import static tr.org.tspb.constants.ProjectConstants.DOLAR_IN;
import static tr.org.tspb.constants.ProjectConstants.DOLAR_NE;
import static tr.org.tspb.constants.ProjectConstants.DOLAR_REGEX;
import static tr.org.tspb.constants.ProjectConstants.MENU_ORDER;
import static tr.org.tspb.constants.ProjectConstants.REPLACEABLE_KEY_FMS_VALUE;
import static tr.org.tspb.constants.ProjectConstants.VALUE;
import static tr.org.tspb.constants.ProjectConstants.pattern_fms_crud;
import tr.org.tspb.datamodel.dao.MyField;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.datamodel.dao.TagItemsQueryRef;
import tr.org.tspb.datamodel.expected.FmsScriptRunner;
import tr.org.tspb.datamodel.pojo.RoleMap;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class FmsQuery {

    private static Document build(Document queryDoc, Map filter,
            FmsScriptRunner fmsScriptRunner, ObjectId loginMemberId,
            MyField myField, MyMap crudObject) {

        Document q = new Document();

        String key = queryDoc.get("key", String.class);

        Document refValue = queryDoc.get("ref-value", Document.class);
        Document inRef = queryDoc.get("in-ref", Document.class);
        String fmsValue = queryDoc.get(REPLACEABLE_KEY_FMS_VALUE, String.class);
        Boolean booleanValue = queryDoc.get("boolean-value", Boolean.class);
        String strValue = queryDoc.get("string-value", String.class);
        Number numberValue = queryDoc.get("number-value", Number.class);
        List<String> listOfString = queryDoc.
                getList("array-value", String.class);
        List<Number> listOfNumber = queryDoc.getList("array-number",
                Number.class);
        Document aggregateValue = queryDoc.
                get("aggregate-value", Document.class);

        if (aggregateValue != null) {
            q.
                    put(key, aggregate(aggregateValue, fmsScriptRunner,
                            loginMemberId));

        } else if (refValue != null) {
            q.put(key, new TagItemsQueryRef(refValue, filter, fmsScriptRunner,
                    loginMemberId,
                    null, null).value());
        } else if (inRef != null) {
            q.put(key, new Document(DOLAR_IN,
                    new TagItemsQueryRef(inRef, filter, fmsScriptRunner,
                            loginMemberId,
                            null, null).values()));
        } else if (fmsValue != null) {
            Matcher m;
            if ((m = pattern_fms_crud.matcher(fmsValue)).find()) {
                //throw new RuntimeException(fmsValue.concat(" is not supported"));
                q.put(key, crudObject == null ? null : crudObject.
                        get(m.group(1)));
            } else {
                switch (fmsValue) {
                    case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_FILTER_PERIOD:
                        q.put(key,
                                filter.get("period") == null ? "no result" : filter.
                                get("period"));
                        break;
                    case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_FILTER_TEMPLATE:
                        q.put(key,
                                filter.get("template") == null ? "no result" : filter.
                                get("template"));
                        break;
                    case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_LOGIN_MEMBER_ID:
                        q.put(key,
                                loginMemberId == null ? "no result" : loginMemberId);
                        break;
                    case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_THIS_FORM_KEY:
                        q.put(key, myField.getMyFormKey());
                        break;
                    default:
                        throw new RuntimeException(
                                "could not find replaceable word");
                }
            }

        } else if (booleanValue != null) {
            q.put(key, booleanValue);
        } else if (strValue != null) {
            q.put(key, strValue);
        } else if (numberValue != null) {
            q.put(key, numberValue);
        } else if (listOfString != null) {
            q.put(key, new Document(DOLAR_IN, listOfString));
        } else if (listOfNumber != null) {
            q.put(key, new Document(DOLAR_IN, listOfNumber));
        } else {
            String type = queryDoc.get("type", String.class);
            if (type == null) {
                type = "string";
            }
            switch (type) {
                case "number":
                    q.put(key, queryDoc.get(VALUE, Number.class));
                    break;
                case "string":
                    q.put(key, queryDoc.get(VALUE, String.class).
                            replaceAll(DIEZ, DOLAR));
                    break;
                case "in":
                    q.put(key, new Document(DOLAR_IN, Arrays.asList(queryDoc.
                            get(VALUE, String.class).
                            replaceAll(DIEZ, DOLAR).
                            split(","))));
                    break;
                case "ne":
                    q.put(key, new Document(DOLAR_NE, queryDoc.get(VALUE,
                            String.class).
                            replaceAll(DIEZ, DOLAR)));
                    break;
                case "regex":
                    q.put(key, new Document(DOLAR_REGEX, queryDoc.get(VALUE,
                            String.class).
                            replaceAll(DIEZ, DOLAR)));
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "field.items.query.type is not supported  : " + type);
            }
        }
        return q;
    }

    public static Document buildListQuery(List<Document> listOfFilter,
            Map filter,
            FmsScriptRunner fmsScriptRunner, ObjectId loginMemberId,
            MyField myField, MyMap crudObject) {
        Document result = new Document();
        if (listOfFilter != null) {
            for (Document doc : listOfFilter) {
                result.putAll(FmsQuery.build(doc, filter, fmsScriptRunner,
                        loginMemberId,
                        myField, crudObject));
            }
        }
        return result;
    }

    public static Document buildListQuery(List<Document> listOfFilter,
            Map filter,
            FmsScriptRunner fmsScriptRunner, ObjectId loginMemberId)
            throws RuntimeException {

        Document result = new Document();
        if (listOfFilter != null) {
            for (Document d : listOfFilter) {
                result.putAll(FmsQuery.build(d, filter, fmsScriptRunner,
                        loginMemberId,
                        null, null));
            }
        }
        return result;
    }

    public static Document buildListQueryAjax(List<Document> listOfFilter,
            Map filter,
            FmsScriptRunner fmsScriptRunner, ObjectId loginMemberId,
            MyMap crud, RoleMap roleMap) throws RuntimeException {

        Document result = new Document();

        Collections.sort(listOfFilter, new Comparator<Document>() {
            @Override
            public int compare(Document t1, Document t2) {
                Number menuOrder1 = (Number) t1.get("order");
                Number menuOrder2 = (Number) t2.get("order");
                int order1 = (menuOrder1 == null) ? 0 : menuOrder1.intValue();
                int order2 = (menuOrder2 == null) ? 0 : menuOrder2.intValue();
                return Integer.compare(order1, order2);
            }
        });

        for (Document d : listOfFilter) {
            result.putAll(FmsQuery.buildAjax(d, filter, fmsScriptRunner,
                    loginMemberId, crud, roleMap));
        }
        return result;
    }

    public static Document buildAjax(Document queryDoc, Map filter,
            FmsScriptRunner fmsScriptRunner, ObjectId loginMemberId,
            MyMap crud, RoleMap roleMap) {

        Document resolvedQuery = new Document();

        String key = queryDoc.get("key", String.class);

        List roles = queryDoc.getList("roles", String.class);
        if (roles != null && !roleMap.isUserInRole(roles)) {
            return new Document();
        }

        Document refValue = queryDoc.get("ref-value", Document.class);
        Document inRef = queryDoc.get("in-ref", Document.class);
        String fmsValue = queryDoc.get(REPLACEABLE_KEY_FMS_VALUE, String.class);
        Boolean booleanValue = queryDoc.get("boolean-value", Boolean.class);
        String strValue = queryDoc.get("string-value", String.class);
        Number numberValue = queryDoc.get("number-value", Number.class);
        List<String> listOfString = queryDoc.
                getList("array-value", String.class);
        List<Number> listOfNumber = queryDoc.getList("array-number",
                Number.class);

        if (refValue != null) {
            resolvedQuery.put(key,
                    new TagItemsQueryRef(refValue, filter, fmsScriptRunner,
                            loginMemberId,
                            null, crud).value());
        } else if (inRef != null) {
            resolvedQuery.put(key, new Document(DOLAR_IN,
                    new TagItemsQueryRef(inRef, filter, fmsScriptRunner,
                            loginMemberId,
                            null, crud).values()));
        } else if (fmsValue != null) {

            Matcher m = pattern_fms_crud.matcher(fmsValue);
            if (m.find()) {
                Object crudValue = crud.get(m.group(1));
                resolvedQuery.put(key,
                        crudValue == null ? "no result" : crudValue);
            } else {
                switch (fmsValue) {
                    case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_FILTER_PERIOD:
                        resolvedQuery.put(key,
                                filter.get("period") == null ? "no result" : filter.
                                get("period"));
                        break;
                    case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_FILTER_TEMPLATE:
                        resolvedQuery.put(key,
                                filter.get("template") == null ? "no result" : filter.
                                get("template"));
                        break;
                    case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_LOGIN_MEMBER_ID:
                        resolvedQuery.put(key,
                                loginMemberId == null ? "no result" : loginMemberId);
                        break;
                    default:
                        throw new RuntimeException(
                                "could not find replaceable word");
                }
            }

        } else if (booleanValue != null) {
            resolvedQuery.put(key, booleanValue);
        } else if (strValue != null) {
            resolvedQuery.put(key, strValue);
        } else if (numberValue != null) {
            resolvedQuery.put(key, numberValue);
        } else if (listOfString != null) {
            resolvedQuery.put(key, new Document(DOLAR_IN, listOfString));
        } else if (listOfNumber != null) {
            resolvedQuery.put(key, new Document(DOLAR_IN, listOfNumber));
        } else {
            String type = queryDoc.get("type", String.class);
            if (type == null) {
                type = "string";
            }
            switch (type) {
                case "number":
                    resolvedQuery.put(key, queryDoc.get(VALUE, Number.class));
                    break;
                case "string":
                    resolvedQuery.put(key, queryDoc.get(VALUE, String.class).
                            replaceAll(DIEZ, DOLAR));
                    break;
                case "in":
                    resolvedQuery.put(key, new Document(DOLAR_IN, Arrays.asList(
                            queryDoc.get(VALUE, String.class).
                                    replaceAll(DIEZ, DOLAR).
                                    split(","))));
                    break;
                case "ne":
                    resolvedQuery.put(key, new Document(DOLAR_NE, queryDoc.get(
                            VALUE, String.class).
                            replaceAll(DIEZ, DOLAR)));
                    break;
                case "regex":
                    resolvedQuery.put(key, new Document(DOLAR_REGEX, queryDoc.
                            get(VALUE, String.class).
                            replaceAll(DIEZ, DOLAR)));
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "field.items.query.type is not supported  : " + type);
            }
        }
        return resolvedQuery;
    }

    private static Object aggregate(Document aggregateValue,
            FmsScriptRunner fmsScriptRunner, ObjectId loginMemberId) {
        String db = aggregateValue.getString("db");
        String table = aggregateValue.getString("table");
        List<Document> pipline = aggregateValue.getList("pipline",
                Document.class);
        String projection = aggregateValue.getString("projection");

        List<Document> modifiedPipline = new ArrayList<>();

        for (Document doc : pipline) {
            Document matchDoc = doc.get("$match", Document.class);
            if (matchDoc != null) {
                for (String key : matchDoc.keySet()) {
                    Object value = matchDoc.get(key);
                    if (ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_LOGIN_MEMBER_ID.
                            equals(value)) {
                        matchDoc.put(key, loginMemberId);
                    } else {
                        matchDoc.put(key, value);
                    }
                }
            }
            modifiedPipline.add(doc);
        }

        List<Document> result = fmsScriptRunner.aggreagate(db, table, pipline);
        if (result.size() > 0) {
            return result.get(0).
                    get(projection);
        }
        return null;
    }

}
