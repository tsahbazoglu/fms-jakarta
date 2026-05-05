package tr.org.tspb.web.util;

import jakarta.faces.component.html.HtmlCommandButton;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.event.ValueChangeListener;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class ResetChangeListener implements ValueChangeListener {

    private String myKey;

    public ResetChangeListener(String myKey) {
        this.myKey = myKey;
    }

    @Override
    public void processValueChange(ValueChangeEvent event) throws AbortProcessingException {
        HtmlCommandButton button = (HtmlCommandButton) FacesContext//
                .getCurrentInstance().getViewRoot()//
                .findComponent("iceformContent:buttonResetId");
        button.invokeOnComponent(null, myKey, null);
    }
}
