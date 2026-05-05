package tr.org.tspb.constants.exceptions;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class FormConfigException extends Exception {

    public FormConfigException() {
        super();
    }

    public FormConfigException(String message) {
        super("<br/><br/>".concat(message).
                concat("<br/><br/> Please contact with the related form admin"));
    }

    public FormConfigException(String message, Exception ex) {
        super("<br/><br/>".concat(message).
                concat("<br/><br/> Please contact with the related form admin"),
                ex);
    }

}
