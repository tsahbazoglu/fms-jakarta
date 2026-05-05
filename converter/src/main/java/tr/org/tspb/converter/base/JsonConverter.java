package tr.org.tspb.converter.base;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
//
import org.bson.Document;

/**
 *
 * @author Telman Şahbazoğlu
 */
@FacesConverter(value = "tr.org.tspb.converter.jsf.util.converter.JsonConverter")
public class JsonConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        if (value == null || value.trim().
                isEmpty()) {
            return null;
        }

        try {
            return Document.parse(value);
        } catch (Exception e) {
            FacesMessage msg = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "JSON Conversion Error",
                    "Invalid JSON format.");
            throw new ConverterException(msg);
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        if (value == null) {
            return new Document().toString();
        }
        return value.toString();
    }
}
