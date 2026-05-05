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
    private String jsfupdate;

    public FormDef(String value, String label) {
        this.value = value;
        this.label = label;
        this.jsfupdate = String.format("%s,%s", ID_MSG_DLG, ID_CENTER_MIDDLE);
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
        return label;
    }

    /**
     * @return the jsfupdate
     */
    public String getJsfupdate() {
        return jsfupdate;
    }

}
