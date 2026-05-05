package tr.org.tspb.datamodel.dao;

import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import static tr.org.tspb.constants.ProjectConstants.AJAX_ACTION;
import static tr.org.tspb.constants.ProjectConstants.AJAX_EFFECTED_KEYS;
import static tr.org.tspb.constants.ProjectConstants.CONFIG_ATTR_REF;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class TagAjax {

    /**
     * @param enable the enable to set
     */
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    private String action;
    private int order;
    private boolean enable;
    private final String render;
    private final String showHideJsFunction;
    private final List<String> effectedKeys;
    private final boolean ajaxRemoveNonRenderdFieldOnRecord;
    private TagAjaxRef tagAjaxRef;
    private List<AjaxElement> ajaxElements;

    public TagAjax(Document doc) {
        this.order = doc.getInteger("order", 0);
        this.enable = Boolean.TRUE.equals(doc.getBoolean("enable"));
        this.render = doc.getString("render");
        this.action = doc.getString(AJAX_ACTION);
        this.showHideJsFunction = doc.getString("show-hide");
        this.effectedKeys = doc.getList(AJAX_EFFECTED_KEYS, String.class);
        this.ajaxRemoveNonRenderdFieldOnRecord = doc.getBoolean(
                "remove-non-rendered-field-on-record", false);
        Document ajaxRef = doc.get(CONFIG_ATTR_REF, Document.class);
        if (ajaxRef != null) {
            this.tagAjaxRef = new TagAjaxRef(ajaxRef);
        }

        List<Document> list = doc.getList("list", Document.class);
        if (list != null) {
            ajaxElements = new ArrayList<>();
            for (Document ajaxDoc : list) {
                ajaxElements.add(new AjaxElement(ajaxDoc));
            }
            this.action = "list";
        }
    }

    /**
     * @return the action
     */
    public int getOrder() {
        return order;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @return the enable
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * @return the render
     */
    public String getRender() {
        return render;
    }

    /**
     * @return the showHideJsFunction
     */
    public String getShowHideJsFunction() {
        return showHideJsFunction;
    }

    /**
     * @return the effected_keys
     */
    public List<String> getEffectedKeys() {
        return effectedKeys;
    }

    /**
     * @return the ajaxRemoveNonRenderdFieldOnRecord
     */
    public boolean isAjaxRemoveNonRenderdFieldOnRecord() {
        return ajaxRemoveNonRenderdFieldOnRecord;
    }

    public TagAjaxRef getRef() {
        return tagAjaxRef;
    }

    /**
     * @return the ajaxElements
     */
    public List<AjaxElement> getAjaxElements() {
        return ajaxElements;
    }

    public class AjaxElement {

        List<String> effectedKeys;
        boolean isRender;
        boolean isRefreshItems;
        String refreshItemsDb;
        String refreshItemsTable;
        List<Document> refreshItemsQuery;

        public AjaxElement(Document ajaxElement) {
            this.effectedKeys = ajaxElement.getList("keys", String.class);
            Document render = ajaxElement.get("render", Document.class);
            if (render != null) {

                Document renderRef = ajaxElement.get(CONFIG_ATTR_REF,
                        Document.class);
                Document renderDoc = ajaxElement.get("doc", Document.class);
                String renderFunc = ajaxElement.getString("func");
                if (renderRef == null && renderDoc == null && renderFunc == null) {
                    this.isRender = false;
                }
            }
            //
            Document refreshItems = ajaxElement.get("refresh-items",
                    Document.class);
            if (refreshItems != null) {
                this.isRefreshItems = true;
                this.refreshItemsDb = refreshItems.getString("db");
                this.refreshItemsTable = refreshItems.getString("table");

                Object query_ = refreshItems.get("query");
                if (query_ instanceof Document) {
                    this.refreshItemsQuery = ((Document) query_).getList("list",
                            Document.class);
                } else if (query_ instanceof List) {
                    this.refreshItemsQuery = refreshItems.getList("query",
                            Document.class);
                }

                if (this.refreshItemsDb == null || this.refreshItemsTable == null || this.refreshItemsQuery == null) {
                    throw new RuntimeException(
                            "error occured on ajax.refresh-items config");

                }
            }

        }

    }

}
