package tr.org.tspb.datamodel.gui;

import java.util.function.Function;

/**
 * Utility for dynamic translation of MongoDB form/module names.
 * Delegate is registered by AppScopeSrvCtrl at application startup.
 *
 * @author Telman Şahbazoğlu
 */
public class DynamicTranslator {

    private static Function<String, String> delegate;

    public static void setDelegate(Function<String, String> del) {
        delegate = del;
    }

    public static String translate(String text) {
        if (text == null || delegate == null) {
            return text;
        }
        try {
            return delegate.apply(text);
        } catch (Exception e) {
            return text;
        }
    }
}
