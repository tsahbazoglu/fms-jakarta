package tr.org.tspb.converter.props;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import jakarta.faces.context.FacesContext;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MessageBundleLoaderv1 extends HashMap {

    public static final String MESSAGE_PATH = "tr.org.tspb.converter.props.translator";
    public static final String SELECTED_LANG = "SELECTED_LANG";
    private static HashMap messageBundles = new HashMap();

    /**
     * Gets a formatted string for the given key and arguments.
     *
     * @param key the key for the desired string
     * @param args arguments used to format the string
     * @return the formatted string
     */
    public static String getMessage(String key, Object... args) {
        return MessageBundleLoader.getMessage(key, args);
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its
     * parents.
     *
     * @param key the key for the desired string
     * @return the string for the given key. If the key string value is not
     * found the key itself is returned.
     */
    public static String getMessage(String key) {
        return MessageBundleLoader.getMessage(key);
    }

    @Override
    public Object get(Object key) {
        return getMessage((String) key);
    }
}
