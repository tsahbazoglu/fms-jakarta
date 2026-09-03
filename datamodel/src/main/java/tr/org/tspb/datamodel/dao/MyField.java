package tr.org.tspb.datamodel.dao;

import tr.org.tspb.datamodel.pojo.ForumColumnCellKey;
import tr.org.tspb.datamodel.pojo.UserDetail;
import tr.org.tspb.datamodel.pojo.RoleMap;

import static tr.org.tspb.constants.ProjectConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jakarta.faces.convert.Converter;
import jakarta.faces.model.SelectItem;
import org.bson.Document;
import org.bson.types.Code;
import org.bson.types.ObjectId;
import tr.org.tspb.datamodel.dao.refs.PlainRecord;
import tr.org.tspb.datamodel.expected.FmsScriptRunner;
import tr.org.tspb.constants.exceptions.FormConfigException;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MyField {

    /**
     * @return the initialReadonly
     */
    public boolean isInitialReadonly() {
        return initialReadonly;
    }

    private static final String DEFAULT_CURRENT_VALUE = "defaultCurrentValue";
    private static final String DEFAULT_HISTORY_VALUE = "defaultHistoryValue";

    private ObjectId loginMemberId;
    private FmsForm myForm;

// <editor-fold defaultstate="collapsed" desc="encapsulated fields">

    /*
     * Aa	Bb	Cc	Dd	Ee	Ff	Gg
     * Hh	Ii	Jj	Kk      Ll	Mm	Nn
     * Oo	Pp	Qq	Rr	Ss	Tt	Uu
     * Vv	Ww	Xx	Yy	Zz
     */
    // A
    private String accesscontrol;
    private Boolean searchAccess;
    private Boolean quickFilter;
    private Boolean autoset;
    private Boolean hasAjaxEffectedInputFileField;
    private String ajaxUpdate;
    private String filterSelectOneMenuAjaxUpdate;
    private TagAjax tagAjax;
    // C
    private String code;//this is the case for nd
    private String calculateEngine;
    private String componentType;
    private String calculateOnClient;
    private String calculate;
    private Boolean calculateOnListView;
    private Boolean calculateOnCrudView;
    private Boolean calculateAfterSave;
    private Boolean calculateAfterDelete;
    private Boolean calculateOnSave;
    private Converter measureConverter;
    private Converter converterValue;
    private String converterInstance;
    private String converterFormat;
    private String converterParam;
    private Integer converterMinStrLength;
    private Map<String, String> cacheBsonConverter = new HashMap<>();
    private Map<ForumColumnCellKey, Converter> cacheMapConverter = new HashMap<>();
    // D
    private String description;
    private boolean renderDesc;
    private boolean renderPopupDesc;
    private String dateRangeBeginKey;
    private String dateRangeEndKey;
    private ObjectId defaultCurrentValue;
    private ObjectId defaultHistoryValue;
    private boolean disabled;
    private Document dbo;
    private Boolean dateRangeControl;
    private Boolean dateRangeValidate;
    private Object divider;
    private Object defaultValue;
    // E
    private boolean embeddedAsList;
    // F
    private String filterProjection;
    private String fieldNote;
    private String field;
    private String fileType = "/(\\.|\\/)(pdf)$/";
    private int fileLimit;
    // G
    private String genererate;
    // I
    private ObjectId _id;//this is the case for nd
    private boolean immediate;
    // H
    private FmsHref href;
    // K
    private String key;
    // L
    private List<SelectItem> listOfValues;
    private Boolean loginFK;
    // M
    private Object minFractationDigits;
    private Object maxFractationDigits;
    private List<SelectItem> selectItemsFilter = new ArrayList<>();
    private List<SelectItem> selectItemsCurrent = new ArrayList<>();
    private List<SelectItem> selectItemsHistory = new ArrayList<>();
    private FmsFieldItems itemsAsMyItems;
    private String myDatePattern;
    private String mask;
    private String myFormKey;
    private Double maxValue;
    private String maxMoney;
    private boolean money;
    // N
    private String name;
    private String ndType;
    private String ndAxis;
    // O
    private Integer order;
    private Object observer;
    private Object observableAttr;
    private Object observerAttr;
    private Object observable;
    // P
    private String popupDesc;
    // R
    private String refCollection;
    private Integer reportOrder;
    private boolean rendered;
    private boolean required;
    private boolean reportRendered;
    private boolean roleCheck;
    private boolean readonly;
    private boolean initialReadonly;
    // S
    private String sessionKey;
    private String subGroup;
    private String shortName;
    private String style;
    private String labelStyle;
    private String styleClass;
    private String filterCssClass;
    private Boolean shouldCheckNegative;
    // U
    private String uysformat;
    // V
    private Object valueChangeListenerAction;
    private String valueType;
    private String visible;
    private List<String> viewKey;
    private boolean version;
    // W
    private int width;
    private boolean workflow;
    private MyMap crudRecord = new MyMap();
    private FmsAutoComplete fmsAutoComplete;
    private FmsScriptRunner fmsScriptRunner;
    // 
    private boolean isAutoComplete;
    private List<Object> selectAllValues;
    private String db;//this is the case for nd

    // </editor-fold>
    private MyField(Builder builder) {
        this.loginMemberId = builder.loginMemberId;
        this.myForm = builder.myForm;
        this.accesscontrol = builder.accesscontrol;
        this.searchAccess = builder.searchAccess;
        this.quickFilter = builder.quickFilter;
        this.autoset = builder.autoset;
        this.hasAjaxEffectedInputFileField = builder.hasAjaxEffectedInputFileField;
        this.ajaxUpdate = builder.ajaxUpdate;
        this.filterSelectOneMenuAjaxUpdate = builder.filterSelectOneMenuAjaxUpdate;
        this.tagAjax = builder.tagAjax;
        this.code = builder.code;
        this.calculateEngine = builder.calculateEngine;
        this.componentType = builder.componentType;
        this.calculateOnClient = builder.calculateOnClient;
        this.calculate = builder.calculate;
        this.calculateOnListView = builder.calculateOnListView;
        this.calculateOnCrudView = builder.calculateOnCrudView;
        this.calculateAfterSave = builder.calculateAfterSave;
        this.calculateAfterDelete = builder.calculateAfterDelete;
        this.calculateOnSave = builder.calculateOnSave;
        this.measureConverter = builder.measureConverter;
        this.converterValue = builder.converterValue;
        this.converterInstance = builder.converterInstance;
        this.converterFormat = builder.converterFormat;
        this.converterParam = builder.converterParam;
        this.converterMinStrLength = builder.converterMinStrLength;
        this.cacheBsonConverter = builder.cacheBsonConverter;
        this.cacheMapConverter = builder.cacheMapConverter;
        this.description = builder.description;
        this.renderDesc = builder.renderDesc;
        this.renderPopupDesc = builder.renderPopupDesc;
        this.dateRangeBeginKey = builder.dateRangeBeginKey;
        this.dateRangeEndKey = builder.dateRangeEndKey;
        this.defaultCurrentValue = builder.defaultCurrentValue;
        this.defaultHistoryValue = builder.defaultHistoryValue;
        this.disabled = builder.disabled;
        this.dbo = builder.dbo;
        this.dateRangeControl = builder.dateRangeControl;
        this.dateRangeValidate = builder.dateRangeValidate;
        this.divider = builder.divider;
        this.defaultValue = builder.defaultValue;
        this.embeddedAsList = builder.embeddedAsList;
        this.filterProjection = builder.filterProjection;
        this.fieldNote = builder.fieldNote;
        this.field = builder.field;
        this.fileType = builder.fileType;
        this.fileLimit = builder.fileLimit;
        this.genererate = builder.genererate;
        this._id = builder._id;
        this.immediate = builder.immediate;
        this.href = builder.href;
        this.key = builder.key;
        this.listOfValues = builder.listOfValues;
        this.loginFK = builder.loginFK;
        this.minFractationDigits = builder.minFractationDigits;
        this.maxFractationDigits = builder.maxFractationDigits;
        this.selectItemsFilter = builder.selectItemsFilter;
        this.selectItemsCurrent = builder.selectItemsCurrent;
        this.selectItemsHistory = builder.selectItemsHistory;
        this.itemsAsMyItems = builder.itemsAsMyItems;
        this.myDatePattern = builder.myDatePattern;
        this.mask = builder.mask;
        this.myFormKey = builder.myFormKey;
        this.maxValue = builder.maxValue;
        this.maxMoney = builder.maxMoney;
        this.money = builder.money;
        this.name = builder.name;
        this.ndType = builder.ndType;
        this.ndAxis = builder.ndAxis;
        this.order = builder.order;
        this.observer = builder.observer;
        this.observableAttr = builder.observableAttr;
        this.observerAttr = builder.observerAttr;
        this.observable = builder.observable;
        this.popupDesc = builder.popupDesc;
        this.refCollection = builder.refCollection;
        this.reportOrder = builder.reportOrder;
        this.rendered = builder.rendered;
        this.required = builder.required;
        this.reportRendered = builder.reportRendered;
        this.roleCheck = builder.roleCheck;
        this.readonly = builder.readonly;
        this.initialReadonly = builder.initialReadonly;
        this.sessionKey = builder.sessionKey;
        this.subGroup = builder.subGroup;
        this.shortName = builder.shortName;
        this.style = builder.style;
        this.labelStyle = builder.labelStyle;
        this.styleClass = builder.styleClass;
        this.filterCssClass = builder.filterCssClass;
        this.shouldCheckNegative = builder.shouldCheckNegative;
        this.uysformat = builder.uysformat;
        this.valueChangeListenerAction = builder.valueChangeListenerAction;
        this.valueType = builder.valueType;
        this.visible = builder.visible;
        this.viewKey = builder.viewKey;
        this.version = builder.version;
        this.width = builder.width;
        this.workflow = builder.workflow;
        this.crudRecord = builder.crudRecord;
        this.fmsAutoComplete = builder.fmsAutoComplete;
        this.fmsScriptRunner = builder.fmsScriptRunner;
        this.isAutoComplete = builder.isAutoComplete;
        this.selectAllValues = builder.selectAllValues;
        this.db = builder.db;
    }

    public List<SelectItem> getSelectItemsFilter() {
        return selectItemsFilter;
    }

    public String getGenererate() {
        return genererate;
    }

    // <editor-fold defaultstate="collapsed" desc="getters">
    public FmsFieldItems getItemsAsMyItems() {
        return itemsAsMyItems;
    }

    public Boolean getDateRangeControl() {
        return dateRangeControl;
    }

    public Boolean getCalculateAfterDelete() {
        return calculateAfterDelete;
    }

    public Boolean getCalculateAfterSave() {
        return calculateAfterSave;
    }

    public Boolean getSearchAccess() {
        return searchAccess;
    }

    public Boolean getCalculateOnListView() {
        return calculateOnListView;
    }

    public Boolean getCalculateOnCrudView() {
        return calculateOnCrudView;
    }

    public String getMask() {
        if (mask == null || mask.isEmpty()) {
            mask = "(999) 999-9999";
        }
        return mask;
    }

    public String getDatePattern() {
        return myDatePattern;
    }

    public String getFileType() {
        return fileType;
    }

    public FmsHref getHref() {
        return href;
    }

    public String getField() {
        return field;
    }

    public String getFieldNote() {
        return fieldNote;
    }

    //FIXME need to be removed
    public String getKey() {
        return key;
    }

    public String getSubGroup() {
        return subGroup;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getComponentType() {
        if (componentType == null) {
            componentType = "inputText";
        }
        return componentType;
    }

    public boolean isAutoComplete() {
        return isAutoComplete;
    }

    public String getValueType() {
        return valueType;
    }

    public String getAccesscontrol() {
        return accesscontrol;
    }

    public String getCalculateEngine() {
        return calculateEngine;
    }

    public String getNdType() {
        return ndType;
    }

    public String getRefCollection() {
        return refCollection;
    }

    public String getNdAxis() {
        return ndAxis;
    }

    public String getCode() {
        return code;
    }

    public String getPopupDescription() {
        return popupDesc;
    }

    public String getDescription() {
        return description;
    }

    public String getDateRangeBeginKey() {
        return dateRangeBeginKey;
    }

    public String getDateRangeEndKey() {
        return dateRangeEndKey;
    }

    public String getAjaxUpdate() {
        return ajaxUpdate;
    }

    public String getMyFormKey() {
        return myFormKey;
    }

    public String getVisible() {
        return visible;
    }

    public String getMaxMoney() {
        return maxMoney;
    }

    public String getUysformat() {
        return uysformat;
    }

    public List<String> getViewKey() {
        return viewKey;
    }

    public Object getObservable() {
        return observable;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getCalculateOnClient() {
        return calculateOnClient;
    }

    public String getCalculate() {
        return calculate;
    }

    public Integer getOrder() {
        return order;
    }

    public Integer getReportOrder() {
        return reportOrder;
    }

    public int getWidth() {
        return width;
    }

    public boolean isHasHref() {
        return href != null;
    }

    public boolean isVersion() {
        return version;
    }

    public boolean isRendered() {
        return Boolean.TRUE.equals(rendered);
    }

    public boolean isRequired() {
        return Boolean.TRUE.equals(required);
    }

    public boolean isRoleCheck() {
        return roleCheck;
    }

    public boolean isMoney() {
        return money;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public Boolean getAutoset() {
        return autoset;
    }

    public Object getObserver() {
        return observer;
    }

    public Object getObservableAttr() {
        return observableAttr;
    }

    public Object getObserverAttr() {
        return observerAttr;
    }

    public int getFileLimit() {
        return fileLimit;
    }

    public ObjectId getDefaultHistoryValue() {
        return defaultHistoryValue;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isImmediate() {
        return immediate;
    }

    public Object getValueChangeListenerAction() {
        return valueChangeListenerAction;
    }

    public ObjectId getDefaultCurrentValue() {
        return defaultCurrentValue;
    }

    public Object getMinFractationDigits() {
        return minFractationDigits;
    }

    public Object getMaxFractationDigits() {
        return maxFractationDigits;
    }

    public Object getDivider() {
        return divider;
    }

    public boolean isReportRendered() {
        return reportRendered;
    }

    public List<SelectItem> getSelectItemsCurrent() {
        return Collections.unmodifiableList(selectItemsCurrent);
    }

    public List<SelectItem> getSelectItemsHistory() {
        return Collections.unmodifiableList(selectItemsHistory);
    }

    public ObjectId getId() {
        return _id;
    }

    public List<SelectItem> getListOfValues() {
        return Collections.unmodifiableList(listOfValues);
    }

    public Document getDbo() {
        return dbo;
    }

    public Boolean getCalculateOnSave() {
        return calculateOnSave;
    }

    public Boolean getDateRangeValidate() {
        return dateRangeValidate;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public Boolean getShouldCheckNegative() {
        return shouldCheckNegative;
    }

    public Object getReferance() {
        if (dbo.get(MONGO_ID) != null) {
            return dbo.get(MONGO_ID);
        } else {
            return dbo.get(FIELD);
        }
    }

    public void setSearchAccess(Boolean searchAccess) {
        this.searchAccess = searchAccess;
    }

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

// </editor-fold>

    /**
     * @return the measureConverter
     */
    public Converter getMeasureConverter() {
        return measureConverter;
    }

    @Override
    public int hashCode() {
        return (12345 + this.key.hashCode()) * (67890 + getReferance().
                hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MyField other = (MyField) obj;

        return Objects.equals(this.key, other.key);
    }

    @Override
    public String toString() {
        if (this.key == null) {
            throw new RuntimeException("Coordinate.java, dimensionOrMeasure:".
                    concat("key=null").
                    concat(dbo.toString()));
        }
        if (getReferance() == null) {
            throw new RuntimeException("Coordinate.java, dimensionOrMeasure:".
                    concat("getReferance()=null").
                    concat(dbo.toString()));
        }
        return this.key + "-" + getReferance().
                toString();
    }

    public void calcWfRendered(MyMap crud, RoleMap roleMap, Map searchObject) {

        if (!workflow) {
            return;
        }

        Object dboRendered = this.dbo.get(RENDERED);

        boolean localRendered = Boolean.TRUE.equals(dboRendered);

        if (dboRendered instanceof Document) {

            Document renderedObject = (Document) dboRendered;

            String onUserRole = (String) renderedObject.get(ON_USER_ROLE);
            if (onUserRole != null) {
                localRendered = roleMap.isUserInRole(onUserRole);
            } else {
                String db = (String) renderedObject.get(FORM_DB);
                String collection = (String) renderedObject.get(COLLECTION);
                String returnKey = (String) renderedObject.get(RETURN_KEY);
                Document query = (Document) renderedObject.get(QUERY);

                Document result = fmsScriptRunner.findOne(db, collection,
                        (Document) query);

                if (result == null || (HAYIR.equals(result.get(returnKey)))) {
                    localRendered = false;
                }
            }

        } else if (dboRendered instanceof Code) {
            Code appearFunction = (Code) dboRendered;
            appearFunction = new Code(appearFunction.getCode().
                    replace(DIEZ,
                            DOLAR));

            if (this.dbo.get(FORM_DB) == null) {
                throw new RuntimeException(
                        "field." + this.getKey() + ".rendered is defined as func. 'db' tag is required.");
            }

            Document crudDoc = new Document(crud);
            crudDoc.remove(INODE);

            Document commandResult = fmsScriptRunner.runCommand(this.dbo.get(
                                    FORM_DB).
                            toString(),
                    appearFunction.getCode(), crudDoc, searchObject, roleMap.
                            keySet());

            localRendered = Boolean.TRUE.equals(commandResult.get(RETVAL));
        }

        this.rendered = localRendered;
    }

    public void createDefaultCurrentValue(FmsForm myForm) {
        Code defaultCurrentValueCode = (Code) this.dbo.
                get(DEFAULT_CURRENT_VALUE);

        if (myForm.getRoleMap().
                isUserInRole(myForm.getMyProject().
                        getAdminAndViewerRole())) {
            defaultCurrentValueCode = (Code) this.dbo.get("adminFunc");
        }

        if (defaultCurrentValueCode != null) {
            defaultCurrentValueCode = new Code(
                    defaultCurrentValueCode.getCode().
                            replace(DIEZ, DOLAR));

            String itemsDB = itemsAsMyItems.getDb();

            Document commandResult = fmsScriptRunner.runCommand(itemsDB,
                    defaultCurrentValueCode.getCode(),
                    new Document(myForm.getDefaultCurrentQuery()), null);

            Object returnObject = commandResult.get(RETVAL);

            if (returnObject instanceof Document) {
                this.defaultCurrentValue = (ObjectId) ((Document) returnObject).
                        get(MONGO_ID);
            }
        }

        if (this.defaultCurrentValue == null && this.key.equals(myForm.
                getLoginFkField())) {
            List<ObjectId> list = (List<ObjectId>) myForm.getUserDetail().
                    getLoginFkSearchMapInListOfValues().
                    get(DOLAR_IN);
            this.defaultCurrentValue = list.get(0);
        }

    }

    public void createDefaultHistoryValue(FmsForm myForm) {
        Code defaultHistoryValueCode = (Code) this.dbo.
                get(DEFAULT_HISTORY_VALUE);

        if (myForm.getRoleMap().
                isUserInRole(myForm.getMyProject().
                        getAdminAndViewerRole())) {
            defaultHistoryValueCode = (Code) this.dbo.get("adminFunc");
        }

        if (defaultHistoryValueCode != null) {
            defaultHistoryValueCode = new Code(
                    defaultHistoryValueCode.getCode().
                            replace(DIEZ, DOLAR));

            String itemsDB = itemsAsMyItems.getDb();

            Document commandResult = fmsScriptRunner.runCommand(itemsDB,
                    defaultHistoryValueCode.getCode(),
                    new Document(myForm.getDefaultCurrentQuery()), null);

            Object returnObject = commandResult.get(RETVAL);

            if (returnObject instanceof Document) {
                this.defaultHistoryValue = (ObjectId) ((Document) returnObject).
                        get(MONGO_ID);
            }
        }

        if (this.defaultHistoryValue == null && this.key.equals(myForm.
                getLoginFkField())) {
            List<ObjectId> list = (List<ObjectId>) myForm.getUserDetail().
                    getLoginFkSearchMapInListOfValues().
                    get(DOLAR_IN);
            this.defaultHistoryValue = list.get(0);
        }
    }

    public List<PlainRecord> completeMethod(String query) {

        Map search = new HashMap(fmsAutoComplete.getFilter());
        if (query != null && query.length() >= 1) {
            Document queryRexegIgnoreCaseQuery = new Document().append(
                            DOLAR_REGEX, query).
                    append(DOLAR_OPTIONS, "i");
            search.put(itemsAsMyItems.getSearchField(),
                    queryRexegIgnoreCaseQuery);
            itemsAsMyItems = itemsAsMyItems.reCreateQuery(loginMemberId, search, crudRecord,
                    fmsAutoComplete.getRoleMap(), fmsScriptRunner);
        }
        Document resultFilter = new Document(search);
        resultFilter.putAll(itemsAsMyItems.getEditQuery());
        return fmsAutoComplete.completeMethod(resultFilter);
    }

    public void createSelectItems(Map filter, MyMap crudObject, RoleMap roleMap,
                                  UserDetail userDetail, boolean ajax) {
        if (this.itemsAsMyItems == null) {
            return;
        }
        if (ajax) {
            this.itemsAsMyItems = this.itemsAsMyItems.reCreateQuery(loginMemberId, filter,
                    crudObject, roleMap, fmsScriptRunner);
        }
        this.selectItemsFilter = fmsAutoComplete.createSelectItemsFilter(filter,
                crudObject);
        this.selectItemsCurrent = fmsAutoComplete.createSelectItemsEdit(filter,
                crudObject);
        this.selectItemsHistory = fmsAutoComplete.createSelectItemsHistory(
                filter, crudObject);
    }

    public Boolean getLoginFK() {
        return loginFK;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public boolean isRenderDesc() {
        return renderDesc;
    }

    public boolean isRenderPopupDesc() {
        return renderPopupDesc;
    }

    public boolean isEmbeddedAsList() {
        return embeddedAsList;
    }

    public boolean isWorkflow() {
        return workflow;
    }

    public void setAutoComplete(FmsAutoComplete autoComplete) {
        this.fmsAutoComplete = autoComplete;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Map<String, String> getCacheBsonConverter() {
        return cacheBsonConverter;
    }

    public void setCrudRecord(MyMap crudRecord) {
        this.crudRecord = crudRecord;
    }

    public String getStyle() {
        return style;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public String getLabelStyle() {
        return labelStyle;
    }

    public Boolean getQuickFilter() {
        return quickFilter;
    }

    public FmsForm getMyForm() {
        return myForm;
    }

    public void setMyForm(FmsForm myForm) {
        this.myForm = myForm;
    }

    public TagAjax getAjax() {
        return tagAjax;
    }

    public Boolean getHasAjaxEffectedInputFileField() {
        return hasAjaxEffectedInputFileField;
    }

    public void setHasAjaxEffectedInputFileField(
            Boolean hasAjaxEffectedInputFileField) {
        this.hasAjaxEffectedInputFileField = hasAjaxEffectedInputFileField;
    }

    public void createCrudSelectOneMenuAjaxUpdate(String ajaxUpdate) {
        this.ajaxUpdate = ajaxUpdate;
    }

    public void createFilterSelectOneMenuAjaxUpdate(String ajaxUpdate) {
        this.filterSelectOneMenuAjaxUpdate = ajaxUpdate;
    }

    public void createFilterCssClass(String effectedFiledKey) {
        this.filterCssClass = "class-filter-".concat(effectedFiledKey);
    }

    public void createCrudCssClass(String effectedKey) {
        this.styleClass = "id-class-".concat(effectedKey);
    }

    public String getFilterCssClass() {
        return filterCssClass;
    }

    public String getFilterSelectOneMenuAjaxUpdate() {
        return filterSelectOneMenuAjaxUpdate;
    }

    public static class Builder {

        private MyProject myProject;

        private ObjectId loginMemberId;
        private FmsForm myForm;

// <editor-fold defaultstate="collapsed" desc="encapsulated fields">

        /*
         * Aa	Bb	Cc	Dd	Ee	Ff	Gg
         * Hh	Ii	Jj	Kk      Ll	Mm	Nn
         * Oo	Pp	Qq	Rr	Ss	Tt	Uu
         * Vv	Ww	Xx	Yy	Zz
         */
        // A
        private String accesscontrol;
        private Boolean searchAccess;
        private Boolean quickFilter;
        private Boolean autoset;
        private Boolean hasAjaxEffectedInputFileField;
        private String ajaxUpdate;
        private String filterSelectOneMenuAjaxUpdate;
        private TagAjax tagAjax;
        // C
        private String code;//this is the case for nd
        private String calculateEngine;
        private String componentType;
        private String calculateOnClient;
        private String calculate;
        private Boolean calculateOnListView;
        private Boolean calculateOnCrudView;
        private Boolean calculateAfterSave;
        private Boolean calculateAfterDelete;
        private Boolean calculateOnSave;
        private Converter measureConverter;
        private Converter converterValue;
        private String converterInstance;
        private String converterFormat;
        private String converterParam;
        private Integer converterMinStrLength;
        private Map<String, String> cacheBsonConverter = new HashMap<>();
        private Map<ForumColumnCellKey, Converter> cacheMapConverter = new HashMap<>();
        // D
        private String description;
        private boolean renderDesc;
        private boolean renderPopupDesc;
        private String dateRangeBeginKey;
        private String dateRangeEndKey;
        private ObjectId defaultCurrentValue;
        private ObjectId defaultHistoryValue;
        private boolean disabled;
        private Document dbo;
        private Boolean dateRangeControl;
        private Boolean dateRangeValidate;
        private Object divider;
        private Object defaultValue;
        // E
        private boolean embeddedAsList;
        // F
        private String filterProjection;
        private String fieldNote;
        private String field;
        private String fileType = "/(\\.|\\/)(pdf)$/";
        private int fileLimit;
        // G
        private String genererate;
        // I
        private ObjectId _id;//this is the case for nd
        private boolean immediate;
        // H
        private FmsHref href;
        // K
        private String key;
        // L
        private List<SelectItem> listOfValues;
        private Boolean loginFK;
        // M
        private Object minFractationDigits;
        private Object maxFractationDigits;
        private List<SelectItem> selectItemsFilter = new ArrayList<>();
        private List<SelectItem> selectItemsCurrent = new ArrayList<>();
        private List<SelectItem> selectItemsHistory = new ArrayList<>();
        private FmsFieldItems itemsAsMyItems;
        private String myDatePattern;
        private String mask;
        private String myFormKey;
        private Double maxValue;
        private String maxMoney;
        private boolean money;
        // N
        private String name;
        private String ndType;
        private String ndAxis;
        // O
        private Integer order;
        private Object observer;
        private Object observableAttr;
        private Object observerAttr;
        private Object observable;
        // P
        private String popupDesc;
        // R
        private String refCollection;
        private Integer reportOrder;
        private boolean rendered;
        private boolean required;
        private boolean reportRendered;
        private boolean roleCheck;
        private boolean readonly;
        private boolean initialReadonly;
        // S
        private String sessionKey;
        private String subGroup;
        private String shortName;
        private String style;
        private String labelStyle;
        private String styleClass;
        private String filterCssClass;
        private Boolean shouldCheckNegative;
        // U
        private String uysformat;
        // V
        private Object valueChangeListenerAction;
        private String valueType;
        private String visible;
        private List<String> viewKey;
        private boolean version;
        // W
        private int width;
        private boolean workflow;
        private MyMap crudRecord = new MyMap();
        private FmsAutoComplete fmsAutoComplete;
        private FmsScriptRunner fmsScriptRunner;
        //
        private boolean isAutoComplete;
        private List<Object> selectAllValues;
        private String db;//this is the case for nd

        public Builder(Document docField) {
            this.dbo = docField;
        }

        public Builder(ObjectId loginMemberId, MyProject myProject,
                       Document docField, FmsScriptRunner fmsScriptRunner) {

            this.myProject = myProject;
            //
            this.loginMemberId = loginMemberId;
            //
            this.fmsScriptRunner = fmsScriptRunner;
            this._id = (ObjectId) docField.get(MONGO_ID);
            this.code = docField.getString(CODE);

            maskCalculate(docField);

            this.converterInstance = docField.getString(CONVERTER_INSTANCE);
            this.converterFormat = docField.getString(CONVERTER_FORMAT);
            this.converterParam = docField.getString(CONVERTER_PARAM);

            this.dbo = docField;
            this.dateRangeControl = Boolean.TRUE.equals(docField.get(DATE_RANGE_CONTROL));
            this.dateRangeValidate = Boolean.TRUE.equals(docField.get(DATE_RANGE_VALIDATE));
            this.dateRangeBeginKey = docField.getString(DATE_RANGE_BEGIN_KEY);
            this.dateRangeEndKey = docField.getString(DATE_RANGE_END_KEY);
            this.divider = docField.get(DIVIDER);
            this.disabled = Boolean.TRUE.equals(docField.get(DISABLED));
            this.field = docField.getString(FIELD);
            this.fieldNote = docField.getString(FIELD_NOTE);
            this.immediate = Boolean.TRUE.equals(docField.get(IMMEDIATE));
            this.key = docField.getString(FORM_KEY);
            this.loginFK = Boolean.TRUE.equals(docField.get("loginFK"));
            this.money = Boolean.TRUE.equals(docField.get(MONEY));
            this.maxMoney = docField.getString(MAX_MONEY);

            Number number = docField.get(MAX_VALUE, Number.class);
            if (number != null) {
                this.maxValue = number.doubleValue();
            }

            this.mask = docField.getString(MASK);
            this.minFractationDigits = docField.get(MIN_FRACTATION_DIGITIS);
            this.maxFractationDigits = docField.get(MAX_FRACTATION_DIGITIS);
            this.name = docField.getString(NAME);
            this.observable = docField.get(OBSERVABLE);
            this.observableAttr = docField.get(OBSERVABLE);
            this.observerAttr = docField.get(OBSERVER);
            this.observer = docField.get(OBSERVER);
            this.required = Boolean.TRUE.equals(docField.get(REQUIRED));
            this.roleCheck = Boolean.TRUE.equals(docField.get(ROLECHECK));
            this.reportRendered = Boolean.TRUE.equals(docField.get(REPORT_RENDERED));
            this.refCollection = docField.getString(REF_COLLECTION);
            this.subGroup = docField.getString(SUB_GROUP);
            this.shouldCheckNegative = Boolean.TRUE.equals(docField.get(SHOULD_CHECK_NEGOTIF));
            this.uysformat = docField.getString(FORMAT);
            this.version = Boolean.TRUE.equals(docField.get(VERSION));
            this.valueType = docField.getString(VALUE_TYPE);
            this.visible = docField.getString(VISIBLE);
            this.valueChangeListenerAction = docField.get(VALUE_CHANGE_LISTENER_ACTION);
            this.workflow = Boolean.TRUE.equals(docField.get(WORKFLOW));
            this.filterProjection = docField.getString(FILTER_PROJECTION);
            maskConverterJson(docField);

            Document pleaseSelect = docField.
                    get("please-select", Document.class);
            if (pleaseSelect != null) {
                this.selectAllValues = new FmsSelectAllStrategy(
                        fmsScriptRunner,
                        pleaseSelect, loginMemberId, null).getListOfObjectIds();
            }

            this.genererate = docField.getString("generate");

            Document href = docField.get("href", Document.class);
            if (href != null) {
                String projectKey = href.getString("projectKey");
                String formKey = href.getString("formKey");
                String callBackFormKey = href.getString("callBackFormKey");
                this.href = new FmsHref(projectKey, formKey, callBackFormKey, this.key);
            }

            this.db = docField.getString("db");
        }

        private Builder maskConverterJson(Document docField) {
            Document converterJson = docField.get("converter-json",
                    Document.class);
            if (converterJson != null) {
                this.converterInstance = converterJson.getString(
                        CONVERTER_INSTANCE);
                this.converterFormat = converterJson.getString(
                        CONVERTER_FORMAT);
                this.converterParam = converterJson.getString(
                        CONVERTER_PARAM);
                this.converterMinStrLength = converterJson.getInteger(
                        CONVERTER_MIN_STR_LENGTH);
            }
            return this;
        }

        private Builder maskCalculate(Document docField) {

            Document docCalc = docField.get(CALCULATE, Document.class);
            if (docCalc != null) {
                this.calculate = docCalc.getString(CALCULATE_ACTION);
                this.calculateOnSave = Boolean.TRUE.equals(docCalc.get(
                        CALCULATE_ON_SAVE));
                this.calculateAfterSave = Boolean.TRUE.equals(docCalc.
                        get(CALCULATE_AFTER_SAVE));
                this.calculateAfterDelete = Boolean.TRUE.equals(docCalc.
                        get(CALCULATE_AFTER_DELETE));
                this.calculateOnListView = Boolean.TRUE.equals(docCalc.
                        get(CALCULATE_ON_LIST_VIEW));
                this.calculateOnCrudView = Boolean.TRUE.equals(docCalc.
                        get(CALCULATE_ON_CRUD_VIEW));
                this.calculateOnClient = docCalc.getString(
                        CALCULATE_ON_CLIENT);
                this.calculateEngine = docCalc.getString(
                        CALCULATE_ENGINE);//FIXME
            } else {
                this.calculateOnSave = false;
                this.calculateAfterSave = false;
                this.calculateAfterDelete = false;
                this.calculateOnListView = false;
                this.calculateOnCrudView = false;
            }

            return this;
        }

        public Builder maskFormKey(String formKey) {
            this.myFormKey = formKey;
            return this;
        }

        public Builder maskId() {
            this._id = this.dbo.get(MONGO_ID, ObjectId.class);
            return this;
        }

        public Builder maskField() {
            this.field = (String) this.dbo.get(FIELD);
            return this;
        }

        public Builder maskKey() {
            this.key = (String) this.dbo.get(FORM_KEY);
            return this;
        }

        public Builder maskCode() {
            this.code = (String) this.dbo.get(CODE);
            if (this.code == null || MEASURE.equals(this.dbo.
                    get(ND_TYPE))) {
                this.code = (String) this.dbo.get(FORM_KEY);
            }
            return this;
        }

        public Builder maskSearchAccess() {
            Object searchAccess = this.dbo.get(SEARCH_ACCESS);
            this.searchAccess = !Boolean.FALSE.equals(searchAccess);

            Object quickFilter = this.dbo.get(QUICK_FILTER);
            this.quickFilter = !Boolean.FALSE.equals(quickFilter);
            return this;
        }

        public Builder withRendered(boolean rendered) {
            this.rendered = rendered;
            return this;
        }

        public Builder withReadonly(boolean readOnly) {
            this.readonly = readOnly;
            this.initialReadonly = readOnly;
            return this;
        }

        public Builder maskAccesscontrol() {
            StringBuilder accesscontrolSB = new StringBuilder();

            Object objAccessControl = this.dbo.get(ACCESS_CONTROL);

            if (objAccessControl == null) {
                throw new RuntimeException(ACCESS_CONTROL.concat(" = null"));
            }

            if (objAccessControl instanceof List) {
                for (String access : (List<String>) objAccessControl) {
                    accesscontrolSB.append((String) access);
                    accesscontrolSB.append(",");
                }
            } else {
                accesscontrolSB.append((String) this.dbo.get(
                        ACCESS_CONTROL));
            }

            if (this.myProject != null) {
                if (this.myProject.getAdminRole() != null) {
                    String[] roles = this.myProject.getAdminRole().
                            split(COMMA);
                    for (String role : roles) {
                        if (accesscontrolSB.indexOf(role) < 0) {
                            accesscontrolSB.append(COMMA);
                            accesscontrolSB.append(role);
                        }
                    }
                }

                if (this.myProject.getViewerRole() != null) {
                    String[] roles = this.myProject.getViewerRole().
                            split(COMMA);
                    for (String role : roles) {
                        if (accesscontrolSB.indexOf(role) < 0) {
                            accesscontrolSB.append(COMMA);
                            accesscontrolSB.append(role);
                        }
                    }
                }
            }

            this.accesscontrol = accesscontrolSB.toString();
            return this;
        }

        public Builder maskNdTypeAndNdAxis() {
            this.ndType = (String) this.dbo.get(ND_TYPE);
            this.ndAxis = (String) this.dbo.get(ND_AXIS);

            if (this.ndType != null && (!MEASURE.equals(
                    this.ndType)) && this.ndAxis == null) {
                throw new RuntimeException("ndAxis == null");
            }
            return this;
        }

        public Builder maskComponentType() {
            this.componentType = (String) this.dbo.get(
                    COMPONENTTYPE);
            if (this.componentType == null || this.componentType.
                    isEmpty()) {
                this.componentType = "inputText";
            }
            this.isAutoComplete = "autoComplete".equals(
                    this.componentType);
            return this;
        }

        public Builder withDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder maskItemsAsMyItems(
                String schemaVersion,
                Map filter,
                boolean admin,
                Set<String> roles) throws FormConfigException {

            //if (MyForm.SCHEMA_VERSION_110.equals(schemaVersion)) {
            maskItemsAsMyItemsSchemaVersion110(this.myFormKey, schemaVersion, filter, admin, roles);
            //} else {
            //  maskItemsAsMyItemsNoScema(schemaVersion, filter, admin, roles);
            //}

            return this;
        }

        private Builder maskItemsAsMyItemsSchemaVersion110(
                String formKey,
                String schemaVersion,
                Map filter,
                boolean admin,
                Set<String> roles) throws FormConfigException {

            Document itemsDoc = this.dbo.get(ITEMS, Document.class);
            if (itemsDoc == null) {
                return this;
            }
            try {
                ObjectId loginMemberId = this.loginMemberId;
                this.itemsAsMyItems = new FmsFieldItems
                        .Builder(
                        formKey,
                        this.key,
                        myProject.getAdminAndViewerRole(),
                        filter, itemsDoc,
                        this.fmsScriptRunner
                )
                        .withFilterQuery(loginMemberId, admin, roles)
                        .withQuerySchemaVersion110(loginMemberId, admin, roles)
                        .withSortSchemaVersion110(roles)
                        .withViewSchemaVersion110(roles)
                        .withHistoryQuerySchemaVersion110(loginMemberId, admin, roles)
                        .withLookup()
                        .withQueryProjection()
                        .withResultProjection()
                        .build();
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                sb.append("field : ");
                sb.append(this.key);
                sb.append(" : error in getting field.items<br/><br/> ");
                sb.append(e.getLocalizedMessage());
                throw new FormConfigException(sb.toString(), e);
            }
            return this;
        }

        public Builder withConverter(Converter converterValue, FmsForm myForm) {

            if (converterValue == null) {
                return this;
            }

            this.converterValue = converterValue;

            if (converterValue.getClass().
                    getSimpleName().
                    equalsIgnoreCase(
                            CONVERTER_BSON_CONVERTER)) {
                // this.createMyItemsOnSession(null, null, roleMap, this.myForm.userDetail);
                this.viewKey = Arrays.asList("name");
            } else if (converterValue.getClass().
                    getSimpleName().
                    equalsIgnoreCase(CONVERTER_SELECT_ONE_OBJECTID_CONVERTER)) {

                if (this.itemsAsMyItems != null) {

                    if (FmsFieldItems.ItemType.list.equals(this.itemsAsMyItems.getItemType())) {
                        Map<String, String> itemMap = new HashMap();
                        for (SelectItem selectItem : this.itemsAsMyItems.getListOfSelectItem()) {
                            itemMap.put(selectItem.getValue().
                                            toString(),
                                    selectItem.getLabel());
                        }
                        // ((SelectOneObjectIdConverter) this.converterValue).setItemMap(itemMap);
                    }
                    this.viewKey = this.itemsAsMyItems.getView();
                }

            } else if (converterValue.getClass().
                    getSimpleName().
                    equalsIgnoreCase(CONVERTER_TELMAN_STRING_CONVERTER)) {
                if (this.itemsAsMyItems != null) {
                    throw new RuntimeException(new StringBuilder()
                            .append(this.key).
                            append("<br/>").
                            append("\"TelmanStringConverter\" converter conflicts with \"items\" attribute.").
                            append("<br/>").
                            append("<br/>").
                            append("acceptable convrters are : ").
                            append("[").
                            append("none, ").
                            append("SelectOneStringConverter, ").
                            append("SelectOneObjectIdConverter").
                            append("]").
                            toString());
                }
            }
            return this;
        }

        public Builder maskRestOfThem() {
            if (this.dbo.get(LIST_OF_VALUES) != null) {
                this.listOfValues = (List<SelectItem>) this.dbo.get(LIST_OF_VALUES);
            }
            this.fileLimit = this.dbo.get(FILE_LIMIT) == null ? 1 : ((Number) this.dbo.
                    get(FILE_LIMIT)).intValue();
            if (this.dbo.get(FILE_TYPE) instanceof String) {
                this.fileType = (String) this.dbo.get(FILE_TYPE);
            }
            Object tmpWidth = this.dbo.get("width");
            this.width = tmpWidth == null ? 100 : ((Number) tmpWidth).
                    intValue();
            this.style = (this.dbo.get(STYLE) == null)
                    ? "white-space:nowrap;font-family: monospace;text-align:left;"
                    : "white-space:nowrap;".concat(this.dbo.getString(STYLE));

            Object datePattern = this.dbo.get(MY_DATE_PATTERN);

            this.myDatePattern = (datePattern instanceof String) ? datePattern.
                    toString() : "yyyy.MM.dd HH:mm";

            // maskLabelStyle
            this.labelStyle = (this.dbo.get(LABEL_STYLE) == null)
                    ? ""
                    : "".concat(this.dbo.get(LABEL_STYLE).
                    toString());

            return this;
        }

        public Builder maskDescription() {

            this.description = (String) this.dbo.
                    get(DESCRIPTION);
            this.renderDesc = this.rendered && this.description != null;

            this.popupDesc = (String) this.dbo.get(
                    POPUP_DESCRIPTION);
            this.renderPopupDesc = this.rendered && this.popupDesc != null;

            return this;
        }

        public Builder maskOrders() {

            if (this.dbo.get(ORDER) instanceof Number) {
                this.order = ((Number) this.dbo.get(ORDER)).
                        intValue();
            } else {
                this.order = 0;
            }

            if (this.dbo.get(REPORT_ORDER) instanceof Number) {
                this.reportOrder = ((Number) this.dbo.get(
                        REPORT_ORDER)).intValue();
            } else {
                this.reportOrder = 0;
            }

            return this;
        }

        public Builder maskAjax() {

            this.tagAjax = new TagAjax(new Document());

            Document ajax = this.dbo.get(AJAX, Document.class);
            if (ajax == null) {
                return this;
            }

            this.tagAjax = new TagAjax(ajax);

            return this;
        }

        public Builder maskEmbeddedAsList() {
            this.embeddedAsList = Boolean.TRUE.equals(this.dbo.
                    get("embeddedAsList"));
            return this;
        }

        public Builder cacheBsonConverter(boolean isBsonConverter) {
            if (isBsonConverter) {
                List<Document> list = this.itemsAsMyItems.getListOfDocument();
                for (Document doc : list) {
                    FmsCodeName fmsCodeName = new FmsCodeName(doc);
                    this.cacheBsonConverter.put(fmsCodeName.getCode(), fmsCodeName.getName());
                }
            }
            return this;
        }

        public Builder maskAutoset(String schemaVersion, RoleMap roleMap) {

            if (schemaVersion == null) {
                maskAutosetNoSchema();
                return this;
            }

            switch (schemaVersion) {
                case FmsForm.SCHEMA_VERSION_110:
                case FmsForm.SCHEMA_VERSION_111:
                    maskAutosetSchemaVersion110(roleMap);
                    break;
                default:
                    maskAutosetNoSchema();
            }
            return this;
        }

        private void maskAutosetSchemaVersion110(RoleMap roleMap) {

            Document autoset = this.dbo.get(AUTOSET, Document.class);

            if (autoset == null) {
                this.autoset = false;
                return;
            }

            Boolean value = autoset.get(VALUE, Boolean.class);

            if (value == null) {
                List<Document> list = autoset.get(CONFIG_ATTR_FIELD_ITEMS_LIST,
                        List.class);

                Document noRoleDoc = null;
                boolean noRole = true;

                for (Document docRoleValue : list) {
                    List<String> roles = docRoleValue.get("roles", List.class);
                    if (roles == null) {
                        noRoleDoc = docRoleValue;
                    } else if (roleMap.isUserInRole(roles)) {
                        noRole = false;
                        value = docRoleValue.getBoolean(VALUE);
                    }
                }

                if (noRole && noRoleDoc != null) {
                    value = noRoleDoc.getBoolean(VALUE);
                }
            }

            this.autoset = Boolean.TRUE.equals(value);

        }

        private void maskAutosetNoSchema() {
            this.autoset = this.dbo.get(AUTOSET, Boolean.class);
        }

        public Builder maskShortName() {
            this.shortName = (String) this.dbo.get(SHORT_NAME);
            if (this.shortName == null) {
                this.shortName = (String) this.dbo.get(NAME);
            }
            return this;
        }

        public Builder maskName() {
            this.name = (String) this.dbo.get(NAME);
            return this;
        }

        public Builder maskPivotMeaseureConverter(Converter measureConverter) {
            this.measureConverter = measureConverter;
            return this;
        }

        public MyField build() {
            return new MyField(this);
        }

    }

    /**
     * @return the filterProjection
     */
    public String getFilterProjection() {
        return filterProjection;
    }

    public Converter getMyconverter() {
        return converterValue;
    }

    public String getConverterInstance() {
        return converterInstance;
    }

    public String getConverterFormat() {
        return converterFormat;
    }

    public String getConverterParam() {
        return converterParam;
    }

    public Integer getConverterMinStrLength() {
        return converterMinStrLength;
    }

    /**
     * @return the selectAllStrategy
     */
    public List<Object> getSelectAllValues() {
        return selectAllValues;
    }

    /**
     * @return the db
     */
    public String getDb() {
        return db;
    }

}
