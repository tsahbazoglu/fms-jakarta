package tr.org.tspb.util.service;

import static tr.org.tspb.constants.ProjectConstants.MESSAGE_DIALOG;
import static tr.org.tspb.constants.ProjectConstants.WARNING;

import java.io.Serializable;
import org.primefaces.PrimeFaces;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.pojo.PostSaveResult;
import tr.org.tspb.util.stereotype.MyController;

/**
 *
 * @author Telman Şahbazoğlu
 */
@MyController
public class DlgCtrl implements Serializable {

    private String position = "center";
    private String imageLocation;
    private String imageAlt;
    private String title;
    private String message;
    private String detail;
    private String severity = "info";
    private String style = "font-size:13px;";
    private boolean renderedButon;
    private boolean renderedOkButon = true;
    private boolean rendered;
    private boolean visible;
    private boolean esignInstallNote = true;

    public DlgCtrl() {
    }

    public DlgCtrl(String imageLocation, String imageAlt, String title,
                   String msg, boolean rendered, boolean visible) {
        this.imageLocation = imageLocation;
        this.imageAlt = imageAlt;
        this.title = title;
        this.message = msg;
        this.rendered = rendered;
        this.visible = visible;
    }

    public void bulkSet(String imageLocation, String imageAlt, String title,
                        String msg, boolean rendered, boolean visible) {
        this.imageLocation = imageLocation;
        this.imageAlt = imageAlt;
        this.title = title;
        this.message = msg;
        this.rendered = rendered;
        this.visible = visible;
    }

    private String getLocalizedText(String key, String defaultText, Object... args) {
        if (key == null) {
            return defaultText;
        }
        try {
            jakarta.faces.context.FacesContext context = jakarta.faces.context.FacesContext.getCurrentInstance();
            if (context != null) {
                // 1. Try JSF application resource bundle "msg" registered in faces-config.xml
                try {
                    java.util.ResourceBundle jsfBundle = context.getApplication().getResourceBundle(context, "msg");
                    if (jsfBundle != null && jsfBundle.containsKey(key)) {
                        String text = jsfBundle.getString(key);
                        if (args != null && args.length > 0) {
                            return java.text.MessageFormat.format(text, args);
                        }
                        return text;
                    }
                } catch (Exception ex) {
                    // ignore
                }

                // 2. Try loading bundle via Thread Context ClassLoader
                java.util.Locale locale = null;
                if (context.getExternalContext() != null && context.getExternalContext().getSessionMap() != null) {
                    locale = (java.util.Locale) context.getExternalContext().getSessionMap().get("SELECTED_LANG");
                }
                if (locale == null && context.getViewRoot() != null) {
                    locale = context.getViewRoot().getLocale();
                }
                if (locale != null) {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("tr.org.tspb.web.messages", locale, cl);
                    if (bundle != null && bundle.containsKey(key)) {
                        String text = bundle.getString(key);
                        if (args != null && args.length > 0) {
                            return java.text.MessageFormat.format(text, args);
                        }
                        return text;
                    }
                }
            }
        } catch (Exception e) {
            // fallback
        }
        if (args != null && args.length > 0 && defaultText != null) {
            try {
                return java.text.MessageFormat.format(defaultText, args);
            } catch (Exception e) {
                return defaultText;
            }
        }
        return defaultText;
    }

    private String getBilgilendirme() {
        return getLocalizedText("bilgilendirme", "Bilgilendirme");
    }

    private String getUyari() {
        return getLocalizedText("uyari", "Uyarı");
    }

    private String getHata() {
        return getLocalizedText("hata", "Hata");
    }

    public void showPopup(String title, String msg, String clientSideDialogName) {
        this.severity = "info";
        this.detail = null;
        bulkSet(null, null, title, msg, true, true);
        setRenderedButon(false);
        setRenderedOkButon(true);
        setRendered(false);
        setStyle("font-size:13px;");
        setTitle(title);
        showPopup(clientSideDialogName);
    }

    public void showPopupInfo(String msg, String clientSideDialogName) {
        this.severity = "info";
        this.detail = null;
        String infoTitle = getBilgilendirme();
        String displayMsg;
        if (PostSaveResult.MSG.equals(msg) || "verileriniz.kaydedildi".equals(msg)) {
            displayMsg = getLocalizedText("verileriniz.kaydedildi", msg);
        } else if ("form.kaydedildi".equals(msg)) {
            displayMsg = getLocalizedText("verileriniz.kaydedildi", "Verileriniz Kaydedildi.");
        } else {
            displayMsg = msg;
        }
        bulkSet(null, null, infoTitle, displayMsg, true, true);
        setRenderedButon(false);
        setRenderedOkButon(true);
        setRendered(false);
        setStyle("font-size:13px;");
        setTitle(infoTitle);
        showPopup(clientSideDialogName);
    }

    private String codeMirrorContent;

    public String getCodeMirrorContent() {
        return codeMirrorContent;
    }

    public void showCodePopupInfo(String title, String codeMirrorContent,
                                  String errMessage, String clientSideDialogName) {
        this.codeMirrorContent = codeMirrorContent;
        this.message = errMessage;
        this.title = title;
        showPopup(clientSideDialogName);
    }

    public void showPopupWarning(String msg, String clientSideDialogName) {
        this.severity = "warn";
        this.detail = null;
        String warnTitle = getUyari();
        bulkSet(null, null, warnTitle, msg, true, true);
        setRenderedButon(false);
        setRenderedOkButon(true);
        setRendered(false);
        setStyle("font-size:13px;");
        setTitle(warnTitle);
        showPopup(clientSideDialogName);
    }

    public void showPopupWarning(String msg, String detail, String clientSideDialogName) {
        this.severity = "warn";
        this.detail = null;
        String warnTitle = getUyari();
        bulkSet(null, null, warnTitle, msg, true, true);
        setRenderedButon(false);
        setRenderedOkButon(true);
        setRendered(false);
        setStyle("font-size:13px;");
        setTitle(warnTitle);
        showPopup(clientSideDialogName);
    }

    public void showPopupError(String msg) {
        showPopupError(msg, (String) null);
    }

    public void showPopupError(String msg, String detail) {
        this.severity = "error";
        this.detail = null;
        String errTitle = getHata();
        bulkSet(null, null, errTitle, msg, true, true);
        setRenderedButon(false);
        setRenderedOkButon(true);
        setRendered(false);
        setStyle("font-size:13px;");
        setTitle(errTitle);
        showPopup(MESSAGE_DIALOG);
    }

    public void showPopupException(String userFriendlyMsg, Throwable ex) {
        this.severity = "error";
        this.detail = null;
        String errTitle = getHata();
        String mainMsg = userFriendlyMsg;
        if (mainMsg == null || mainMsg.isBlank()) {
            mainMsg = getLocalizedText("kayit.sirasinda.hata.olustu", "Kaydetme işlemi sırasında bir hata oluştu.");
        }

        bulkSet(null, null, errTitle, mainMsg, true, true);
        setRenderedButon(false);
        setRenderedOkButon(true);
        setRendered(false);
        setStyle("font-size:13px;");
        setTitle(errTitle);
        showPopup(MESSAGE_DIALOG);
    }

    /**
     *
     * @param title
     * @param msg
     */
    public void showPopupInfo2(String title, String msg) {
        this.severity = "info";
        this.detail = null;
        bulkSet(null, null, title, msg, true, true);
        setRenderedButon(false);
        setRenderedOkButon(true);
        setRendered(false);
        setStyle("font-size:13px;");
        showPopup(MESSAGE_DIALOG);
    }

    public void showPopupInfoWithOk(String msg, String clientSideDialogName) {
        this.severity = "info";
        this.detail = null;
        String infoTitle = getBilgilendirme();
        String displayMsg;
        if (PostSaveResult.MSG.equals(msg) || "verileriniz.kaydedildi".equals(msg)) {
            displayMsg = getLocalizedText("verileriniz.kaydedildi", msg);
        } else if ("form.kaydedildi".equals(msg)) {
            displayMsg = getLocalizedText("verileriniz.kaydedildi", "Verileriniz Kaydedildi.");
        } else {
            displayMsg = msg;
        }
        bulkSet(null, null, infoTitle, displayMsg, true, true);
        setRenderedButon(false);
        setRenderedOkButon(true);
        setRendered(false);
        setStyle("font-size:13px;");
        setTitle(infoTitle);
        showPopup(clientSideDialogName);
    }

    public void showPopupInfoWithOk(FmsForm form, String clientSideDialogName) {
        String msg = (form != null && form.getName() != null && !form.getName().isBlank())
                ? getLocalizedText("form.kaydedildi", "{0} kaydedildi.", form.getName())
                : getLocalizedText("verileriniz.kaydedildi", "Verileriniz Kaydedildi.");
        showPopupInfoWithOk(msg, clientSideDialogName);
    }

    /**
     *
     * @param msg
     */
    public void showMsgDlgWarnWithB(String msg) {
        bulkSet(null, null, WARNING, msg, true, true);
        setRenderedButon(true);
        setRenderedOkButon(false);
        setRendered(false);
        setStyle("font-size:13px;");
        setTitle(WARNING);
        showPopup(MESSAGE_DIALOG);
    }

    public void showPopup(String componentId, String widgetWar) {
        PrimeFaces.current().resetInputs(componentId);
        PrimeFaces.current().ajax().update(componentId);
        PrimeFaces.current().executeScript("PF('" + widgetWar + "').show()");
    }

    public void showPopup(String clientSideDialogName) {
        if (MESSAGE_DIALOG.equals(clientSideDialogName)) {
            try {
                PrimeFaces.current().ajax().update("idMessageDialog");
            } catch (Exception e) {
                // ignore
            }
        }
        PrimeFaces.current().
                executeScript("PF('" + clientSideDialogName + "').show()");
    }

    public void hidePopup(String clientSideDialogName) {
        PrimeFaces.current().
                executeScript("PF('" + clientSideDialogName + "').hide()");
    }

    public String getImageLocation() {
        return imageLocation;
    }

    public void setImageLocation(String imageLocation) {
        this.imageLocation = imageLocation;
    }

    public String getImageAlt() {
        return imageAlt;
    }

    public void setImageAlt(String imageAlt) {
        this.imageAlt = imageAlt;
    }

    public String getTitle() {
        if (title == null) {
            return "";
        }
        if ("Bilgilendirme".equalsIgnoreCase(title)) {
            return getBilgilendirme();
        }
        if ("Uyarı".equalsIgnoreCase(title) || "Uyari".equalsIgnoreCase(title)) {
            return getUyari();
        }
        if ("Hata".equalsIgnoreCase(title)) {
            return getHata();
        }
        return getLocalizedText(title, title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public boolean isRenderedButon() {
        return renderedButon;
    }

    public void setRenderedButon(boolean renderedButon) {
        this.renderedButon = renderedButon;
    }

    public boolean isRendered() {
        return rendered;
    }

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }

    public boolean isRenderedOkButon() {
        return renderedOkButon;
    }

    public void setRenderedOkButon(boolean renderedOkButon) {
        this.renderedOkButon = renderedOkButon;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public boolean isEsignInstallNote() {
        return esignInstallNote;
    }

    public void setEsignInstallNote(boolean esignInstallNote) {
        this.esignInstallNote = esignInstallNote;
    }

    public void showDlgUserInfo(String message) {
        this.message = message;
        showPopup("wv-dlg-user-info");
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getSeverityIcon() {
        if (severity == null) {
            return "pi-info-circle";
        }
        switch (severity.toLowerCase()) {
            case "error":
                return "pi-times-circle";
            case "warn":
            case "warning":
                return "pi-exclamation-triangle";
            case "success":
                return "pi-check-circle";
            case "info":
            default:
                return "pi-info-circle";
        }
    }

    public String getSeverityColorClass() {
        if (severity == null) {
            return "text-blue-500";
        }
        switch (severity.toLowerCase()) {
            case "error":
                return "text-red-500";
            case "warn":
            case "warning":
                return "text-orange-500";
            case "success":
                return "text-green-500";
            case "info":
            default:
                return "text-blue-500";
        }
    }

    public String getSeverityContainerClass() {
        if (severity == null) {
            return "border-left-3 border-blue-500 bg-blue-50 text-blue-900";
        }
        switch (severity.toLowerCase()) {
            case "error":
                return "border-left-3 border-red-500 bg-red-50 text-red-900";
            case "warn":
            case "warning":
                return "border-left-3 border-orange-500 bg-orange-50 text-orange-900";
            case "success":
                return "border-left-3 border-green-500 bg-green-50 text-green-900";
            case "info":
            default:
                return "border-left-3 border-blue-500 bg-blue-50 text-blue-900";
        }
    }

}
