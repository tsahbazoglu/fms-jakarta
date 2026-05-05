package tr.org.tspb.converter.base;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
//
import org.bson.Document;
import com.mongodb.DBObject;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MapConverter implements Converter {

    public MapConverter() {
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component,
            String value) {
        try {
            return Document.parse(value);
        } catch (Exception ex) {
            throw new ConverterException("Could not convert");
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component,
            Object value) {
        if (value instanceof DBObject) {
            return value.toString();
        }
        return null;
    }
}
