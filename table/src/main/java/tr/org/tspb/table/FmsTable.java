package tr.org.tspb.table;

import static tr.org.tspb.constants.ProjectConstants.*;
//
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Type;
import java.util.Set;
//
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
//
import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoWriteException;
import com.mongodb.client.model.Filters;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.bson.conversions.Bson;
//
import tr.org.tspb.common.qualifier.MyCtrlServiceQualifier;
import tr.org.tspb.datamodel.dao.ChildFilter;
import tr.org.tspb.datamodel.dao.MyBaseRecord;
import tr.org.tspb.datamodel.dao.TagEvent;
import tr.org.tspb.constants.exceptions.MongoOrmFailedException;
import tr.org.tspb.datamodel.pojo.PreSaveResult;
import tr.org.tspb.factory.qualifier.OgmCreatorQualifier;
import tr.org.tspb.service.CtrlService;
import tr.org.tspb.factory.cp.OgmCreatorIntr;
import tr.org.tspb.datamodel.pojo.PostSaveResult;
import tr.org.tspb.service.CalcService;
import tr.org.tspb.util.tools.MongoDbVersion;
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.common.services.LoginController;
import tr.org.tspb.util.crypt.RandomString;
import tr.org.tspb.common.services.LdapService;
import tr.org.tspb.common.services.MailService;
import tr.org.tspb.service.RepositoryService;
import tr.org.tspb.constants.exceptions.FormConfigException;
import tr.org.tspb.constants.exceptions.LdapException;
import tr.org.tspb.constants.exceptions.UserException;
import tr.org.tspb.converter.props.MessageBundleLoaderv1;
import tr.org.tspb.datamodel.dao.MyField;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.FmsFieldItems;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.datamodel.dao.MyNotifies;
import tr.org.tspb.datamodel.dp.nullobj.PlainRecordData;
import tr.org.tspb.datamodel.pojo.UserDetail;

/**
 *
 * @author Telman Şahbazoğlu
 */
public abstract class FmsTable extends FmsTableView {

    @Inject
    LdapService ldapService;

    @Inject
    MailService mailService;

    @Inject
    RepositoryService repositoryService;

    @Inject
    @MyCtrlServiceQualifier
    CtrlService ctrlService;

    @Inject
    @OgmCreatorQualifier
    private OgmCreatorIntr ogmCreator;

    @Inject
    protected CalcService calcService;

    protected MyMap crudObject;

    private Map<String, MyField> componentMap = new HashMap();
    private Map<String, MyField> componentMapChilds = new HashMap();

    private String crudObjectTextViewer;
    private String imz64;
    private UploadedFile uploadedFile;

    // mapRequired
    protected Map mapRequired = new HashMap();

    private final static String WRITE_TO = "GRIDFS_DB";
    private List<Map<String, String>> myImzas;

    private int fileLimit = 1;
    private String mongoUploadFileType = "/(\\.|\\/)(pdf)$/";
    private String invalidFileMessage = "Geçersiz Dosya Tipi (Sadece PDF dosyalar eklenebilir) : ";
    protected String toBeDeletedFileID;
    private boolean enableHistoryOnSave = true;
    private String headerTitle;
    protected List<Map<String, String>> listFileData = new ArrayList<>();
    protected List<Map<String, String>> fileListAll = new ArrayList<>();
    private List versionHistory = new ArrayList<>();
    private List historyColumnModel = new ArrayList();

    public static final String WV_BULK_COPY = "wv-bulk-copy";
    public static final String CRUD_OPERATION_DIALOG2 = "crudOperationDialog2";
    public static final String DLG_CRUD_JSON = "wv-dlg-crud-json";

    Gson gsonConverter = new Gson();
    Type gsonType = new TypeToken<List<String>>() {
    }.getType();

    @PostConstruct
    public void init() {
        crudObject = ogmCreator.getCrudObject();
    }

    public Map<String, MyField> getComponentMap() {
        return Collections.unmodifiableMap(componentMap);
    }

    public void addComponent(String key, MyField myField) {
        this.componentMap.put(key, myField);
    }

    public void addComponentChild(String key, MyField myField) {
        this.componentMapChilds.put(key, myField);
    }

    public void setComponentMap(Map<String, MyField> componentMap) {
        this.componentMap = componentMap;
    }

    public boolean runEventPreSave(Map query, MyMap crud) {

        FmsForm fmsForm = formService.getMyForm();

        if (fmsForm.getEventPreSave() == null) {
            return false;
        }

        TagEvent tagEventPreSave = fmsForm.getEventPreSave();
        TagEvent.TagEventType type = tagEventPreSave.getType();

        Object result = null;
        if (type == null || type == TagEvent.TagEventType.nothing) {
            String eventPreSaveDB = fmsForm.getEventPreSave().getDb();
            if (eventPreSaveDB == null) {
                dialogController.showPopupInfoWithOk("""
                                             <ul>
                                             <li>
                                             <font color='red'>Kaydetme İşlemi Gerçekleştirilemedi.</font>
                                             </li> 
                                             <li>Konfigürasyon Hatası : db tanımlı değil.</li>
                                             </ul>                        
                        """, MESSAGE_DIALOG);
                return true;
            }
            Document myCrudObject = new Document(crud);
            myCrudObject.remove(INODE);// we remove it bacuase of MyForm class cannot be serialized for mongo.doEval
            String code = fmsForm.getEventPreSave().getJsFunction();
            Document commandResult = mongoDbUtil.runCommand(eventPreSaveDB, code, query, myCrudObject);
            result = commandResult.get(RETVAL);
        } else {
            switch (type) {
                case externalApi -> {
                    try {
                        PreSaveResult preSaveResult = repositoryService.runEventPreSaveByGivenTagEvent(
                                fmsForm.getMyProject().getKey(),
                                fmsForm.getMyProject().getApiToken(),
                                tagEventPreSave,
                                new Document(crud));

                        if (!preSaveResult.isResult()) {
                            result = new Document()
                                    .append("popupMessage", preSaveResult.getMsg());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (Boolean.TRUE.equals(result)) {
            //FIXME messagebundle
            dialogController.showPopupInfoWithOk("<ul>" + "<li><font color='red'>Kaydetme İşlemi Gerçekleştirilemedi.</font></li>" + "<li>\"Birlik Temsilcisi\" yalnız bir defa seçilebilmektedir. <br/>Daha önce seçim yaptınız.</li>" + "</ul>", MESSAGE_DIALOG);
            return true;
        }

        if (result instanceof Document resultJSON) {
            if ("facesMessage".equals(resultJSON.get("gui"))) {
                String mssssage = resultJSON.get("facesMessage").toString();
                FacesMessage.Severity severity;
                switch (resultJSON.get("facesMessageSeverity").toString()) {
                    case "error":
                        severity = FacesMessage.SEVERITY_ERROR;
                        break;
                    case "info":
                        severity = FacesMessage.SEVERITY_INFO;
                        break;
                    case "warn":
                        severity = FacesMessage.SEVERITY_WARN;
                        break;
                    default:
                        severity = FacesMessage.SEVERITY_INFO;
                        break;
                }
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, mssssage, "*"));
            } else {
                String mssssage = resultJSON.get("popupMessage").toString();
                dialogController.showPopupInfoWithOk(mssssage, MESSAGE_DIALOG);
            }
            return true;
        }

        return false;
    }

    public boolean runEventPreSaveOnChild(Map query, MyMap crud) {
        FmsForm fmsForm = formService.getMyForm();

        TagEvent tagEventPreSaveOnChild = fmsForm.getEventPreSaveOnChild();

        if (tagEventPreSaveOnChild == null) {
            return false;
        }

        TagEvent.TagEventType type = tagEventPreSaveOnChild.getType();
        Object result = null;
        if (type == null || type == TagEvent.TagEventType.nothing) {
            String eventPreSaveDB = tagEventPreSaveOnChild.getDb();
            if (eventPreSaveDB == null) {
                //FIXME messagebundle
                dialogController.showPopupInfoWithOk("<ul>" + "<li><font color='red'>Kaydetme İşlemi Gerçekleştirilemedi.</font></li>" + "<li>Konfigürasyon Hatası : db tanımlı değil.</li>" + "</ul>", MESSAGE_DIALOG);
                return true;
            }
            Document myCrudObject = new Document(crud);
            myCrudObject.remove(INODE);// we remove it bacuase of MyForm class cannot be serialized for mongo.doEval
            String code = tagEventPreSaveOnChild.getJsFunction();
            Document commandResult = mongoDbUtil.runCommand(eventPreSaveDB, code, query, myCrudObject);
            result = commandResult.get(RETVAL);
        } else {
            switch (type) {
                case externalApi -> {
                    try {
                        PreSaveResult preSaveResult = repositoryService.runEventPreSaveByGivenTagEvent(
                                fmsForm.getMyProject().getKey(),
                                fmsForm.getMyProject().getApiToken(),
                                tagEventPreSaveOnChild,
                                new Document(crud));

                        if (!preSaveResult.isResult()) {
                            result = new Document()
                                    .append("popupMessage", preSaveResult.getMsg());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (Boolean.TRUE.equals(result)) {
            //FIXME messagebundle
            dialogController.showPopupInfoWithOk("<ul>" + "<li><font color='red'>Kaydetme İşlemi Gerçekleştirilemedi.</font></li>" + "<li>\"Birlik Temsilcisi\" yalnız bir defa seçilebilmektedir. <br/>Daha önce seçim yaptınız.</li>" + "</ul>", MESSAGE_DIALOG);
            return true;
        }
        if (result instanceof Document) {
            Document resultJSON = (Document) result;
            if ("facesMessage".equals(resultJSON.get("gui"))) {
                String mssssage = resultJSON.get("facesMessage").toString();
                FacesMessage.Severity severity;
                switch (resultJSON.get("facesMessageSeverity").toString()) {
                    case "error":
                        severity = FacesMessage.SEVERITY_ERROR;
                        break;
                    case "info":
                        severity = FacesMessage.SEVERITY_INFO;
                        break;
                    case "warn":
                        severity = FacesMessage.SEVERITY_WARN;
                        break;
                    default:
                        severity = FacesMessage.SEVERITY_INFO;
                        break;
                }
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, mssssage, "*"));
            } else {
                String mssssage = resultJSON.get("popupMessage").toString();
                dialogController.showPopupInfoWithOk(mssssage, MESSAGE_DIALOG);
            }
            return true;
        }

        return false;
    }

    public String getInvalidFileMessage() {
        return invalidFileMessage;
    }

    public void setInvalidFileMessage(String invalidFileMessage) {
        this.invalidFileMessage = invalidFileMessage;
    }

    /**
     * @return the mongoUploadFileType
     */
    public String getMongoUploadFileType() {
        return mongoUploadFileType;
    }

    /**
     * @param mongoUploadFileType the mongoUploadFileType to set
     */
    public void setMongoUploadFileType(String mongoUploadFileType) {
        this.mongoUploadFileType = mongoUploadFileType;
    }

    public int getFileLimit() {
        return fileLimit;
    }

    public void setFileLimit(int fileLimit) {
        this.fileLimit = fileLimit;
    }

    public List<Map<String, String>> getMyEimzas() {
        return Collections.unmodifiableList(myImzas);
    }

    public String getCrudObjectTextViewer() {
        return crudObjectTextViewer;
    }

    public String getImz64() {
        return imz64;
    }

    public void setImz64(String imz64) {
        this.imz64 = imz64;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public void upload(FileUploadEvent event) {
        uploadedFile = event.getFile();

        try {
            switch (WRITE_TO) {
                case "GRIDFS_DB" -> {
                    Document metadata = new Document();

                    if (crudObject.get("_id") instanceof ObjectId) {
                        metadata.put(CRUD_OBJECT_ID, (ObjectId) crudObject.get("_id"));
                    } else {
                        // null points to the fact that this record(file) is not related yet.
                        // we will use this NULL state during the search over all other forms
                        metadata.put(CRUD_OBJECT_ID, null);
                    }

                    metadata.put(SELECT_FORM_KEY, formService.getMyForm().getKey());
                    metadata.put("selectFormName", formService.getMyForm().getName());
                    metadata.put("username", loginController.getLoggedUserDetail().getUsername());

                    String uploadTable = baseService.getProperties().getUploadTable();

                    String fileName = uploadedFile.getFileName();

                    try (InputStream inputStream = uploadedFile.getInputStream()) {
                        ObjectId fileId = mongoDbUtil.createFile(uploadTable, fileName, inputStream, metadata);
                    }
                }
                case "FILESISTEM" -> {
                }
                case "FILESISTEM_AND_DATABASE" -> {
                }
                default -> {
                }
            }
        } catch (Exception ex) {
            logger.error("error occured", ex);
            dialogController.showPopupError(ex.toString());
        }

        refreshUploadedFileList();

    }

    public String getToBeDeletedFileID() {
        return toBeDeletedFileID;
    }

    public void setToBeDeletedFileID(String toBeDeletedFileID) {
        this.toBeDeletedFileID = toBeDeletedFileID;
    }

    public String deleteFile() {
        deleteFile(toBeDeletedFileID);
        return null;
    }

    private String deleteFile(String objectId) {
        if (formService.getMyForm().getMyActions().isDelete()) {
            mongoDbUtil.removeFile(baseService.getProperties().getUploadTable(), new ObjectId(objectId));
            refreshUploadedFileList();
        }
        return null;
    }

    protected void refreshUploadedFileList() {
        Object idValue = this.crudObject.get(MONGO_ID);
        logger.info("refreshUploadedFileList for  : " + idValue);

        ObjectId objectId = null;

        if (idValue instanceof ObjectId id) {
            objectId = id;
        } else if (idValue instanceof MyBaseRecord record) {
            objectId = record.getObjectId();
        }

        if (objectId == null) {
            listFileData = new ArrayList<>();
        } else {
            logger.info("get file list for record : " + objectId);
            listFileData = repositoryService.findGridFsFileList(objectId);
        }
        refreshUploadedFileListAll();
    }

    public void refreshUploadedFileListAll() {
        this.fileListAll = repositoryService.findGridFsFileList(formService.getMyForm());
    }

    public void resetMyObject() {
        this.crudObject = ogmCreator.getCrudObject();
    }

    public MyMap getMyObject() {
        return crudObject;
    }

    public void setMyObject(MyMap myObject) {
        this.crudObject = myObject;
    }

    public boolean isEnableHistoryOnSave() {
        return enableHistoryOnSave;
    }

    public void setEnableHistoryOnSave(boolean enableHistoryOnSave) {
        this.enableHistoryOnSave = enableHistoryOnSave;
    }

    public String prepareQuery(MyMap crudObject) {
        return null;
    }

    public List<Map<String, String>> getFileData() {
        return Collections.unmodifiableList(listFileData);
    }

    public List<Map<String, String>> getListFileData() {
        return listFileData;
    }

    public List<Map<String, String>> getFileListAll() {
        return Collections.unmodifiableList(fileListAll);
    }

    public ObjectId saveObjectFromPaymentService(Document operatedObject, String username, FmsForm myForm, String ip) throws Exception {
        if (!"payment-service-user".equals(username)) {
            throw new RuntimeException("Upsss ...");
        }
        return saveOneDimensionObject(operatedObject, username, myForm, ip, "payment-service-no-session");
    }

    public ObjectId saveOneDimensionObject(Document operatedObject, String username, FmsForm myForm, String ip, String sessionId) throws MessagingException, NullNotExpectedException, LdapException, FormConfigException, MongoOrmFailedException, UserException {

        FmsForm inode = (FmsForm) operatedObject.get(INODE);
        operatedObject.remove(INODE);//just to sutisfy the icefaces
        if (inode == null) {
            inode = myForm;
        }

        ctrlService.checkRecordConverterValueType(operatedObject, myForm);

        if (myForm.isHasAttachedFiles() && (listFileData == null || listFileData.isEmpty())) {
            for (MyField field : myForm.getFields().values()) {
                if (field.isRequired() && getInputFile().equals(field.getComponentType())) {
                    FacesMessage facesMessageRequired = new FacesMessage(//
                            FacesMessage.SEVERITY_ERROR, //
                            MessageFormat.format("[{0}] {1}", field.getShortName(), MessageBundleLoaderv1.getMessage("requiredMessage")),//
                            "*");
                    FacesContext.getCurrentInstance().addMessage(null, facesMessageRequired);
                    throw new UserException("<br/><br/> Dosya Eksik");
                }
            }
        }

        operatedObject = repositoryService.expandCrudObject(myForm, operatedObject);

        operatedObject.put(OPERATOR_LDAP_UID, username);
        operatedObject.put(FORMS, myForm.getForm());

        if (myForm.getFindAndSaveFilter() != null) {
            operatedObject.putAll(myForm.getFindAndSaveFilter());
        }

        Document uysAdditionalMetaData = (Document) operatedObject.get(ADMIN_METADATA);
        if (uysAdditionalMetaData == null) {
            uysAdditionalMetaData = new Document();
            operatedObject.put(ADMIN_METADATA, uysAdditionalMetaData);
            uysAdditionalMetaData.put(CREATE_USER, username);
            uysAdditionalMetaData.put(CREATE_DATE, new Date());
            uysAdditionalMetaData.put(CREATE_SESSIONID, sessionId);
        } else {
            uysAdditionalMetaData.put(UPDATE_DATE, new Date());
            uysAdditionalMetaData.put(UPDATE_USER, username);
        }

        for (MyField myField : inode.getAutosetFields()) {
            Object value = operatedObject.get(myField.getKey());
            if (value == null) {
                operatedObject.put(myField.getKey(), filterService.getTableFilterCurrent().get(myField.getKey()));
            }
        }

        for (String fieldKey : operatedObject.keySet()) {
            MyField fieldStriucture = myForm.getField(fieldKey);
            if (fieldStriucture == null) {
                continue;
            }
            Object fieldValue = operatedObject.get(fieldKey);
            Object defaultValue = fieldStriucture.getDefaultValue();
            if (defaultValue != null && (fieldValue == null || "".equals(fieldValue))) {
                operatedObject.put(fieldKey, defaultValue);
            }
        }

        for (String fieldKey : myForm.getFieldsKeySet()) {
            MyField myField = myForm.getField(fieldKey);
            if (myField.getCalculateOnSave()) {
                operatedObject.put(fieldKey, calcService.calculateValue(operatedObject, myField, FacesContext.getCurrentInstance()));
            }
        }

        String operatorLdapUID = username;

        Document result;

        if (operatedObject.get(MONGO_ID) != null) {

            Bson query = Filters.eq(MONGO_ID, operatedObject.get(MONGO_ID));

            mongoDbUtil.updateOne(inode.getDb(), inode.getTable(), query, operatedObject);

            result = mongoDbUtil.findOne(inode.getDb(), inode.getTable(), query);
        } else {
            // still no way to get the just inserted object id. 
            // we dont wont to create id on java side. we want to leave this job to mongodb.
            // for ease retrieving the just inserted object we add an additonal retrieve InsertId to object
            // it can be easly removed later.

            String toBeRetrivedValue = String.format("s:%s_r:%s_t:%s_u:%s_c:%s", sessionId,//
                    new RandomString(32).nextString(),//
                    new Date().getTime(),//
                    username,//
                    myForm.getTable());

            Document record = new Document(operatedObject).append(UYS_EASY_FIND_KEY, toBeRetrivedValue);

            try {

                for (String key : record.keySet()) {
                    if (record.get(key) instanceof Object[]) {
                        Object[] multiValues = (Object[]) record.get(key);
                        List<String> list = new ArrayList<>();
                        for (Object obj : multiValues) {
                            list.add(obj.toString());
                        }
                        record.put(key, list);
                    }
                }
                mongoDbUtil.insertOne(inode.getDb(), inode.getTable(), record);
            } catch (MongoWriteException ex) {
                throw new FormConfigException(ex.getMessage(), ex);
            }

            result = mongoDbUtil.findOne(inode.getDb(), inode.getTable(), new Document(UYS_EASY_FIND_KEY, toBeRetrivedValue));

            operatedObject.append(MONGO_ID, record.get(MONGO_ID));

        }

        for (String fieldKey : myForm.getFieldsKeySet()) {
            MyField myField = myForm.getField(fieldKey);
            if (myField.getCalculateAfterSave()) {
                result.put(fieldKey, calcService.calculateValue(operatedObject, myField, FacesContext.getCurrentInstance()));
            }
        }

        if (enableHistoryOnSave) {
            try {
                MongoDbVersion.instance(mongoDbUtil).archive(//
                        inode.getDb(),//
                        inode.getVersionCollection(),//
                        myForm.getKey(),//
                        result, //
                        ip, //
                        operatorLdapUID,//
                        inode.getVersionFields());
            } catch (Exception ex) {
                logger.error("error occured", ex);
            }
        }

        //begin : provide uploaded file relation
        if (!listFileData.isEmpty()) {
            List<ObjectId> listOfFileIDs = new ArrayList<>();

            for (Map map : listFileData) {
                listOfFileIDs.add(new ObjectId((String) map.get(FILE_ID)));
            }

            mongoDbUtil.updateMany(baseService.getProperties().getUploadTable(), "fs.files", new Document(MONGO_ID, new Document(DOLAR_IN, listOfFileIDs)), new Document(METADATA_CRUD_OBJECT_ID, result.get(MONGO_ID)));

            if ("iondb".equals(myForm.getDb()) && !myForm.isHasAttachedFiles()) {
                /*
                 eimza bidirim formlarında 
                 eğer formun eki ajax olarak schemadan kaldırıldıysa
                 kayderken daha önceden eklenen ekleri sil. aksi takdirde bunlar eimzaya yansıyor
                 */

                mongoDbUtil.removeFile(baseService.getProperties().getUploadTable(), new BasicDBObject().append(METADATA_CRUD_OBJECT_ID, result.get(MONGO_ID)).append("metadata.username", username));
            }
        }
        //end : provide uploaded file relation

        try {
            PostSaveResult postSaveResult = repositoryService.runEventPostSave(operatedObject, myForm, null);
            //FIXME messagebundle
            if (postSaveResult.getMsg() != null) {
                dialogController.showPopupInfoWithOk(postSaveResult.getMsg(), MESSAGE_DIALOG);
            }
        } catch (Exception ex) {
            logger.error("error occured", ex);
            StringBuilder dlgSb = new StringBuilder();
            dlgSb.append("Kayıt Sonrası tetikleyici çalıştırılıyor iken bir hata oluştu. ");
            dlgSb.append("<br/><br/>");
            dlgSb.append("Lütfen bu durumu sistem yöneticisine bildiriniz.");
//            dialogController.showPopupError(dlgSb.toString());
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Hata", dlgSb.toString().replace("<br/>", "")));
        }

        if (formService.getMyForm().getMyNotifies() != null) {
            for (MyNotifies myNotifies : formService.getMyForm().getMyNotifies().getList()) {
                myNotifies.reEnable(crudObject);
                myNotifies.reTo(crudObject);
                myNotifies.reSubject(crudObject);
                myNotifies.reContent(crudObject);
                if (myNotifies.isEnable() && myNotifies.isEmail()) {
                    mailService.sendMail(myNotifies.getSubject(), myNotifies.getContent(), myNotifies.getTo());
                }
            }
        }

        return (ObjectId) result.get(MONGO_ID);
    }

    protected String deleteObject(LoginController loginMB, FmsForm myForm, MyMap crudObject) throws Exception {
        String collection = myForm.getTable();

        ObjectId objectID = (ObjectId) crudObject.get(MONGO_ID);
        if (objectID != null) {

            Document toBeDeleted = repositoryService.expandCrudObject(myForm, new Document(crudObject));

            mongoDbUtil.trigger(toBeDeleted, myForm.getEventPreDelete(), loginController.getRolesAsList());

            if (myForm.getDeleteChildsOnDelete()) {
                deleteChilds(myForm, objectID);
            } else {
                checkChilds(myForm, objectID);
            }

            mongoDbUtil.deleteMany(myForm.getDb(), collection, new Document(MONGO_ID, objectID));

            TagEvent postDelete = myForm.getEventPostDelete();
            if (postDelete != null && TagEvent.TagEventType.externalApi.equals(postDelete.getType())) {

                String memberIdAsStr = ((ObjectId) crudObject.get("member")).toHexString();
                String periodIdAsStr = ((ObjectId) crudObject.get("period")).toHexString();

                Map<String, Object> requestPayload = new HashMap<>();
                requestPayload.put("member", memberIdAsStr);
                requestPayload.put("period", periodIdAsStr);
                requestPayload.put("table", myForm.getTable());

                String TARGET_URL = "http://localhost:8080" + postDelete.getUri();

                // 2. Instantiate the native Jakarta REST client worker engine
                try (Client client = ClientBuilder.newClient()) {
                    // 3. Dispatch the HTTP POST execution payload over the wire
                    Response response = client.target(TARGET_URL)
                            .request(MediaType.APPLICATION_JSON)
                            .header("X-API-KEY", myForm.getMyProject().getApiToken()).header("X-API-PROJECT", myForm.getMyProject().getKey())
                            .post(Entity.entity(requestPayload, MediaType.APPLICATION_JSON));
                    // 4. Validate output response signals cleanly
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        String jsonResponse = response.readEntity(String.class);
                        System.out.println("Success response signature received from service: " + jsonResponse);
                    } else {
                        System.err.println("Failed to execute. HTTP Status Code: " + response.getStatus());
                        System.err.println("Error output detail: " + response.readEntity(String.class));
                    }
                    response.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            } else {
                mongoDbUtil.trigger(repositoryService.expandCrudObject(myForm, new Document(crudObject)), myForm.getEventPostDelete(), loginController.getRolesAsList());
            }


            for (Map entry : listFileData) {
                deleteFile(entry.get("fileID").toString());
            }

            crudObject = ogmCreator.getCrudObject();

        }
        dialogController.hidePopup(CRUD_OPERATION_DIALOG2);

        return null;
    }

    private void checkChilds(FmsForm myForm, ObjectId objectID) throws Exception {
        List<ChildFilter> childFilters = myForm.getChilds();
        for (ChildFilter childFilter : childFilters) {
            if (mongoDbUtil.findOne(childFilter.getDb(), childFilter.getTable(), Filters.eq(childFilter.getFieldKey(), objectID)) != null) {
                throw new Exception(childFilter.print(objectID));
            }
        }
    }

    private void deleteChilds(FmsForm myForm, ObjectId objectID) throws Exception {
        List<ChildFilter> childFilters = myForm.getChilds();
        for (ChildFilter childFilter : childFilters) {
            mongoDbUtil.deleteMany(childFilter.getDb(), childFilter.getTable(), new Document(childFilter.getFieldKey(), objectID));
        }
    }

    public String copyObject(FmsForm myForm, LoginController loginMB, MyMap crudObject) throws Exception {

        Document operatedObject = new Document(crudObject);

        operatedObject.remove(MONGO_ID);

        if (!(loginController.isUserInRole(formService.getMyForm().getMyProject().getAdminRole()) //
                || crudObject.get(formService.getMyForm().getLoginFkField()) == null || loginMB.getLoggedUserDetail().getDbo().getObjectId().equals(operatedObject.get(formService.getMyForm().getLoginFkField())))) {
            throw new Exception("Sisteme girş yapan kullanıcı yalnızca kendisine ait veri ekleyip değiştirebilir.");
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        String sessionId = ((HttpSession) facesContext.getExternalContext().getSession(false)).getId();

        saveOneDimensionObject(operatedObject, loginMB.getLoggedUserDetail().getUsername(), formService.getMyForm(), request.getRemoteAddr(), sessionId);

        crudObject.put(STATE, "saved");
        return null;
    }

    public ObjectId saveObject(FmsForm myForm, LoginController loginMB, MyMap crudObject) throws UserException, MessagingException, NullNotExpectedException, LdapException, FormConfigException, MongoOrmFailedException {

        Object loginFkFieldValue = crudObject.get(formService.getMyForm().getLoginFkField());

        if (loginFkFieldValue instanceof MyBaseRecord) {
            loginFkFieldValue = ((MyBaseRecord) loginFkFieldValue).getObjectId();
        }

        boolean ok = loginController.isUserInRole(formService.getMyForm().getMyProject().getAdminRole());

        ok = ok || loginController.getLoggedUserDetail().getDbo().getObjectId().equals(loginFkFieldValue);

        if (!ok) {
            for (UserDetail.EimzaPersonel ep : loginController.getLoggedUserDetail().getEimzaPersonels()) {
                if (ep.getDelegatingMember() != null && ep.getDelegatingMember().equals(loginFkFieldValue)) {
                    ok = true;
                    break;
                }
            }
        }

        if (!ok) {
            throw new UserException("Sisteme girş yapan kullanıcı yalnızca kendisine ait veri ekleyip değiştirebilir.");
        }

        Document operatedObject = new Document(crudObject);

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        String sessionId = ((HttpSession) facesContext.getExternalContext().getSession(false)).getId();

        ObjectId returnID = saveOneDimensionObject(operatedObject, loginMB.getLoggedUserDetail().getUsername(), formService.getMyForm(), request.getRemoteAddr(), sessionId);
        crudObject.put(STATE, "saved");

        return returnID;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public void setHeaderTitle(String headerTitle) {
        this.headerTitle = headerTitle;
    }

    public List getVersionHistory() {
        return Collections.unmodifiableList(versionHistory);
    }

    public List getHistoryColumnModel() {
        return Collections.unmodifiableList(historyColumnModel);
    }

    protected MyMap prepareCrudObject(Map rowData) {

        MyMap crudObject = ogmCreator.getCrudObject();

        crudObject.clear();

        //FIXME Why?
        if (Boolean.TRUE.equals(rowData.get(CALCULATE))) {
            return null;
        }

        crudObject.putAll(rowData);

        for (String key : (Set<String>) rowData.keySet()) {

            MyField myField = formService.getMyForm().getField(key);

            if (myField != null && myField.isAutoComplete()) {

                FmsFieldItems myItems = myField.getItemsAsMyItems();

                Document doc = mongoDbUtil.findOne(myItems.getDb(), myItems.getTable(), Filters.eq(MONGO_ID, rowData.get(key)));

                crudObject.put(key, PlainRecordData.getPlainRecord(doc, myItems));
            }
        }

        if (formService.getMyForm().getVersionCollection() != null) {

            ObjectId objectId = null;

            if (crudObject.get(MONGO_ID) instanceof ObjectId) {
                objectId = (ObjectId) crudObject.get(MONGO_ID);
            } else if (crudObject.get(MONGO_ID) instanceof MyBaseRecord) {
                objectId = ((MyBaseRecord) crudObject.get(MONGO_ID)).getObjectId();
            }

            Map<String, List> map = MongoDbVersion.instance(mongoDbUtil).fetch(//
                    formService.getMyForm(),//
                    formService.getMyForm().getDb(), //
                    formService.getMyForm().getVersionCollection(), //
                    objectId, formService.getMyForm().getVersionFields());

            historyColumnModel = map.get(COLUMN_LIST);
            versionHistory = map.get(ROW_LIST);
        }
        return crudObject;
    }

    public void resetHistory() {
        historyColumnModel = new ArrayList();
        versionHistory = new ArrayList();
    }

    public Map<String, MyField> getComponentMapChilds() {
        return componentMapChilds;
    }

}
