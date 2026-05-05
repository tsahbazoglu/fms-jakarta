package tr.org.tspb.datamodel.pojo;

import java.util.Date;
import jakarta.faces.component.html.HtmlCommandButton;
import jakarta.faces.component.html.HtmlInputText;
import jakarta.faces.component.html.HtmlInputTextarea;
import jakarta.faces.component.html.HtmlSelectBooleanCheckbox;
import jakarta.faces.component.html.HtmlSelectManyListbox;
import jakarta.faces.component.html.HtmlSelectOneMenu;
import jakarta.faces.component.html.HtmlSelectOneRadio;
import org.primefaces.component.calendar.Calendar;
import org.primefaces.component.inputnumber.InputNumber;

/**
 *
 * @author Telman Şahbazoğlu
 */
public enum ComponentType {

    selectOneMenu(String.class, HtmlSelectOneMenu.class,
            HtmlSelectOneMenu.COMPONENT_TYPE),
    selectManyListbox(String.class, HtmlSelectManyListbox.class,
            HtmlSelectManyListbox.COMPONENT_TYPE),
    inputDate(Date.class, Calendar.class, Calendar.COMPONENT_TYPE),
    inputText(String.class, HtmlInputText.class, HtmlInputText.COMPONENT_TYPE),
    inputTextarea(String.class, HtmlInputTextarea.class,
            HtmlInputTextarea.COMPONENT_TYPE),
    inputNumber(Number.class, InputNumber.class, InputNumber.COMPONENT_TYPE),
    selectBooleanCheckbox(Boolean.class, HtmlSelectBooleanCheckbox.class,
            HtmlSelectBooleanCheckbox.COMPONENT_TYPE),
    selectOneRadio(String.class, HtmlSelectOneRadio.class,
            HtmlSelectOneRadio.COMPONENT_TYPE),
    commandButton(String.class, HtmlCommandButton.class,
            HtmlCommandButton.COMPONENT_TYPE),
    //FIXME
    inputFile(String.class, Object.class, null),
    inputMask(String.class, Object.class, null),
    chips(String.class, Object.class, null),
    pickList(String.class, Object.class, null);
    //
    Class bindClass;
    Class componentClass;
    String componentType;

    public Class getBindClass() {
        return bindClass;
    }

    public Class getComponentClass() {
        return componentClass;
    }

    private ComponentType(Class bindClass, Class componentClass,
            String componentType) {
        this.bindClass = bindClass;
        this.componentClass = componentClass;
        this.componentType = componentType;
    }

    public static ComponentType value(String key) {
        try {
            return valueOf(key);
        } catch (Exception ex) {
            return null;
        }
    }

    public String getComponentType() {
        return componentType;
    }
}
