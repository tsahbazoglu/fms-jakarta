package tr.org.tspb.web.session.mb;

import static tr.org.tspb.constants.ProjectConstants.*;
//
import java.io.InputStream;
import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Collator;
import java.util.*;
//
import jakarta.annotation.PostConstruct;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.faces.application.Application;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.model.DataModel;
import jakarta.faces.model.ListDataModel;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
//
import com.fasterxml.jackson.databind.ObjectMapper;
import htmlflow.HtmlFlow;
import jakarta.faces.FacesException;
import org.primefaces.component.tabview.Tab;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.LazyDataModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.primefaces.model.menu.MenuModel;
import org.slf4j.Logger;
import tr.org.tspb.web.helper.EnumPage;
import tr.org.tspb.common.util.AbstractDynamicFlow;
import tr.org.tspb.common.util.ViewRecord;
import tr.org.tspb.common.qualifier.MyLoginQualifier;
import tr.org.tspb.common.qualifier.ViewerController;
import tr.org.tspb.page.CrudOneDim;
import tr.org.tspb.util.service.DlgCtrl;
import tr.org.tspb.table.TwoDimViewCtrl;
import tr.org.tspb.table.TwoDimModifyCtrl;
import tr.org.tspb.fgtpswd.fp.FgtPswdTokenManager;
import tr.org.tspb.common.qualifier.MyQualifier;
import tr.org.tspb.common.services.BaseService;
import tr.org.tspb.service.CalcService;
import tr.org.tspb.common.services.LoginController;
import tr.org.tspb.common.services.MailService;
import tr.org.tspb.common.services.AppScopeSrvCtrl;
import tr.org.tspb.service.RepositoryService;
import tr.org.tspb.constants.ProjectConstants;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.FmsHref;
import tr.org.tspb.datamodel.dao.MyField;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.datamodel.dao.MyProject;
import tr.org.tspb.datamodel.gui.FormDef;
import tr.org.tspb.datamodel.gui.FormItem;
import tr.org.tspb.datamodel.gui.ModuleItem;
import tr.org.tspb.constants.exceptions.FieldConfigException;
import tr.org.tspb.constants.exceptions.FormConfigException;
import tr.org.tspb.pivot.ctrl.PivotModifierCtrl;
import tr.org.tspb.pivot.ctrl.PivotViewerCtrl;
import tr.org.tspb.constants.exceptions.MongoOrmFailedException;
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.web.helper.MenuModelCreator;
import tr.org.tspb.web.jsonschema.JSONSchemaTest;
import tr.org.tspb.pivot.simple.ctrl.SimplePivotCtrl;
import tr.org.tspb.util.stereotype.MyController;
import tr.org.tspb.service.FilterService;
import tr.org.tspb.service.FormService;
import tr.org.tspb.datamodel.pojo.DatabaseUser;
import tr.org.tspb.outsider.intr.EsignDoor;
import tr.org.tspb.table.MasakMB;

/**
 *
 * @author Telman Şahbazoğlu
 */
@MyController
public class MainFrame implements Serializable {

    private final JSONObject jsonSchemaFmsForm = new JSONObject(
            new JSONTokener(JSONSchemaTest.class.getResourceAsStream(
                    "fms-form-schema.json")));

    private final JSONObject jsonSchemaMyNamedQueries = new JSONObject(
            new JSONTokener(JSONSchemaTest.class.getResourceAsStream(
                    "fms-form-myNamedQueries.json")));

    private final JSONObject jsonSchemaMyField = new JSONObject(
            new JSONTokener(JSONSchemaTest.class.getResourceAsStream(
                    "fms-field-schema.json")));

    private final JSONObject jsonSchemaFieldItems = new JSONObject(
            new JSONTokener(JSONSchemaTest.class.getResourceAsStream(
                    "fms-field-items-schema.json")));

    private final JSONObject jsonSchemaFieldAjax = new JSONObject(
            new JSONTokener(JSONSchemaTest.class.getResourceAsStream(
                    "fms-field-ajax-schema.json")));

    private final Schema schemaFmsForm = SchemaLoader.load(jsonSchemaFmsForm);
    private final Schema schemaMyNamedQueries = SchemaLoader.load(
            jsonSchemaMyNamedQueries);
    private final Schema schemaMyMyField = SchemaLoader.load(jsonSchemaMyField);
    private final Schema schemaFieldItems = SchemaLoader.load(
            jsonSchemaFieldItems);
    private final Schema schemaFieldAjax = SchemaLoader.
            load(jsonSchemaFieldAjax);

    private static final String PATH = "path";

    private static final String INDEX = "index";
    private static final String ADMIN = "admin";

    private String myStyle = "smoothness";
    private String popupMessage;
    private Boolean flowPanelPopup;
    private AbstractDynamicFlow dynamicFlow;
    private Integer historyPositionColumns = 1;
    private String positionStyle = "VTopHLeftW100pNowrap,VTopHLeftW100pNowrap";
    private EnumPage currEnumPage;
    private DataModel mongoDbExportedFiles;
    private DataModel mysqlExportedFiles;
    private List<ModuleItem> accordionItems;
    private Map<String, Object> addInfoMap = new HashMap<>();
    private String selectedFormInfo;
    private String testForgetPswdEmail = "tsahbazoglu@tspb.org.tr";
    private String forgetPswdEnv = "LIVE";
    private Object showDetailObject;
    private FmsHref fmsHref;
    private MyMap callBackCrud;
    private String showDetailProjectFormKeys;
    private boolean welcome = true;
    private String emailTestRcpts = "";
    private MyProject myProject;
    private ModuleItem moduleItem;
    private LazyDataModel<Map> data;
    private Boolean adminRole = Boolean.FALSE;
    protected String fineCSS;
    private final int definedDimensionViewMeasure = 2;
    private int detectedDimension = 1;
    protected Map mapPattern = null;
    private MenuModel menuModel;
    private String aboutContent;
    private boolean historyRendered;
    private boolean hasSetToRelatedForm;

    @Inject
    private RepositoryService repositoryService;

    @Inject
    CalcService calcService;

    @Inject
    @MyQualifier(myEnum = ViewerController.crudOneDim)
    private CrudOneDim crudOneDim;

    @Inject
    @MyQualifier(myEnum = ViewerController.twoDimModifyCtrl)
    private TwoDimModifyCtrl twoDimModifyCtrl;

    @Inject
    @MyQualifier(myEnum = ViewerController.crudFourDim)
    private CrudFourDim crudFourDim;

    @Inject
    @MyQualifier(myEnum = ViewerController.crudPivot)
    private PivotModifierCtrl pivotModifierCtrl;

    @Inject
    @MyQualifier(myEnum = ViewerController.viewerPivot)
    private PivotViewerCtrl pivotViewerCtrl;

    @Inject
    @MyQualifier(myEnum = ViewerController.freeDesigner)
    private FreeDesigner freeDesigner;

    @Inject
    @MyQualifier(myEnum = ViewerController.simplePivotCtrl)
    private SimplePivotCtrl simplePivotCtrl;

    @Inject
    @MyQualifier(myEnum = ViewerController.twoDimViewCtrl)
    private TwoDimViewCtrl viewerTwoDim;

    @Inject
    private DlgCtrl dialogController;

    @Inject
    private MailService mailService;

    @Inject
    private BaseService baseService;

    @Inject
    private FilterService filterService;

    @Inject
    @MyLoginQualifier
    private LoginController loginController;

    @Inject
    private Logger logger;

    @Inject
    AppScopeSrvCtrl appScopeSrvCtrl;

    @Inject
    private FormService formService;

    @Inject
    private MasakMB masakMB;

    @Inject
    private EsignDoor esignDoor;

    public MainFrame() {

    }

    @PostConstruct
    public void init() {

        esignDoor.
                eimzaContextInstance(baseService.
                        getEsignProperties());

        currEnumPage = EnumPage.WELCOME_PAGE;

        try {
            accordionItems = repositoryService.createModuleList();
            for (ModuleItem mItem : accordionItems) {
                List<FormDef> forms = createFormSelectItems(mItem);
                mItem.createList(forms);
            }

            menuModel = new MenuModelCreator(accordionItems).getModel();

        } catch (FormConfigException | NullNotExpectedException ex) {
            logger.error("Failed to initialize forms: " + ex.getMessage());
            throw new FacesException(
                    "A configuration error occurred during initialization.", ex);
        }

    }

    private void verify(FmsForm myFormXs) throws FieldConfigException {

        Document fmsForm = myFormXs.getDbo();
        if (fmsForm != null) {
            JSONObject jsonFmsForm = new JSONObject(new JSONTokener(fmsForm.
                    toJson()));
            try {
                schemaFmsForm.validate(jsonFmsForm);
            } catch (Exception ex) {
                throw new RuntimeException("fmsForm:".concat(ex.getMessage()),
                        ex);
            }
        }

        Document myNamedQueries = myFormXs.getDbo().
                get("myNamedQueries",
                        Document.class);
        if (myNamedQueries != null) {
            JSONObject jsonSubject;
            if (false) {
                jsonSubject = new JSONObject(
                        new JSONTokener(JSONSchemaTest.class.
                                getResourceAsStream("myNamedQueriesObject.json")));
            } else {
                jsonSubject = new JSONObject(new JSONTokener(myNamedQueries.
                        toJson()));
            }
            try {
                schemaMyNamedQueries.validate(jsonSubject);
            } catch (Exception ex) {
                throw new RuntimeException("myNamedQueries:".concat(ex.
                        getMessage()), ex);
            }
        }

        for (MyField myField : myFormXs.getFieldsAsList()) {
            JSONObject jsonMyField = new JSONObject(new JSONTokener(myField.
                    getDbo().
                    toJson()));
            String prettyJson = null;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                prettyJson = objectMapper.writerWithDefaultPrettyPrinter().
                        writeValueAsString(myField.getDbo());

                schemaMyMyField.validate(jsonMyField);

                // begin validate items
                Document itemsDoc = myField.getDbo().
                        get("items", Document.class);
                if (itemsDoc != null) {
                    JSONObject jsonMyFieldItems = new JSONObject(
                            new JSONTokener(itemsDoc.toJson()));
                    schemaFieldItems.validate(jsonMyFieldItems);
                }
                // end validate items

                // begin validate ajax
                Document ajaxDoc = myField.getDbo().
                        get("ajax", Document.class);
                if (ajaxDoc != null) {
                    JSONObject jsonMyFieldAjax = new JSONObject(new JSONTokener(
                            ajaxDoc.toJson()));
                    schemaFieldAjax.validate(jsonMyFieldAjax);
                }
                // end validate ajax

            } catch (Exception ex) {
                throw new FieldConfigException(ex.getMessage(), prettyJson);
            }
        }

    }

    public static ELContext getELContext() {
        return FacesContext.getCurrentInstance().
                getELContext();
    }

    public static ExpressionFactory getExpressionFactory() {
        return getApplication().
                getExpressionFactory();
    }

    public static Application getApplication() {
        return FacesContext.getCurrentInstance().
                getApplication();
    }

    public String getArchitectRole() {
        return ProjectConstants.ARCHITECT_ROLE;
    }

    public String getEmailTestRcpts() {
        return emailTestRcpts;
    }

    public void setEmailTestRcpts(String emailTestRcpts) {
        this.emailTestRcpts = emailTestRcpts;
    }

    public String getForgetPswdEnv() {
        String x = (String) FgtPswdTokenManager.instance().
                getTokenData(
                        "env-forget-pswd-email", 1);
        if (x != null) {
            forgetPswdEnv = x;
        }
        return forgetPswdEnv;
    }

    public void setForgetPswdEnv(String forgetPswdEnv) {
        this.forgetPswdEnv = forgetPswdEnv;
    }

    public Object getShowDetailObject() {
        return showDetailObject;
    }

    public void setShowDetailObject(Object showDetailObject) {
        this.showDetailObject = showDetailObject;
    }

    public String getShowDetailProjectFormKeys() {
        return showDetailProjectFormKeys;
    }

    public void setShowDetailProjectFormKeys(String showDetailProjectFormKeys) {
        this.showDetailProjectFormKeys = showDetailProjectFormKeys;
    }

    public String getTestForgetPswdEmail() {
        String x = (String) FgtPswdTokenManager.instance().
                getTokenData(
                        "test-forget-pswd-email", 1);
        if (x != null) {
            testForgetPswdEmail = x;

        }
        return testForgetPswdEmail;
    }

    public void setTestForgetPswdEmail(String testForgetPswdEmail) {
        this.testForgetPswdEmail = testForgetPswdEmail;
    }

    public void saveTestForgetPswdEmail() {
        FgtPswdTokenManager.instance().
                getToken("test-forget-pswd-email",
                        testForgetPswdEmail, 1);
        FgtPswdTokenManager.instance().
                getToken("env-forget-pswd-email",
                        forgetPswdEnv, 1);
    }

    public void sendTestEmail() {
        try {

            if (emailTestRcpts.isEmpty()) {
                emailTestRcpts = "tsahbazoglu@tspb.org.tr";
            }

            mailService.sendMail("TDUB Test Email",
                    "This is a test email from Payara Server", emailTestRcpts);
        } catch (MessagingException ex) {
            logger.error("error occured", ex);
        }
    }

    public CrudFourDim getCrud4dMB() {
        return crudFourDim;
    }

    public void setCrud4dMB(CrudFourDim crud4dMB) {
        this.crudFourDim = crud4dMB;
    }

    public FreeDesigner getFreeDesignMB() {
        return freeDesigner;
    }

    public void setFreeDesignMB(FreeDesigner freeDesignMB) {
        this.freeDesigner = freeDesignMB;
    }

    public boolean getAccess() {
        return loginController.isUserInRole(ProjectConstants.ARCHITECT_ROLE);
    }

    public void downloadFileExistCheck() {
        String message = (String) FacesContext.getCurrentInstance().
                getExternalContext().
                getSessionMap().
                remove(
                        "globalWarningMessage");
        if (message != null) {
            //id=applicationWarnMessageNoFile
            FacesContext.getCurrentInstance().
                    addMessage(
                            "applicationWarnMessageNoFile", new FacesMessage(
                                    FacesMessage.SEVERITY_WARN, message, null));
            //globalOnly=true
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, message, null));
        }
    }

    public String getPositionStyle() {
        return positionStyle;
    }

    public void valueChangeListenerTheme(ValueChangeEvent event) {
        myStyle = (String) event.getNewValue();
        HttpServletRequest request = (HttpServletRequest) FacesContext//
                .getCurrentInstance().
                getExternalContext().
                getRequest();
        try {
            FacesContext.getCurrentInstance().
                    getExternalContext().
                    redirect(
                            request.getContextPath());
        } catch (IOException ex) {
            logger.error("error occured", ex);
        }
    }

    public String closePopup() {
        flowPanelPopup = Boolean.FALSE;
        return null;
    }

    private boolean makeDecision() {
        return true;
    }

    private List<FormDef> createFormSelectItems(ModuleItem moduleItem) {

        List<FormItem> formItems = repositoryService.findModuleForms(moduleItem);

        formItems.addAll(repositoryService.findModuleFormsSchemaVersion110(
                moduleItem));

        Collections.sort(formItems, new Comparator<FormItem>() {
            Collator myCollator = Collator.getInstance();

            @Override
            public int compare(FormItem t1, FormItem t2) {

                return Integer.compare(t1.getMenuOrder(), t2.getMenuOrder());
            }
        });

        List<FormDef> listOfFormInfo = new ArrayList<>();

        for (FormItem formItem : formItems) {
            String info = formItem.getKey().
                    concat(COMMA).
                    concat(moduleItem.
                            getProjectKey());

            StringBuilder formName = new StringBuilder((String) formItem.
                    getName());
            if (loginController.isUserInRole(ProjectConstants.ARCHITECT_ROLE)) {
                formName.append(" - ");
                formName.append((String) formItem.getForm());
            }

            FormDef formDef = new FormDef(info, formName.toString());

            listOfFormInfo.add(formDef);
        }
        return listOfFormInfo;
    }

    public String makeRequiredAction() {
        //FIXME Proivde the related new expanded arguments through the mapPattern 
        dynamicFlow.getInput().
                put("mapPattern", mapPattern);
        dynamicFlow.getInput().
                put("addInfoMap", addInfoMap);
        dynamicFlow.getInput().
                put("username", loginController.
                        getLoggedUserDetail().
                        getUsername());
        dynamicFlow.makeRequiredAction();
        makeDecision();//FIXME
        return null;
    }

    public void setCurrEnumPage(EnumPage currEnumPage) {
        this.currEnumPage = currEnumPage;
    }

    public EnumPage getCurrEnumPage() {
        return currEnumPage;
    }

    /**
     * this initial search object will be used in case of the related key value
     * on the Crud or Viewer MB equal null
     *
     * @return
     */
    public String goToSystem() {
        currEnumPage = EnumPage.SYSTEM_PAGE;
        readFiles();
        return null;
    }

    public DataModel getMongoDbExportedFiles() {
        return mongoDbExportedFiles;
    }

    public DataModel getMysqlExportedFiles() {
        return mysqlExportedFiles;
    }

    public String deleteExportFile() {
        if (mongoDbExportedFiles.isRowAvailable()) {
            Map map = (Map) mongoDbExportedFiles.getRowData();
            String path = (String) map.get(PATH);
            try {
                URL url = new URL("http://127.0.0.1:8080/".concat(path));
                HttpURLConnection httpCon = (HttpURLConnection) url.
                        openConnection();

                httpCon.setRequestMethod("DELETE");
                /*
                 you'll need to call httpCon.getInputStream() or 
                 httpCon.getContent() or httpCon.getResponseCode() 
                 at the end to cause the request to actually be sent
                 */
                int responseCode = httpCon.getResponseCode();
                String responseMessage = httpCon.getResponseMessage();
                /*
                 httpCon.setDoOutput(true);
                 httpCon.setRequestProperty(
                 "Content-Type", "application/x-www-form-urlencoded");
                 httpCon.connect();
                 */
                if (responseCode != 200) {
                    dialogController.showPopupError(
                            "dosya silme esanasında hata oluştu <br/>".concat(
                                    responseMessage));
                }
                readFiles();
            } catch (IOException ex) {
                logger.error("error occured", ex);
            }
        }
        return null;
    }

    public void myOnPaneChange(TabChangeEvent event) {

        if (loginController.isEmpty()) {
            dialogController.showPopupInfo("roles is empty", MESSAGE_DIALOG);
            return;
        }

        this.welcome = false;
        try {

            Tab accordionPane = event.getTab();
            String upperNodeKey = (String) accordionPane.getAttributes().
                    get(
                            FORM_KEY);
            String projectKey = (String) accordionPane.getAttributes().
                    get(
                            PROJECT_KEY);

            figureOutProject(projectKey, upperNodeKey);
        } catch (RuntimeException ex) {
            logger.error("error occured", ex);
            String warningsAndErrorAsText = ex.toString();
            dialogController.showPopupInfo(warningsAndErrorAsText,
                    MESSAGE_DIALOG);
        } catch (Exception e) {
            dialogController.showPopupInfo(e.getLocalizedMessage(),
                    MESSAGE_DIALOG);

        }
    }

    private void figureOutProject(String projectKey, String upperNodeKey)
            throws NullNotExpectedException, RuntimeException,
            FormConfigException {

        this.myProject = repositoryService.getMyProject(projectKey);

        this.moduleItem = repositoryService.getModuleItem(myProject.
                getConfigTable(), upperNodeKey);

        DatabaseUser databaseUser = repositoryService
                .getDatabaseUser(myProject, moduleItem, loginController.
                        getLoggedUserDetail().
                        getUsername());

        loginController.getLoggedUserDetail().
                initDatabaseUser(databaseUser);
        loginController.resolveDelegations(databaseUser);

        if (loginController.notMemberNotAdminNotViewer(myProject)) {

            String html = HtmlFlow.doc(System.out).
                    html().
                    head().
                    title().
                    text("Create Jasper Scripts").
                    __().
                    __() //head
                            .
                    body().
                    div().
                    attrClass("container").
                    br().
                    __().
                    br().
                    __().
                    ul().
                    li().
                    text(
                            "<font color='green'>'%1$s' kullanıcı adı ile giriş işlemi ve kullanıcı rollerinin belirlenmesi başarılı.</font>").
                    __().
                    li().
                    text(
                            "<font color='#A4A220'>'%1$s' kullanıcı adı '%2$s' formunun admin grubunda yer almıyor.</font>").
                    __().
                    li().
                    text(
                            "<font color='red'>veya '%1$s' kullanıcı adı ile eşleşen kurum yok.</font>").
                    __().
                    __().
                    __().
                    __() //body
                            .
                    __() //html
                            .
                    toString();

            throw new RuntimeException(String.format(html, loginController.
                    getLoggedUserDetail().
                    getUsername(), moduleItem.getName()));
        }

        String welcomepage = myProject.getWelcomePage();
        if (welcomepage == null) {
            logger.error(String.format(
                    "welocme page is not set yet for project with key %s",
                    projectKey));
            welcomepage = "under-construct.html";
        }

        aboutContent = repositoryService.createWelcomePage(welcomepage);

        fineCSS = upperNodeKey;
    }

    public MenuModel getModel() {
        return menuModel;
    }

    public String goToForm() throws Exception {
        selectedFormInfo = fmsHref.getToDirection();
        this.hasSetToRelatedForm = true;
        localMyActionListener2();
        return null;
    }

    public String setMultiToRelatedForm() throws Exception {

        List objectIds = new ArrayList();
        for (Map<String, Object> map : twoDimModifyCtrl.getSelectedRow()) {
            objectIds.add(map.get("_id"));
        }

        this.selectedFormInfo = fmsHref.getCallBackDirection();

        localMyActionListener();

        callBackCrud.put(fmsHref.getCallBackCrudKey(), objectIds);
        twoDimModifyCtrl.setMyObject(callBackCrud);
        twoDimModifyCtrl.showCrud();

        return null;
    }

    public String setToRelatedForm(ObjectId id) throws Exception {
        selectedFormInfo = fmsHref.getCallBackDirection();

        localMyActionListener();

        callBackCrud.put(fmsHref.getCallBackCrudKey(), id);
        twoDimModifyCtrl.setMyObject(callBackCrud);
        twoDimModifyCtrl.showCrud();

        return null;
    }

    public String myActionListener1() throws Exception {

        String[] selectedFormInfos = showDetailProjectFormKeys.split("[,]");
        String formKey = selectedFormInfos[0];
        String projectKey = selectedFormInfos[1];

        myProject = repositoryService.getMyProject(projectKey);

        String recordView = "no result";

        try {

            FmsForm myForm = repositoryService.
                    getMyFormLarge(myProject, formKey);

            Map record = repositoryService.getRecord(myForm.getDb(), myForm.
                    getTable(), showDetailObject);

            if (record == null) {
                recordView = "Kayıt bulunamadı";
            } else {
                recordView = new ViewRecord.Builder(record, myForm)
                        .withValue(NAME).
                        withBR().
                        withHR().
                        withBR().
                        withAllKeyAsTable(true).
                        build();
            }

        } catch (Exception ex) {
            logger.error("error occured", ex);
        }

        dialogController.showPopupError(recordView);

        return null;
    }

    public String selectForm() {
        try {
            localMyActionListener();
        } catch (FieldConfigException ex) {
            logger.error("error occured", ex);
            dialogController.showCodePopupInfo("Alan Tanımlama Hatası", ex.
                    getMyFieldJson(), ex.getMessage(), "wv-dlg-code");
        } catch (Exception ex) {
            logger.error("error occured", ex);
            dialogController.showPopupInfo(ex.getMessage(), MESSAGE_DIALOG);
        }
        return null;
    }

    public String myActionListener(AjaxBehaviorEvent event) {
        try {
            localMyActionListener();
        } catch (Exception ex) {
            logger.error("error occured", ex);
            dialogController.showPopupInfo(ex.getMessage(), MESSAGE_DIALOG);
        }
        return null;
    }

    public void masaktedbirler() throws Exception {
        try {
            masakMB.tedbirlerigetir();
            currEnumPage = EnumPage.MASAK_RISK_SUB_MENU;
        } catch (Exception e) {
            dialogController.showPopupError(e.getLocalizedMessage());
        }
        return;
    }

    public void localMyActionListener() throws Exception {
        this.hasSetToRelatedForm = false;
        localMyActionListener2();
    }

    public void localMyActionListener2() throws Exception {
        this.welcome = false;
        this.isTwoDim = false;

//        EsignViewMB esignViewMB = (EsignViewMB) FacesContext.getCurrentInstance().getViewRoot().getViewMap().get("esignViewMB");
//
//        if (esignViewMB != null && esignViewMB.isPreventedMultiTab()) {
//            showNOKMessage("Bir oturumda bir görünüm kısıtlaması aktivleştirildi. Lütfen İlk açıtığınız sekmden devam ediniz.");
//            return;
//        }
        HttpSession httpSession = (HttpSession) FacesContext.
                getCurrentInstance().
                getExternalContext().
                getSession(false);
        httpSession.removeAttribute(HTTP_SESSION_ATTR_MAP_REQURED_CONTROL);

        String[] selectedFormInfos = selectedFormInfo.split("[,]");

        String formKey = selectedFormInfos[0];
        String projectKey = selectedFormInfos[1];

        if ("about".equals(selectedFormInfos[0])) {
            currEnumPage = EnumPage.ABOUT_PAGE;
            String moduleKey = selectedFormInfos[2];
            figureOutProject(projectKey, moduleKey);
            this.historyRendered = false;
            return;
        }

        this.myProject = appScopeSrvCtrl.getProject(projectKey);
        if (myProject.getUserRole() == null) {
            String html = HtmlFlow.doc(System.out).
                    html().
                    body().
                    div().
                    attrClass("container").
                    span().
                    text("configdb.project.".concat(projectKey).
                            concat(
                                    ".userRole is missed. please contact with related module administrator.")).
                    __().
                    br().
                    __().
                    br().
                    __().
                    span().
                    text("use configdb").
                    __().
                    br().
                    __().
                    br().
                    __().
                    span().
                    text("db.project.update({key:'".concat(projectKey).
                            concat("'},{$set:{")).
                    __().
                    br().
                    __().
                    span().
                    text("userRole:'smth'").
                    __().
                    br().
                    __().
                    span().
                    text("}});").
                    __().
                    br().
                    __().
                    __().
                    __() //body
                            .
                    __() //html
                            .
                    toString();

            throw new RuntimeException(html);
        }

        FmsForm myFormXs = repositoryService.getMyFormXs(myProject, formKey);

        if (myFormXs.getLoginFkField() == null
                && !loginController.isUserInRole(myFormXs.getMyProject().
                getAdminAndViewerRole())) {
            throw new RuntimeException("login foreign key had not been set"
                    .concat(myFormXs.printToConfigAnalyze("smth")));
        }

        if (loginController.notMemberNotAdminNotViewer(myProject)) {

            String html = HtmlFlow.doc(System.out).
                    html().
                    body().
                    div().
                    attrClass("container").
                    br().
                    __().
                    br().
                    __().
                    ul().
                    li().
                    text(
                            "<font color='green'>'%1$s' kullanıcı adı ile giriş işlemi ve kullanıcı rollerinin belirlenmesi başarılı.</font>").
                    __().
                    li().
                    text(
                            "<font color='#A4A220'>'%1$s' kullanıcı adı '%2$s' formunun admin grubunda yer almıyor.</font>").
                    __().
                    li().
                    text(
                            "<font color='red'>veya '%1$s' kullanıcı adı ile eşleşen kurum yok.</font>").
                    __().
                    __().
                    __().
                    __() //body
                            .
                    __() //html
                            .
                    toString();

            throw new RuntimeException(String.format(html, loginController.
                            getLoggedUserDetail().
                            getUsername(),
                    myFormXs.getName()));
        }

        this.detectedDimension = myFormXs.getDimension().
                intValue();

        if (detectedDimension == 3) {
            currEnumPage = EnumPage.GENERAL_4D;
            crudFourDim.draw();
            return;
        }

        if ("simplePivot".equals(myFormXs.getFormType())) {
            filterService.createBaseFilter(myFormXs);

            FmsForm myFormLarge = repositoryService
                    .getMyFormLargeWithBaseFilter(myProject, formKey);

            if (loginController.isUserInRole(myFormLarge.getMyProject().
                    getAdminAndViewerRole())) {
                currEnumPage = EnumPage.FMS_PIVOT_2D;
            } else if (loginController.isUserInRole(myFormLarge.getMyProject().
                    getUserRole())) {
                currEnumPage = EnumPage.FMS_PIVOT_1D;
            } else {
                return;
            }
            simplePivotCtrl.init(myFormLarge);
            return;
        }

        if ("masakRisk".equals(myFormXs.getFormType())) {
            masakMB.risklerigetir();
            currEnumPage = EnumPage.MASAK_RISK;
            return;
        }

        if ("YetkiBelgeleri.java".equals(myFormXs.getFormType())) {
            createFreeForm(myFormXs, formKey);
        } else if ("DayanakVarliklari.java".equals(myFormXs.getFormType())) {
            createFreeForm2(myFormXs, formKey);
        } else if (detectedDimension == 0) {
            createPageForm(myFormXs);
        } else if (detectedDimension == 1) {
            createTableForm(myFormXs);
        } else if (detectedDimension == 2) {
            createPivotForm(myFormXs);
        } else if (detectedDimension == 10) {
            currEnumPage = EnumPage.BULK_UPLOAD_PAGE;
        } else {
            throw new RuntimeException("not supported yet");
        }

        this.historyRendered = myFormXs.getHistoryRendered();

    }

    private void createFreeForm2(FmsForm myFormXs, String formKey) throws
            MongoOrmFailedException, NullNotExpectedException {
        filterService.createBaseFilter(myFormXs);
        FmsForm myFormLarge = repositoryService
                .getMyFormLargeWithBaseFilter(myProject, formKey);
        myFormLarge.initActions(repositoryService.getAndCacheMyAction(
                myFormLarge,
                filterService.getBaseFilterCurrent()));
        if (loginController.isUserInRole(formService.getMyForm().
                getMyProject().
                getAdminAndViewerRole())) {
            currEnumPage = EnumPage.FREE_FORM_DAYANAK_VARLILARI_2D;
        } else if (loginController.isUserInRole(formService.getMyForm().
                getMyProject().
                getUserRole())) {
            currEnumPage = EnumPage.FREE_FORM_DAYANAK_VARLILARI_1D;
        } else {
            return;
        }
        freeDesigner.init(myFormLarge);
    }

    private void createFreeForm(FmsForm myFormXs, String formKey) throws
            NullNotExpectedException, MongoOrmFailedException {

        filterService.createBaseFilter(myFormXs);

        FmsForm myFormLarge = repositoryService
                .getMyFormLargeWithBaseFilter(myProject, formKey);

        myFormLarge.initActions(repositoryService.getAndCacheMyAction(
                myFormLarge,
                filterService.getBaseFilterCurrent()));

        if (loginController.isUserInRole(myFormLarge.getMyProject().
                getAdminAndViewerRole())) {
            currEnumPage = EnumPage.FREE_FORM_2D;
        } else if (loginController.isUserInRole(myFormLarge.getMyProject().
                getUserRole())) {
            currEnumPage = EnumPage.FREE_FORM_1D;
        } else {
            return;
        }
        freeDesigner.init(myFormLarge);
        formService.setMyForm(myFormLarge);
    }

    private void createPageForm(FmsForm myFormXs) throws MongoOrmFailedException,
            NullNotExpectedException, MongoOrmFailedException, Exception {

        filterService.createBaseFilter(myFormXs);

        FmsForm myFormLarge = repositoryService
                .getMyFormLargeWithBaseFilter(myProject, myFormXs.getKey());

        myFormLarge.initActions(repositoryService.getAndCacheMyAction(
                myFormLarge,
                filterService.getBaseFilterCurrent()));

        formService.setMyForm(myFormLarge);

        filterService.createTableFilterCurrent(myFormLarge);

        crudOneDim.drawGUI(myFormLarge);

        currEnumPage = EnumPage.GENERAL_1D;

        this.adminRole = loginController.isUserInRole(formService.getMyForm().
                getMyProject().
                getAdminRole());

        FacesContext.getCurrentInstance().
                getExternalContext().
                getSessionMap().
                put(SESSION_ATTR_SELECTED_FORM, formService.getMyForm());

        // check and activate history
        if (formService.getMyForm().
                getHistoryRendered()) {
            filterService.createTableFilterHistory(myFormLarge);
            if (myFormLarge.getHistoryPosition() != null) {
                this.historyPositionColumns = myFormLarge.getHistoryPosition().
                        intValue();
            }
        }

        // check and activate esign
        if (myFormLarge.getMyActions().
                isEsign()) {
            esignDoor.
                    initEsignCtrlV3(myFormLarge);
        }

        figureOutHistoryPositionStyle();

    }

    private void figureOutHistoryPositionStyle() {
        switch (historyPositionColumns) {
            case 1:
                positionStyle = "VTopHLeftW100pNowrap,VTopHLeftW100pNowrap";
                break;
            case 2:
                positionStyle = "VTopHLeftW50pNowrap,VTopHLeftW50pNowrap";
                break;
        }
    }

    private void createTableForm(FmsForm myFormXs)
            throws NullNotExpectedException, MongoOrmFailedException, Exception {

        filterService.createBaseFilter(myFormXs);
        for (String key : filterService.getBaseFilterCurrent().
                keySet()) {
            if (filterService.getGuiFilterCurrent().get(key) == null) {
                filterService.getGuiFilterCurrent().put(key, filterService.getBaseFilterCurrent().get(key));
            }
        }

        FmsForm myFormLarge = repositoryService.getMyFormLargeWithBaseFilter(
                myProject, myFormXs.getKey());

        if (myProject.isCheckJsonSchema()) {
            verify(myFormLarge);
        }

        myFormLarge.initActions(repositoryService.getAndCacheMyAction(
                myFormLarge,
                new Document(filterService.getGuiFilterCurrent())));

        formService.setMyForm(myFormLarge);

        filterService.initQuickFilters();

        twoDimModifyCtrl.drawGUI(myFormLarge);

        currEnumPage = EnumPage.GENERAL_2D;

        this.adminRole = loginController.isUserInRole(formService.getMyForm().
                getMyProject().
                getAdminRole());

        isTwoDim = true;

        FacesContext.getCurrentInstance().
                getExternalContext().
                getSessionMap().
                put(SESSION_ATTR_SELECTED_FORM, formService.getMyForm());

        // check and create history
        if (formService.getMyForm().
                getHistoryRendered()) {
            viewerTwoDim.drawGUI(myFormLarge);
            if (myFormLarge.getHistoryPosition() != null) {
                this.historyPositionColumns = myFormLarge.getHistoryPosition().
                        intValue();
            }
        } else {
            this.historyPositionColumns = 1;
        }

        // check and create esign
        if (myFormLarge.getMyActions().
                isEsign()) {
            esignDoor.
                    initEsignCtrlV3(myFormLarge);
        }

        twoDimModifyCtrl.refreshUploadedFileListAll();
        figureOutHistoryPositionStyle();

    }

    private void createPivotForm(FmsForm fmsForm) throws Exception {

        filterService.createBaseFilter(fmsForm);
        for (String key : filterService.getBaseFilterCurrent().
                keySet()) {
            if (filterService.getGuiFilterCurrent().
                    get(key) == null) {
                filterService.getGuiFilterCurrent().
                        put(key, filterService.getBaseFilterCurrent().
                                get(key));
            }
        }

        FmsForm myFormLarge = repositoryService.getMyFormLargeWithBaseFilter(
                myProject, fmsForm.getKey());

        if (myProject.isCheckJsonSchema()) {
            verify(myFormLarge);
        }

        myFormLarge.initActions(repositoryService.getAndCacheMyAction(
                myFormLarge,
                new Document(filterService.getGuiFilterCurrent())));

        formService.setMyForm(myFormLarge);

        filterService.createPivotFilterCurrent();
        pivotModifierCtrl.cleanPivotData();
        pivotModifierCtrl.drawGUI();

        currEnumPage = EnumPage.GENERAL_ND;

        this.adminRole = loginController.isUserInRole(formService.getMyForm().
                getMyProject().
                getAdminRole());

        // check and activate esign
        if (myFormLarge.getMyActions().
                isEsign()) {
            esignDoor.
                    initEsignCtrlV3(myFormLarge);
        }

        // check and activate history
        if (Boolean.TRUE.equals(formService.getMyForm().
                getHistoryRendered())) {
            if (myFormLarge.getHistoryPosition() != null) {
                this.historyPositionColumns = myFormLarge.getHistoryPosition().
                        intValue();
            }
            filterService.createPivotFilterHistory();
            pivotViewerCtrl.cleanPivotData();
            pivotViewerCtrl.drawGUI();
        }

        figureOutHistoryPositionStyle();

    }

    private void readFiles() {
        File pathMongoDbExport = new File(baseService.getProperties().
                getTmpDownloadPath());

        try {
            if (!pathMongoDbExport.exists()) {
                pathMongoDbExport.mkdirs();
            }

            if (!pathMongoDbExport.isDirectory()) {
                throw new Exception("FileServlet init param 'basePath' value '"
                        + pathMongoDbExport + "' is actually not a directory in file system.");
            } else if (!pathMongoDbExport.canRead()) {
                throw new Exception("FileServlet init param 'basePath' value '"
                        + pathMongoDbExport + "' is actually not readable in file system.");
            }

            List<Map> lists = new ArrayList<>();
            Map<String, String> object;
            for (String fileName : pathMongoDbExport.list()) {
                object = new HashMap();
                object.put(NAME, fileName);
                object.put(PATH, "uysexport/".concat(URLEncoder.encode(fileName,
                        "UTF-8")));
                lists.add(object);
            }

            mongoDbExportedFiles = new ListDataModel(lists);

        } catch (Exception ex) {
            logger.error("error occured", ex);
        }
    }

    /**
     * Geriye Appletin Pfx Dosya ile mi yoksa SmartCard'la mı imza atılacağı
     * bilgisini döndürür
     *
     * @return CARD | PFX
     */
    public String getImzaType() {
        esignDoor.eimzaContextInstance(baseService.
                getEsignProperties());
        return esignDoor.getSignType();
    }

    /**
     * Geriye appletin test için mi gerçek imza mı atacağı bilgisini döndürür
     *
     * @return
     */
    public String getImzaTest() {
        esignDoor.eimzaContextInstance(baseService.
                getEsignProperties());
        return esignDoor.isTest() ? "TRUE" : "FALSE";
    }

    public String getLogoLarge() {
        return baseService.getLogoLarge();
    }

    public String getLogoSmall() {
        return baseService.getLogoSmall();
    }

    public String getCompanyTitle() {
        return baseService.getProperties().
                getTitle();
    }

    public String getCopyrightInfo() {
        return baseService.getCopyrightInfo();
    }

    public String getSelectedFormInfo() {
        return selectedFormInfo;
    }

    public void setSelectedFormInfo(String selectedFormInfo) {
        this.selectedFormInfo = selectedFormInfo;
    }

    public List getAccordionItems() {
        return Collections.unmodifiableList(accordionItems);
    }

    public Integer getPosition() {
        return historyPositionColumns;
    }

    public void setPosition(Integer position) {
        this.historyPositionColumns = position;
    }

    public String getMyStyle() {
        return myStyle;
    }

    public void setMyStyle(String myStyle) {
        this.myStyle = myStyle;
    }

    public Map<String, Object> getAddInfoMap() {
        return Collections.unmodifiableMap(addInfoMap);
    }

    public void setAddInfoMap(Map<String, Object> addInfoMap) {
        this.addInfoMap = addInfoMap;
    }

    public boolean getWhatIsYouAnswer() {
        return addInfoMap == null || HAYIR.equals(addInfoMap.get(
                "is_spk_operatable"));
    }

    public Boolean getFlowPanelPopup() {
        return flowPanelPopup;
    }

    public void setFlowPanelPopup(Boolean flowPanelPopup) {
        this.flowPanelPopup = flowPanelPopup;
    }

    public String getPopupMessage() {
        return popupMessage;
    }

    public void setPopupMessage(String popupMessage) {
        this.popupMessage = popupMessage;
    }

    public LazyDataModel<Map> getData() {
        return data;
    }

    public void setData(LazyDataModel<Map> data) {
        this.data = data;
    }

    /**
     *
     * @return
     */
    public boolean getRenderedDesktop() {
        if (formService.getMyForm() != null
                && formService.getMyForm().
                getMyActions() != null
                && formService.getMyForm().
                getMyActions().
                isEsign()) {

            if (loginController.isUserInRole(formService.getMyForm().
                    getMyProject().
                    getAdminRole())) {
                return true;
            }

            if (loginController.isUserInRole(formService.getMyForm().
                    getMyProject().
                    getViewerRole())) {
                return false;
            }
        }
        return true;

    }

    public boolean isWelcome() {
        return welcome;
    }

    public void setWelcome(boolean welcome) {
        this.welcome = welcome;
    }

    public Boolean getAdminRole() {
        return adminRole;
    }

    /**
     * @return the isTwoDimensionView
     */
    public boolean isIsTwoDimensionView() {
        return definedDimensionViewMeasure == 2 && detectedDimension == 2;
    }

    private boolean isTwoDim;

    /**
     * @return the isTwoDimensionView
     */
    public boolean isIsTwoDim() {
        return isTwoDim;
    }

    public String getAboutContent() {
        return aboutContent;
    }

    /**
     * @return the moduleItem
     */
    public ModuleItem getModuleItem() {
        return moduleItem;
    }

    public boolean isHistoryRendered() {
        return historyRendered;
    }

    public MyMap getCallBackCrud() {
        return callBackCrud;
    }

    public void setCallBackCrud(MyMap callBackCrud) {
        this.callBackCrud = callBackCrud;
    }

    public FmsHref getFmsHref() {
        return fmsHref;
    }

    public void setFmsHref(FmsHref fmsHref) {
        this.fmsHref = fmsHref;
    }

    /**
     * @return the hasSetToRelatedForm
     */
    public boolean isHasSetToRelatedForm() {
        return hasSetToRelatedForm;
    }


    public String getVersion() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        Object servletContext = facesContext.getExternalContext().getContext();

        String version = "n/a";

        try (InputStream is = ((jakarta.servlet.ServletContext) servletContext)
                .getResourceAsStream("/WEB-INF/classes/git.properties")) {

            if (is != null) {
                Properties prop = new Properties();
                prop.load(is);
                version = String.format("version: %s, branch: %s, time: %s",
                        prop.getProperty("git.build.version"),
                        prop.getProperty("git.branch"),
                        prop.getProperty("git.build.time"));
            } else {
                // File missing case
                facesContext.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "Configuration Warning",
                        "Version info file (git.properties) was not found."
                ));
            }

        } catch (IOException ex) {
            // Stream read failure case
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Error",
                    "Failed to load system version information."
            ));
        }

        return version;
    }

}
