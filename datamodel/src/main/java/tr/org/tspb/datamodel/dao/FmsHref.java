package tr.org.tspb.datamodel.dao;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class FmsHref {

    private final String toProjectKey;
    private final String toFormKey;
    private final String callBackFormKey;
    private final String callBackCrudKey;
    private final String toDirection;
    private final String callBackDirection;
    private final String retrieveValueKey;
    private final String retrieveLabelKey;

    public FmsHref(String toProjectKey, String toFormKey, String callBackFormKey,
            String callBackCrudKey) {
        this.toProjectKey = toProjectKey;
        this.toFormKey = toFormKey;
        this.callBackFormKey = callBackFormKey;
        this.callBackCrudKey = callBackCrudKey;
        this.toDirection = toFormKey.concat(",").
                concat(toProjectKey);
        this.callBackDirection = callBackFormKey.concat(",").
                concat(toProjectKey);
        this.retrieveValueKey = "_id";
        this.retrieveLabelKey = "ad";
    }

    public String getToProjectKey() {
        return toProjectKey;
    }

    public String getToFormKey() {
        return toFormKey;
    }

    public String getCallBackFormKey() {
        return callBackFormKey;
    }

    public String getToDirection() {
        return toDirection;
    }

    public String getCallBackDirection() {
        return callBackDirection;
    }

    public String getCallBackCrudKey() {
        return callBackCrudKey;
    }

    public String getRetrieveValueKey() {
        return retrieveValueKey;
    }

    public String getRetrieveLabelKey() {
        return retrieveLabelKey;
    }

}
