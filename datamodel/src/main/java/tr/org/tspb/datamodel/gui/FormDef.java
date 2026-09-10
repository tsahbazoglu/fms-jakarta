package tr.org.tspb.datamodel.gui;

import static tr.org.tspb.constants.ProjectConstants.ID_CENTER_MIDDLE;
import static tr.org.tspb.constants.ProjectConstants.ID_MSG_DLG;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class FormDef {

    private String value;
    private String label;
    private String formCode;
    private String jsfupdate;

    public FormDef(String value, String label) {
        this.value = value;
        this.label = label;
        this.jsfupdate = String.format("%s,%s", ID_MSG_DLG, ID_CENTER_MIDDLE);
    }

    public FormDef(String value, String label, String formCode) {
        this(value, label);
        this.formCode = formCode;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        String translated = DynamicTranslator.translate(label);
        if (formCode != null && !formCode.trim().isEmpty()) {
            return translated + " - " + formCode;
        }
        return translated;
    }

    public String getRawLabel() {
        return label;
    }

    public String getFormCode() {
        return formCode;
    }

    /**
     * @return the jsfupdate
     */
    public String getJsfupdate() {
        return jsfupdate;
    }

}
