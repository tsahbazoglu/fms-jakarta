package tr.org.tspb.web.session.mb;

import com.mongodb.client.model.Filters;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import tr.org.tspb.common.qualifier.MyLoginQualifier;
import tr.org.tspb.common.services.LoginController;
import tr.org.tspb.service.RepositoryService;

/**
 *
 * @author Telman Şahbazoğlu
 *
 */
@Named
@SessionScoped
public class TemplateThemeHandler implements Serializable {

    private static final long serialVersionUID = 1L;

    private String theme = "saga";//replace varaible name to theme
    private String layout = "web-layout";
    private String fontSize = "normal";

    private static final String DB_NAME = "uysdb";
    private static final String COLLECTION_NAME = "theme-template";
    private static final String FIELD_THEME = "theme";
    private static final String FIELD_LAYOUT = "layout";
    private static final String FIELD_FONT_SIZE = "fontSize";
    private static final String FIELD_MEMBER_ID = "ldapUID";

    @Inject
    RepositoryService repositoryService;

    @Inject
    @MyLoginQualifier
    LoginController loginController;

    @PostConstruct
    public void init() {

        String username = loginController.getLoggedUserDetail().getUsername();

        Map<String, Object> settings = repositoryService.one(DB_NAME, COLLECTION_NAME, Filters.eq(FIELD_MEMBER_ID, username));

        if (settings != null) {
            Optional.ofNullable(settings.get(FIELD_THEME)).ifPresent(t -> this.theme = t.toString());
            Optional.ofNullable(settings.get(FIELD_LAYOUT)).ifPresent(l -> this.layout = l.toString());
            Optional.ofNullable(settings.get(FIELD_FONT_SIZE)).ifPresent(fs -> this.fontSize = fs.toString());
        }
/*
        db.getCollection("theme-template").updateOne({ldapUID:"DENEME_PYS_1"},{$set:{theme:"saga"}})
        db.getCollection("theme-template").updateOne({ldapUID:"DENEME_PYS_2"},{$set:{layout:"web-layout-no-description-sidebar"}})
*/

/*
        db.getCollection("theme-template").updateMany({},{$set:{theme:"saga"}})
        db.getCollection("theme-template").updateMany({},{$set:{theme:"aura-dark-emerald"}})

        db.getCollection("theme-template").updateMany({},{$set:{layout:"web-layout-no-description-sidebar"}})
*/

    }

    private void savePreferences() {
        String username = loginController.getLoggedUserDetail().getUsername();

        Map<String, Object> filter = new HashMap<>();
        filter.put(FIELD_MEMBER_ID, username);

        Map<String, Object> record = new HashMap<>();
        record.put(FIELD_THEME, this.theme);
        record.put(FIELD_LAYOUT, this.layout);
        record.put(FIELD_FONT_SIZE, this.fontSize);

        repositoryService.updateMany(DB_NAME, COLLECTION_NAME, filter, record, true);
    }

    public void changeContract(String contract) {
        this.theme = contract;
        savePreferences();
    }

    public void changeLayout(String layout) {
        this.layout = layout;
        savePreferences();
    }

    public void changeFontSize(String fontSize) {
        this.fontSize = fontSize;
        savePreferences();
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontSizePx() {
        if (fontSize == null) {
            return "14px";
        }
        switch (fontSize.toLowerCase()) {
            case "small":
                return "13px";
            case "large":
                return "15px";
            case "xlarge":
                return "16px";
            case "normal":
            default:
                return "14px";
        }
    }

    public List<String> getFontSizes() {
        return Arrays.asList("small", "normal", "large", "xlarge");
    }

    public List<String> getThemes() {
        return Arrays.asList("saga-blue", "vela-blue", "arya-blue",
                // "aura-common",
                // "aura-common-dark",
                "aura-dark-emerald", "aura-light-emerald", "bootstrap4-blue-dark", "bootstrap4-blue-light",
                //"bootstrap4-dark-common",
                //"bootstrap4-light-common",
                "bootstrap4-purple-dark", "bootstrap4-purple-light", "luna-amber", "luna-blue",
                // "luna-common",
                "luna-green", "luna-pink", "material-compact-deeppurple-dark", "material-compact-deeppurple-light", "material-compact-indigo-dark", "material-compact-indigo-light",
                //"material-dark-common",
                "material-deeppurple-dark", "material-deeppurple-light", "material-indigo-dark", "material-indigo-light",
                //"material-light-common",
                "mytheme", "nova-colored",
                //"nova-common",
                "nova-dark", "nova-light");
    }

}
