package tr.org.tspb.datamodel.dao;

import jakarta.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.Arrays;

import static tr.org.tspb.constants.ProjectConstants.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.bson.Document;
import org.bson.types.Code;
import org.bson.types.ObjectId;
import tr.org.tspb.constants.ProjectConstants;
import tr.org.tspb.datamodel.expected.FmsScriptRunner;
import tr.org.tspb.constants.exceptions.FormConfigException;
import tr.org.tspb.datamodel.pojo.RoleMap;

/**
 *
 * @author Telman Şahbazoğlu
 * son of Alifettah Shahbazov (Şahbazoğlu)
 */
public class FmsFieldItems {
    public static final String ITEM_TABLE = "itemTable";
    public static final String ITEM_DB = "db";
    //
    private MyField myField;
    private ScriptEnv scriptEnv;
    private ItemType itemType;
    private MyLookup lookup;
    private String db;
    private String table;
    private String locale;
    private String labelStringFormat;
    private String searchField;//this is a filed regarding to wich the p:autocomplete completeMethod will be executed
    private List<String> view;
    private Document filterQuery;
    private Document editQuery;
    private Document historyQuery;
    private Document sort;
    private Document queryProjection;
    private Document resultProjection;
    private Number limit;
    private List<Document> listOfDocument;
    private List<SelectItem> listOfSelectItem;
    private List<String> listOfString;
    private Code code;
    private Code queryCode;
    private Document itemsDoc;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(db == null ? "" : db);
        sb.append(":");
        sb.append(table == null ? "" : table);
        sb.append(":");
        sb.append(view == null ? "" : view);
        sb.append(":");
        sb.append(editQuery == null ? "" : editQuery);
        sb.append(":");
        sb.append(historyQuery == null ? "" : historyQuery);
        sb.append(":");
        sb.append(sort == null ? "" : sort);
        sb.append(":");
        sb.append(labelStringFormat == null ? "" : labelStringFormat);
        sb.append(":");
        sb.append(limit == null ? "" : limit);
        sb.append(":");
        sb.append(searchField == null ? "" : searchField);
        sb.append(":");
        sb.append(locale == null ? "" : locale);
        return sb.toString(); //To change body of generated methods, choose Tools | Templates.
    }


    public List<String> getListOfString() {
        return listOfString;
    }

    public List<SelectItem> getListOfSelectItem() {
        return listOfSelectItem;
    }

    private FmsFieldItems() {
    }

    private FmsFieldItems(Builder builder) {
        this.db = builder.db;
        this.table = builder.table;
        this.searchField = builder.searchField;
        this.view = builder.view;
        this.editQuery = builder.editQuery;
        this.filterQuery = builder.filterQuery;
        this.historyQuery = builder.historyQuery;
        this.sort = builder.sort;
        this.queryProjection = builder.queryProjection;
        this.resultProjection = builder.resultProjection;
        this.limit = builder.limit;
        this.locale = builder.locale;
        this.labelStringFormat = builder.labelStringFormat;
        this.lookup = builder.lookup;
        this.itemType = builder.itemType;
        this.scriptEnv = builder.scriptEnv;
        this.myField = builder.myField;
        this.itemsDoc = builder.itemsDoc;
        this.listOfDocument = builder.listOfDocuments;
        if (this.listOfDocument != null) {
            this.listOfString = builder.listOfDocuments.stream()
                    .map(document -> document.getString(CODE))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            this.listOfString = Collections.emptyList();
        }

    }

    public Document getFilterQuery() {
        return filterQuery;
    }

    public String getLocale() {
        return locale;
    }

    public Document getQueryProjection() {
        return queryProjection;
    }

    public Document getResultProjection() {
        return resultProjection;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public List<Document> getListOfDocument() {
        return listOfDocument;
    }

    public Code getCode() {
        return code;
    }

    public String getLabelStringFormat() {
        return labelStringFormat;
    }

    public Document getEditQuery() {
        return editQuery;
    }

    public Document getHistoryQuery() {
        return historyQuery;
    }

    public Document getSort() {
        return sort;
    }

    public List<String> getView() {
        if (view == null) {
            return null;
        }
        return Collections.unmodifiableList(view);
    }

    public String getDb() {
        return db;
    }

    public String getTable() {
        return table;
    }

    public Number getLimit() {
        return limit;
    }

    public String getSearchField() {
        return searchField;
    }

    public MyLookup getLookup() {
        return lookup;
    }

    public Code getQueryCode() {
        return queryCode;
    }


    public static class Builder {
        private final Document itemsDoc;
        private Document filterQuery;
        private Document editQuery;
        private Document historyQuery;
        private String searchField;
        private String table;
        private String db;
        private String locale;
        private String labelStringFormat;
        private MyField myField;
        private ItemType itemType;
        private List<Document> listOfDocuments;
        private List<String> view;
        private Number limit;
        private ScriptEnv scriptEnv;
        private Map filter;
        private Document sort;
        private Document queryProjection;
        private Document resultProjection;
        private MyLookup lookup;
        private Code queryCode;
        private FmsScriptRunner fmsScriptRunner;
        private final Document defaultQueryProjection = new Document()
                .append(CODE, true)
                .append(ORDER, true)
                .append(NAME, true);

        public Builder(Map filter,
                       Document itemsDoc,
                       MyField myField,
                       FmsScriptRunner fmsScriptRunner) {

            String itemTypeAsString = itemsDoc.getString(TYPE);
            if (itemTypeAsString == null) {
                String errMsg = String.format("\"fields.%s.items.type\" resolved to empty.", myField.getKey());
                throw new RuntimeException(errMsg);
            }
            this.itemType = ItemType.valueOf(itemTypeAsString);
            this.filter = filter;
            this.itemsDoc = itemsDoc;
            this.fmsScriptRunner = fmsScriptRunner;
            this.myField = myField;
            switch (itemType) {
                case list -> {
                    this.withList();
                }
                case func -> {
                    throw new UnsupportedOperationException("maskItemsAsMyItems.code");
                }
                case ref -> {
                    Document itemsDoc_ = itemsDoc.get(FIELD_ITEMS_REF, Document.class);
                    this.db = itemsDoc_.getString(FORM_DB);
                    this.searchField = itemsDoc_.getString("searchField");
                    this.table = itemsDoc_.getString(ITEM_TABLE);
                    this.limit = itemsDoc_.getInteger(LIMIT);
                    this.locale = itemsDoc_.getString(LOCALE);
                    String script = itemsDoc_.getString("script");
                    Object labelStringFormat_ = itemsDoc_.get(LABEL_STRING_FORMAT);

                    if (this.db == null) {
                        String errMsg = String.format("\"fields.%s.items.ref.db\" resolved to empty.", myField.getKey());
                        throw new RuntimeException(errMsg);
                    }
                    if (table == null) {
                        String errMsg = String.format("\"fields.%s.items.ref.itemTable\" resolved to empty.", myField.getKey());
                        throw new RuntimeException(errMsg);
                    }
                    if (searchField == null) {
                        searchField = "fullTextSearch";
                    }
                    if (labelStringFormat_ != null) {
                        this.labelStringFormat = labelStringFormat_.toString();
                    }
                    if (script != null) {
                        this.scriptEnv = ScriptEnv.valueOf(script);
                    }
                }
            }
        }

        public Builder withItemType(ItemType itemType) {
            this.itemType = itemType;
            return this;
        }

        public Builder withLookup() {
            if (itemsDoc instanceof Document) {
                Document lookup = ((Document) itemsDoc).get("lookup",
                        Document.class);
                if (lookup != null) {
                    this.lookup = new MyLookup(lookup);
                }
            }
            return this;
        }

        public Builder withSortSchemaVersion110(Set<String> roleSet) {

            if (this.itemsDoc == null) {
                return this;
            }
            if (!ItemType.ref.equals(this.itemType)) {
                return this;
            }

            Document refDocument = this.itemsDoc.get(FIELD_ITEMS_REF, Document.class);

            Document sortDocument = refDocument.get(SORT, Document.class);

            if (sortDocument == null) {
                return this;
            }

            this.sort = new Document();

            if (sortDocument.get("func") != null) {
                try {
                    this.sort = (Document) fmsScriptRunner
                            .runCommand(this.db, sortDocument.get("func",
                                            String.class).
                                    replace(DIEZ, DOLAR), roleSet).
                            get(RETVAL);
                } catch (Exception exception) {
                    //nothing
                }
            } else if (sortDocument.get(CONFIG_ATTR_FIELD_ITEMS_SORT_LIST) != null) {
                for (Document d : sortDocument.getList(CONFIG_ATTR_FIELD_ITEMS_SORT_LIST, Document.class)) {

                    List<String> roles_ = d.getList("roles", String.class);

                    if (roles_ != null) {
                        boolean access = false;
                        for (String schemaRole : roles_) {
                            if (roleSet.contains(schemaRole)) {
                                access = true;
                            }
                        }
                        if (!access) {
                            continue;
                        }
                    }
                    this.sort.put(
                            d.get("key", String.class),
                            d.get("value", Integer.class));
                }
            }

            return this;
        }

        public Builder withQuerySchemaVersion110(ObjectId loginMemberId,
                                                 boolean admin, Set<String> roles) {
            this.createEditQuery(loginMemberId, admin, filter,
                    fmsScriptRunner, roles);
            return this;
        }

        public Builder withFilterQuery(ObjectId loginMemberId,
                                       boolean admin, Set<String> roles) {
            this.createFilterQuery(loginMemberId, admin, filter,
                    fmsScriptRunner, roles);
            return this;
        }

        public Builder withHistoryQuerySchemaVersion110(ObjectId loginMemberId,
                                                        boolean admin, Set<String> roles) {
            this.createHistoryQuery(loginMemberId, admin, filter,
                    fmsScriptRunner, roles);
            return this;
        }

        public Builder withViewSchemaVersion110(Set<String> roleSet) throws
                FormConfigException {

            if (this.itemsDoc == null) {
                return this;
            }
            if (!ItemType.ref.equals(this.itemType)) {
                return this;
            }

            Document refDocument = this.itemsDoc.get(FIELD_ITEMS_REF, Document.class);

            try {
                List<Document> viewList = refDocument.get(VIEW, List.class);

                this.view = new ArrayList<>();

                if (viewList != null) {

                    List<ViewOrder> list = new ArrayList<>();

                    for (Document entry : viewList) {
                        if (entry.get("permit") == null || isUserInRole(roleSet,
                                entry.get("permit"))) {
                            Number number = entry.get(ORDER, Number.class);
                            Integer order = (number == null) ? 0 : number.
                                    intValue();
                            list.add(new ViewOrder(entry.get("key").
                                    toString(), order == null ? 0 : order.
                                    intValue()));
                        }
                    }

                    Collections.sort(list, new Comparator<ViewOrder>() {
                        @Override
                        public int compare(ViewOrder viewOrder,
                                           ViewOrder viewOrder1) {
                            return Integer.compare(viewOrder.order,
                                    viewOrder1.order);
                        }
                    });

                    for (ViewOrder viewOrder : list) {
                        this.view.add(viewOrder.key);
                    }
                } else {
                    this.view.add("_id");
                }
            } catch (Exception ex) {
                throw new FormConfigException("failed on getting items view", ex);
            }
            return this;

        }

        public Builder withQueryProjection() {
            if (this.itemsDoc == null) {
                return this;
            }
            if (!ItemType.ref.equals(this.itemType)) {
                return this;
            }

            Document refDocument = this.itemsDoc.get(FIELD_ITEMS_REF, Document.class);


            this.queryProjection = refDocument.get("queryProjection",
                    Document.class);
            if (this.queryProjection == null) {
                this.queryProjection = defaultQueryProjection;
            }
            return this;
        }

        public Builder withResultProjection() {
            if (this.itemsDoc == null) {
                return this;
            }
            if (!ItemType.ref.equals(this.itemType)) {
                return this;
            }
            Document refDocument = this.itemsDoc.get(FIELD_ITEMS_REF, Document.class);
            this.resultProjection = refDocument.get("resultProjection",
                    Document.class);
            return this;
        }

        public Builder withList() {
            this.listOfDocuments = this.itemsDoc.getList("list", Document.class);
            return this;
        }

        public Builder withParent(MyField myField) {
            this.myField = myField;
            return this;
        }

        public FmsFieldItems build() {
            return new FmsFieldItems(this);
        }

        private void createFilterQuery(ObjectId loginMemberId, boolean admin,
                                       Map filter, FmsScriptRunner fmsScriptRunner, Set<String> roles)
                throws RuntimeException {

            if (itemsDoc == null) {
                return;
            }
            if (!ItemType.ref.equals(this.itemType)) {
                return;
            }
            Document refDocument = itemsDoc.get(FIELD_ITEMS_REF, Document.class);


            Document q = refDocument.get(USER_FILTER_QUERY, Document.class);

            if (q == null) {
                q = refDocument.get(QUERY, Document.class);
            }

            if (admin && refDocument.get(ADMIN_QUERY) != null) {
                q = refDocument.get(ADMIN_QUERY, Document.class);
            }

            List<Document> listOfFilter = q.getList("list", Document.class, new ArrayList<>());

            this.filterQuery = handleListOfQuery(listOfFilter, filter,
                    fmsScriptRunner, loginMemberId, roles);

        }

        private void createEditQuery(ObjectId loginMemberId, boolean admin,
                                     Map filter,
                                     FmsScriptRunner fmsScriptRunner, Set<String> roles) throws
                RuntimeException {

            if (itemsDoc == null) {
                return;
            }
            if (!ItemType.ref.equals(this.itemType)) {
                return;
            }
            Document refDocument = itemsDoc.get(FIELD_ITEMS_REF, Document.class);

            Document query_ = refDocument.get(QUERY, Document.class);
            if (admin && refDocument.get(ADMIN_QUERY) != null) {
                query_ = refDocument.get(ADMIN_QUERY, Document.class);
            }

            this.editQuery = new Document();

            String func = query_.get("func", String.class);
            List<Document> listOfFilter = query_.get("list", List.class);

            if (func != null) {
                this.queryCode = new Code(func);
                try {
                    this.editQuery = (Document) fmsScriptRunner
                            .runCommand(this.db, this.queryCode.getCode(),
                                    filter).
                            get(RETVAL);
                } catch (Exception exception) {
                    this.editQuery = new Document("fms_item_query_code_error",
                            "fms_item_query_code_error");
                }
            } else if (listOfFilter != null) {
                this.editQuery = handleListOfQuery(listOfFilter, filter,
                        fmsScriptRunner, loginMemberId, roles);
            }

        }

        private void createHistoryQuery(ObjectId loginMemberId, boolean admin,
                                        Map filter,
                                        FmsScriptRunner fmsScriptRunner, Set<String> roles)
                throws RuntimeException {
            if (itemsDoc == null) {
                return;
            }
            if (!ItemType.ref.equals(this.itemType)) {
                return;
            }
            Document refDocument = itemsDoc.get(FIELD_ITEMS_REF, Document.class);
            Document historyQuery_ = refDocument.get(HISTORY_QUERY, Document.class);
            if (historyQuery_ == null) {
                historyQuery_ = refDocument.get(QUERY, Document.class);
            }
            if (admin && refDocument.get(ADMIN_QUERY) != null) {
                historyQuery_ = refDocument.get(ADMIN_QUERY, Document.class);
            }
            this.historyQuery = new Document();
            String func = historyQuery_.get("func", String.class);
            List<Document> listOfFilter = historyQuery_.get("list", List.class);
            if (func != null) {
                try {
                    this.historyQuery = (Document) fmsScriptRunner
                            .runCommand(this.db, func, filter).
                            get(RETVAL);
                } catch (Exception exception) {
                    this.historyQuery = new Document("fms_item_query_code_error",
                            "fms_item_query_code_error");
                }
            } else if (listOfFilter != null) {
                this.historyQuery = handleListOfQuery(listOfFilter, filter,
                        fmsScriptRunner, loginMemberId, roles);
            }
        }

        private Document handleListOfQuery(List<Document> listOfFilter, Map filter,
                                           FmsScriptRunner fmsScriptRunner, ObjectId loginMemberId,
                                           Set<String> roles) throws RuntimeException {

            Document result = new Document();

            for (Document d : listOfFilter) {

                List<String> roles_ = d.getList("roles", String.class);

                if (roles_ != null) {
                    boolean access = false;
                    for (String schemaRole : roles_) {
                        if (roles.contains(schemaRole)) {
                            access = true;
                        }
                    }
                    if (!access) {
                        continue;
                    }
                }

                String key = d.get("key", String.class);

                Document refValue = d.get("ref-value", Document.class);
                Document inRef = d.get("in-ref", Document.class);
                String fmsValue = d.get(REPLACEABLE_KEY_FMS_VALUE, String.class);
                String strValue = d.get("string-value", String.class);
                Number numberValue = d.get("number-value", Number.class);
                List<String> listOfString = d.getList("array-value", String.class);
                List<Number> listOfNumber = d.getList("array-number", Number.class);

                if (refValue != null) {
                    result.put(key,
                            new TagItemsQueryRef(refValue, filter, fmsScriptRunner,
                                    loginMemberId,
                                    null, null).value());
                } else if (inRef != null) {
                    result.put(key, new Document(DOLAR_IN,
                            new TagItemsQueryRef(inRef, filter, fmsScriptRunner,
                                    loginMemberId,
                                    myField, null).values()));
                } else if (fmsValue != null) {
                    Matcher m;
                    if ((m = pattern_fms_crud.matcher(fmsValue)).find()) {
//                    Object crudValue = crud.get(m.group(1));
//                    result.put(key, crudValue == null ? "no result" : crudValue);
                    } else if ((m = pattern_fms_filter.matcher(fmsValue)).find()) {
                        result.put(key, filter == null ? null : filter.get(m.
                                group(1)));
                    } else {
                        switch (fmsValue) {
                            case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_FILTER_PERIOD:
                                result.put(key,
                                        filter.get("period") == null ? "no result" : filter.
                                                get("period"));
                                break;
                            case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_FILTER_TEMPLATE:
                                result.put(key,
                                        filter.get("template") == null ? "no result" : filter.
                                                get("template"));
                                break;
                            case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_LOGIN_MEMBER_ID:
                                result.put(key,
                                        loginMemberId == null ? "no result" : loginMemberId);
                                break;
                            case ProjectConstants.REPLACEABLE_KEY_WORD_FOR_FUNCTONS_LOGIN_MEMBER_TYPE:
                                Document user = fmsScriptRunner
                                        .findOne("uysdb", "common", new Document(
                                                "_id", loginMemberId));
                                result.
                                        put(key, user.
                                                get("memberType", String.class));
                                break;
                            default:
                                throw new RuntimeException(
                                        "could not find replaceble word");
                        }
                    }
                } else if (strValue != null) {
                    result.put(key, strValue);
                } else if (numberValue != null) {
                    result.put(key, numberValue);
                } else if (listOfString != null) {
                    result.put(key, new Document(DOLAR_IN, listOfString));
                } else if (listOfNumber != null) {
                    result.put(key, new Document(DOLAR_IN, listOfNumber));
                } else {

                    String type = d.get("type", String.class);
                    if (type == null) {
                        type = "string";
                    }
                    switch (type) {
                        case "number":
                            result.put(key, d.get(VALUE, Number.class));
                            break;
                        case "string":
                            result.put(key, d.get(VALUE, String.class).
                                    replaceAll(DIEZ, DOLAR));
                            break;
                        case "in":
                            result.put(key, new Document(DOLAR_IN, Arrays.asList(d.
                                    get(VALUE, String.class).
                                    replaceAll(DIEZ, DOLAR).
                                    split(","))));
                            break;
                        case "ne":
                            result.put(key, new Document(DOLAR_NE, d.get(VALUE,
                                            String.class).
                                    replaceAll(DIEZ, DOLAR)));
                            break;
                        case "regex":
                            result.put(key, new Document(DOLAR_REGEX, d.get(VALUE,
                                            String.class).
                                    replaceAll(DIEZ, DOLAR)));
                            break;
                        default:
                            throw new UnsupportedOperationException(
                                    "field.items.query.type is not supported  : " + type);
                    }
                }
            }
            return result;
        }


        public Boolean isUserInRole(Set<String> myroles,
                                    Object commaSplittedRoles) {
            if (commaSplittedRoles != null) {
                if (commaSplittedRoles instanceof List) {
                    for (String string : (Iterable<? extends String>) commaSplittedRoles) {
                        if (myroles.contains(string)) {
                            return Boolean.TRUE;
                        }
                    }
                } else if (commaSplittedRoles instanceof String) {
                    String[] roles = ((String) commaSplittedRoles).split("[,]+");
                    for (String string : roles) {
                        if (myroles.contains(string)) {
                            return Boolean.TRUE;
                        }
                    }
                }
            }
            return Boolean.FALSE;
        }

        private class ViewOrder {

            String key;
            Integer order;

            public ViewOrder(String key, Integer order) {
                this.key = key;
                this.order = order;
            }

        }
    }

    public void changeDbTableQuery(String db, String table, Document query) {
        this.db = db;
        this.table = table;
        this.editQuery = query;
    }

    public FmsFieldItems reCreateQuery(ObjectId loginMemberId, Map filter,
                                       MyMap crudObject, RoleMap roleMap,
                                       FmsScriptRunner fmsScriptRunner) {
        if (queryCode != null) {
            Document tempDbObject = new Document(crudObject);
            tempDbObject.remove(INODE); // INODE is not serialized
            if (filter != null) {
                tempDbObject.putAll(filter);
            }

            Object object = fmsScriptRunner
                    .runCommand(db, queryCode.getCode(), tempDbObject).
                    get(RETVAL);

            if (object instanceof Document) {
                this.editQuery = (Document) object;
            } else {
                this.editQuery = new Document("noresult", "noresult");
            }
            throw new UnsupportedOperationException("Query code is not supported");
        } else {
            boolean admin = roleMap.isUserInRole(myField.getMyForm().
                    getMyProject().
                    getAdminAndViewerRole());
            return new Builder(filter, this.itemsDoc, this.myField, fmsScriptRunner)
                    .withQuerySchemaVersion110(loginMemberId, admin, roleMap.keySet())
                    .withFilterQuery(loginMemberId, admin, roleMap.keySet())
                    .withSortSchemaVersion110(roleMap.keySet())
                    .build();
        }
    }

    public enum ItemType {
        list,
        doc,
        code,
        listOfString,
        ref,
        func
    }

    public enum ScriptEnv {
        app,
        mongo
    }

}
