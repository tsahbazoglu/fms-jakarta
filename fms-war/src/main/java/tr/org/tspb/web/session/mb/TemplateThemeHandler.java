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

    private String contract = "redmond";
    private String layout = "web-layout";

    private static final String DB_NAME = "uysdb";
    private static final String COLLECTION_NAME = "theme-template";
    private static final String FIELD_THEME = "theme";
    private static final String FIELD_LAYOUT = "layout";
    private static final String FIELD_MEMBER_ID = "ldapUID";

    @Inject
    RepositoryService repositoryService;

    @Inject
    @MyLoginQualifier
    LoginController loginController;

    @PostConstruct
    public void init() {

        String username = loginController.getLoggedUserDetail().
                getUsername();

        Map<String, Object> settings = repositoryService.one(DB_NAME,
                COLLECTION_NAME,
                Filters.eq(FIELD_MEMBER_ID, username));

        if (settings != null) {
            Optional.ofNullable(settings.get(FIELD_THEME)).
                    ifPresent(t -> this.contract = t.toString());
            Optional.ofNullable(settings.get(FIELD_LAYOUT)).
                    ifPresent(l -> this.layout = l.toString());
        }

    }

    private void savePreferences() {
        String username = loginController.getLoggedUserDetail().
                getUsername();

        Map<String, Object> filter = new HashMap<>();
        filter.put(FIELD_MEMBER_ID, username);

        Map<String, Object> record = new HashMap<>();
        record.put(FIELD_THEME, this.contract);
        record.put(FIELD_LAYOUT, this.layout);

        repositoryService.updateMany(DB_NAME, COLLECTION_NAME, filter, record,
                true);
    }

    public void changeContract(String contract) {
        this.contract = contract;
        savePreferences();
    }

    public void changeLayout(String layout) {
        this.layout = layout;
        savePreferences();
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public List<String> getThemes() {
        return Arrays.asList("redmond", "nova-light", "nova-dark", "luna-blue",
                "omega", "blitzer","south-street");
    }

}
