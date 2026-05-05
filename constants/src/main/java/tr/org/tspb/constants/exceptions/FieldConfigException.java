package tr.org.tspb.constants.exceptions;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class FieldConfigException extends Exception {

    private String myFieldJson;

    public FieldConfigException() {
        super();
    }

    public FieldConfigException(String message, String myFieldJson) {
        super("<br/><br/>".concat(message).
                concat("<br/><br/> Please contact with the related form admin"));
        this.myFieldJson = myFieldJson;
    }

    public FieldConfigException(String message) {
        super("<br/><br/>".concat(message).
                concat("<br/><br/> Please contact with the related form admin"));
    }

    public FieldConfigException(String message, Exception ex) {
        super("<br/><br/>".concat(message).
                concat("<br/><br/> Please contact with the related form admin"),
                ex);
    }

    public String getMyFieldJson() {
        return myFieldJson;
    }

}
