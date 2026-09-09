package tr.org.tspb.converter.props;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import jakarta.faces.context.FacesContext;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MessageBundleLoader extends HashMap {

    public static final String WEB_MESSAGE_PATH = "tr.org.tspb.web.messages";
    public static final String MESSAGE_PATH = "tr.org.tspb.converter.props.translator";
    public static final String SELECTED_LANG = "SELECTED_LANG";
    private static final HashMap<String, ResourceBundle> mapWebResourceBundles = new HashMap<>();
    private static final HashMap<String, ResourceBundle> mapResourceBundles = new HashMap<>();

    /**
     * Gets a string for the given key from this resource bundle or one of its
     * parents.
     *
     * @param key the key for the desired string
     * @return the string for the given key. If the key string value is not
     * found the key itself is returned.
     */
    public static String getMessage(String key) {

        if (key == null) {
            return null;
        }

        Locale locale = null;
        Locale selected_locale = null;

        try {
            if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null) {
                locale = FacesContext.getCurrentInstance().
                        getViewRoot().
                        getLocale();
            }
            if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null && FacesContext.getCurrentInstance().getExternalContext().getSessionMap() != null) {
                selected_locale = (Locale) FacesContext
                        .getCurrentInstance()
                        .getExternalContext()
                        .getSessionMap()
                        .get(SELECTED_LANG);
            }
        } catch (Exception e) {
            //no action
        }

        if (locale == null) {
            locale = new Locale("tr", "TR");
        }

        if (selected_locale != null) {
            locale = selected_locale;
        }

        // 0. Try JSF application resource bundle "msg" registered in faces-config.xml first
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                ResourceBundle jsfBundle = context.getApplication().getResourceBundle(context, "msg");
                if (jsfBundle != null && jsfBundle.containsKey(key)) {
                    return jsfBundle.getString(key);
                }
            }
        } catch (Exception ex) {
            // ignore and fallback
        }

        // 1. Try primary web messages bundle via Thread Context ClassLoader
        try {
            ResourceBundle webMessages = mapWebResourceBundles.get(locale.toString());
            if (webMessages == null) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                webMessages = ResourceBundle.getBundle(WEB_MESSAGE_PATH, locale, cl);
                mapWebResourceBundles.put(locale.toString(), webMessages);
            }
            if (webMessages != null && webMessages.containsKey(key)) {
                return webMessages.getString(key);
            }
        } catch (Exception e) {
            // ignore and fallback
        }

        // 2. Fallback to converter translator bundle
        try {
            ResourceBundle messages = mapResourceBundles.get(locale.toString());
            if (messages == null) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                messages = ResourceBundle.getBundle(MESSAGE_PATH, locale, cl);
                mapResourceBundles.put(locale.toString(), messages);
            }
            if (messages != null && messages.containsKey(key)) {
                return messages.getString(key);
            }
        } catch (Exception e) {
            // ignore
        }

        return key;

    }

    @Override
    public Object get(Object key) {
        return getMessage((String) key);
    }

}
