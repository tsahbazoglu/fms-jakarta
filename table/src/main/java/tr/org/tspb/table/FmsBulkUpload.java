package tr.org.tspb.table;

import java.io.InputStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import tr.org.tspb.common.qualifier.MyQualifier;
import tr.org.tspb.common.qualifier.ViewerController;
import tr.org.tspb.common.services.LoginController;
import static tr.org.tspb.constants.ProjectConstants.FORMS;
import static tr.org.tspb.constants.ProjectConstants.JAVALANG_DATE;
import static tr.org.tspb.constants.ProjectConstants.JAVALANG_STRING;
import static tr.org.tspb.constants.ProjectConstants.JAVAUTIL_DATE;
import static tr.org.tspb.constants.ProjectConstants.JAVAUTIL_OBJECTID;
import static tr.org.tspb.constants.ProjectConstants.MENU_ORDER;
import static tr.org.tspb.constants.ProjectConstants.NAME;
import static tr.org.tspb.constants.ProjectConstants.RETVAL;
import static tr.org.tspb.constants.ProjectConstants.STATE;
import static tr.org.tspb.constants.ProjectConstants.STYLE;
import static tr.org.tspb.constants.ProjectConstants.TYPE;
import static tr.org.tspb.constants.ProjectConstants.UPLOAD_CONFIG;
import static tr.org.tspb.constants.ProjectConstants.VALUE;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.MyBaseRecord;
import tr.org.tspb.datamodel.dao.MyField;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.datamodel.dao.MyMerge;
import tr.org.tspb.constants.exceptions.FormConfigException;
import tr.org.tspb.constants.exceptions.LdapException;
import tr.org.tspb.constants.exceptions.MongoOrmFailedException;
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.constants.exceptions.UserException;
import tr.org.tspb.factory.cp.OgmCreatorIntr;
import tr.org.tspb.factory.qualifier.OgmCreatorQualifier;
import tr.org.tspb.datamodel.pojo.ExcellColumnDef;
import tr.org.tspb.datamodel.pojo.UserDetail;
import tr.org.tspb.service.RepositoryService;
import tr.org.tspb.util.stereotype.MyController;
import tr.org.tspb.util.tools.DocumentRecursive;

/**
 *
 * @author telman
 * <!-- 1024*1024*1 = 1048576 = 1MB -->
 * <!-- 1024*1024*2 = 2097152 = 2MB -->
 * <!-- 1024*1024*3 = 3145728 = 3MB -->
 * <!-- 1024*1024*4 = 4194304 = 4MB -->
 * <!-- 1024*1024*5 = 5242880 = 5MB -->
 * <!-- 1024*1024*6 = 6291456 = 6MB -->
 * <!-- 1024*1024*7 = 7340032 = 7MB -->
 */
@MyController
@MyQualifier(myEnum = ViewerController.fmsBulkUpload)
public class FmsBulkUpload extends FmsTable implements Serializable {

    @Inject
    @MyQualifier(myEnum = ViewerController.twoDimModifyCtrl)
    private TwoDimModifyCtrl twoDimModifyCtrl;

    private String invalidSizeMessage = "Dosya Boyutu 2MB dan büyük olmamalı : ";
    private String invalidFileMessage = "Geçersiz Dosya Tipi (Sadece *.xlsx dosyalar eklenebilir) : ";
    private String fileLimitMessage = "En fazla 2MB dosya eklenebilir.";
    private int fmsFileLimit = 1;
    private int fmsSizeLimit = 2097152;

    public int getFmsSizeLimit() {
        return fmsSizeLimit;
    }

    public void setFmsSizeLimit(int fmsSizeLimit) {
        this.fmsSizeLimit = fmsSizeLimit;
    }

    public int getFmsFileLimit() {
        return fmsFileLimit;
    }

    public void setFmsFileLimit(int fmsFileLimit) {
        this.fmsFileLimit = fmsFileLimit;
    }

    public String getInvalidSizeMessage() {
        return invalidSizeMessage;
    }

    public void setInvalidSizeMessage(String invalidSizeMessage) {
        this.invalidSizeMessage = invalidSizeMessage;
    }

    public String getFileLimitMessage() {
        return fileLimitMessage;
    }

    public void setFileLimitMessage(String fileLimitMessage) {
        this.fileLimitMessage = fileLimitMessage;
    }

    public String getInvalidFileMessage() {
        return invalidFileMessage;
    }

    public void setInvalidFileMessage(String invalidFileMessage) {
        this.invalidFileMessage = invalidFileMessage;
    }

    @Inject
    protected Logger logger;

    @Inject
    RepositoryService repositoryService;

    @Inject
    @OgmCreatorQualifier
    private OgmCreatorIntr ogmCreator;

    private List<Document> listOfToBeUpsert = new ArrayList<>();
    private List<DocumentRecursive> listOfToBeUpsertAsDocumentRecursive = new ArrayList<>();
    private Map<String, Object> uploadedMergeObject = new HashMap<>();

    public Map<String, Object> getUploadedMergeObject() {
        return uploadedMergeObject;
    }

    public void setUploadedMergeObject(Map<String, Object> uploadedMergeObject) {
        this.uploadedMergeObject = uploadedMergeObject;
    }

    public List<Document> getListOfToBeUpsert() {
        return Collections.unmodifiableList(listOfToBeUpsert);
    }

    public void setListOfToBeUpsert(List<Document> listOfToBeUpsert) {
        this.listOfToBeUpsert = listOfToBeUpsert;
    }

    public String previewUpload() {

        try {
            localPreviewUpload();
            dialogController.showPopup("wv-dlg-upload-preview");
        } catch (NullNotExpectedException ex) {
            dialogController.showPopupError(ex.getMessage());
        }
        return null;
    }

    public String bulkLoadExcell() {
        try {
            localBulkLoadExcell();
            twoDimModifyCtrl.refreshDataTable();
        } catch (Exception ex) {
            logger.error("error occured", ex);
            dialogController.showPopupError(ex.getMessage());
        }
        return null;
    }

    public void localBulkLoadExcell() throws Exception {

        List<String> upsertKeys = null;
        if (!formService.getMyForm().
                getUploadMerge().
                getUpsertFields().
                isEmpty()) {
            upsertKeys = new ArrayList<>();
            for (MyField field : formService.getMyForm().
                    getUploadMerge().
                    getUpsertFields()) {
                upsertKeys.add(field.getKey());
            }
        }

        if (formService.getMyForm().
                getUploadMerge().
                isInsert()) {
            for (Document dbo : listOfToBeUpsert) {
                dbo.putAll(uploadedMergeObject);
                MyMap mm = ogmCreator.getCrudObject();
                mm.putAll(dbo);
                saveObject(formService.getMyForm(), loginController, mm);
                Thread.sleep(200);
            }
        } else if (formService.getMyForm().
                getUploadMerge().
                isUpdate()) {
            for (Document dbo : listOfToBeUpsert) {
                dbo.putAll(uploadedMergeObject);
                Document search = new Document(FORMS, formService.getMyForm().
                        getKey());
                if (upsertKeys != null) {
                    for (String key : upsertKeys) {
                        search.put(key, dbo.get(key));
                    }
                } else {
                    search.putAll(dbo);
                }
                mongoDbUtil.updateMany(formService.getMyForm().
                        getUploadMerge().
                        getToDb(),
                        formService.getMyForm().
                                getUploadMerge().
                                getToCollection(),
                        search, dbo);
            }
        } else if (formService.getMyForm().
                getUploadMerge().
                isUpsert()) {
            for (Document dbo : listOfToBeUpsert) {
                Document search = new Document(FORMS, formService.getMyForm().
                        getKey());
                for (String key : formService.getMyForm().
                        getUploadMerge().
                        getUpsertFilterKeys()) {
                    search.put(key, dbo.get(key));
                }
                mongoDbUtil.upsertOne(
                        formService.getMyForm().
                                getUploadMerge().
                                getToDb(),
                        formService.getMyForm().
                                getUploadMerge().
                                getToCollection(),
                        search, dbo);
            }
        }
    }

    public void localPreviewUpload() throws NullNotExpectedException {

        if (formService.getMyForm().
                getUploadMerge() == null) {
            throw new NullNotExpectedException("form.".concat(UPLOAD_CONFIG).
                    concat(". has not been defined."));
        }

        if (listOfToBeUpsert == null) {
            throw new NullNotExpectedException("Dosya Yükleme İşlemi Yapılmamış");
        }

        if (formService.getMyForm().
                getUploadMerge().
                isInsert()) {
            for (Document dbo : listOfToBeUpsert) {
                dbo.putAll(uploadedMergeObject);
                dbo.put(STYLE, "css-new");
            }
        } else if (formService.getMyForm().
                getUploadMerge().
                isUpdate()) {
            for (Document dbo : listOfToBeUpsert) {

                Document filter = new Document();

                String loginFkFieldKey = formService.getMyForm().
                        getLoginFkField();

                filter.put(loginFkFieldKey, dbo.get(loginFkFieldKey));

                filter.putAll(uploadedMergeObject);

                Document record = mongoDbUtil.findOne(formService.getMyForm().
                        getUploadMerge().
                        getToDb(),
                        formService.getMyForm().
                                getUploadMerge().
                                getToCollection(), filter);

                dbo.putAll(uploadedMergeObject);
                dbo.put(STYLE, record == null ? "css-new" : "css-update");
            }
        } else if (formService.getMyForm().
                getUploadMerge().
                isUpsert()) {
            for (Document dbo : listOfToBeUpsert) {
                dbo.putAll(uploadedMergeObject);
                Document filter = new Document();
                for (String key : formService.getMyForm().
                        getUploadMerge().
                        getUpsertFilterKeys()) {
                    filter.put(key, dbo.get(key));
                }
                Document record = mongoDbUtil.findOne(
                        formService.getMyForm().
                                getUploadMerge().
                                getToDb(),
                        formService.getMyForm().
                                getUploadMerge().
                                getToCollection(),
                        filter);
                dbo.put(STYLE, record == null ? "css-new" : "css-update");
            }
        }

        listOfToBeUpsertAsDocumentRecursive = new ArrayList<>();
        for (Document dbo : listOfToBeUpsert) {
            listOfToBeUpsertAsDocumentRecursive.add(mongoDbUtil.wrapIt(
                    formService.getMyForm(), dbo));
        }

        Collections.sort(listOfToBeUpsertAsDocumentRecursive,
                new Comparator<DocumentRecursive>() {
            @Override
            public int compare(DocumentRecursive t, DocumentRecursive t1) {

                String order1 = ((Document) t.get("member")).get("name").
                        toString().
                        concat(((Document) t.get("period")).get("value").
                                toString());

                String order2 = ((Document) t1.get("member")).get("name").
                        toString().
                        concat(((Document) t1.get("period")).get("value").
                                toString());

                return order1.compareToIgnoreCase(order2);
            }
        });

    }

    public List<DocumentRecursive> getListOfToBeUpsertAsDocumentRecursive() {
        return listOfToBeUpsertAsDocumentRecursive;
    }

    public void uploadExcell(FileUploadEvent event) {

        String memberType = loginController.getLoggedUserDetail().
                getDbo().
                getMemberType();

        uysApplicationMB.initKpbMemberCache();

        listOfToBeUpsert = new ArrayList<>();

        UploadedFile uploadedFileKpbDb = event.getFile();

        try {
            MyMerge myMerge = formService.getMyForm().
                    getUploadMerge();

            if (myMerge == null) {
                dialogController.showPopupError(
                        "upload merge config has not been defined");
                return;
            }

            InputStream is = uploadedFileKpbDb.getInputStream();

            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(is);
            XSSFSheet sheet = xssfWorkbook.getSheetAt(0);

            int rowcount = sheet.getLastRowNum() + 1;
            int startrow = myMerge.getWorkbookSheetStartRow();

            List<String> listOfNotFoundMembers = new ArrayList<>();

            for (int i = startrow; i < rowcount; i++) {
                XSSFRow row = sheet.getRow(i);
                //empty rows is resolved to null
                if (row == null) {
                    continue;
                }

                Document record = new Document().append(FORMS, formService.
                        getMyForm().
                        getKey());

                int columnn = 0;
                XSSFCell cellll;

                for (ExcellColumnDef excellColumnDef : myMerge.
                        getWorkbookSheetColumnList()) {

                    cellll = row.getCell(columnn++);

                    if (cellll == null) {
                        if (excellColumnDef.getToMyField().
                                isRequired()) {
                            throw new Exception(
                                    String.format(
                                            "satır '%d' : sütun '%d' : %s' alanı zorunlu alandır",
                                            i, columnn, excellColumnDef.
                                                    getToMyField().
                                                    getName()));
                        }
                        record.append(excellColumnDef.getToMyField().
                                getKey(), null);
                        continue;
                    }

                    if (cellll.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK) {

                        if (excellColumnDef.getToMyField().
                                isRequired()) {
                            throw new Exception(
                                    String.format(
                                            "satır '%d' : sütun '%d' : '%s' alanı zorunlu alandır",
                                            i, columnn, excellColumnDef.
                                                    getToMyField().
                                                    getName()));
                        }
                        record.append(excellColumnDef.getToMyField().
                                getKey(), "");
                        continue;
                    }

                    Object obtainedValue = null;

                    if (cellll.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        obtainedValue = switch (excellColumnDef.getToMyField().
                                getValueType()) {
                            case JAVAUTIL_DATE ->
                                cellll.getDateCellValue();
                            case JAVALANG_DATE ->
                                cellll.getDateCellValue();
                            case JAVALANG_STRING ->
                                cellll.getStringCellValue();
                            default ->
                                cellll.getStringCellValue();
                        };
                    } else if (cellll.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        obtainedValue = switch (excellColumnDef.getToMyField().
                                getValueType()) {
                            case JAVAUTIL_DATE ->
                                cellll.getDateCellValue();
                            case JAVALANG_DATE ->
                                cellll.getDateCellValue();
                            case JAVALANG_STRING ->
                                String.valueOf(((Number) cellll.
                                getNumericCellValue()).longValue());
                            case JAVAUTIL_OBJECTID ->
                                cellll.getNumericCellValue();
                            default ->
                                cellll.getNumericCellValue();
                        };
                    }

                    Object resolvedValue = obtainedValue;

                    if (excellColumnDef.getConverter() != null && excellColumnDef.
                            getConverter().
                            getCode() != null) {

                        try {
                            Document commandResult = mongoDbUtil.runCommand(
                                    formService.getMyForm().
                                            getUploadMerge().
                                            getToDb(),
                                    excellColumnDef.getConverter().
                                            getCode(), resolvedValue, memberType);

                            resolvedValue = commandResult.get(RETVAL);

                        } catch (Exception ex) {
                            throw new Exception(
                                    String.format(
                                            "satır '%d' : sütun '%d' : değer : '%s' : '%s' alanının tespiti esnasında hata oluştu",
                                            i + startrow, columnn, obtainedValue,
                                            excellColumnDef.getToMyField().
                                                    getName()));
                        }

                        if (resolvedValue instanceof Document) {

                            Document resolvedValueDoc = (Document) resolvedValue;
                            String type = resolvedValueDoc.getString(TYPE);
                            switch (type) {
                                case "objectid":
                                    resolvedValue = new ObjectId(
                                            resolvedValueDoc.getString(VALUE));
                                    break;
                                default:
                                    break;
                            }
                        }
                    }

                    record.append(excellColumnDef.getToMyField().
                            getKey(), resolvedValue);

                }

                if (!loginController.isUserInRole(formService.getMyForm().
                        getMyProject().
                        getAdminAndViewerRole())) {
                    record.put(formService.getMyForm().
                            getLoginFkField(), loginController.
                                    getLoggedUserDetail().
                                    getDbo().
                                    getObjectId());
                }

                listOfToBeUpsert.add(record);
            }

            if (!listOfNotFoundMembers.isEmpty()) {
                showNOKMessage(
                        "Kayıtlı üye listesinde aşağıdaki kurum isimleri tespit edilemedi : ");
            }
            for (String message : listOfNotFoundMembers) {
                showNOKMessage(message);
            }

        } catch (Exception ex) {
            listOfToBeUpsert = new ArrayList<>();
            logger.error("error occured", ex);
            dialogController.showPopupError(
                    "Dosya Yükleme Esnasında hata oluştu.<br/><br/>".concat(ex.
                            getMessage()));
        }

    }

    protected void showOKMessage(String okMessage) {
        addMessage(null, MessageFormat.format("{0}", okMessage),
                okMessage,
                FacesMessage.SEVERITY_INFO);
    }

    protected void showNOKMessage(String nokMessage) {
        addMessage(null, MessageFormat.format("{0}", nokMessage),
                nokMessage,
                FacesMessage.SEVERITY_ERROR);
    }

    protected void addMessage(String componentId, String summary, String message,
            FacesMessage.Severity severity) {
        FacesContext.getCurrentInstance().
                addMessage(componentId, new FacesMessage(severity, summary,
                        message));
    }

    public ObjectId saveObject(FmsForm myForm, LoginController loginMB,
            MyMap crudObject)
            throws UserException, MessagingException, NullNotExpectedException,
            LdapException, FormConfigException, MongoOrmFailedException {

        Object loginFkFieldValue = crudObject.get(formService.getMyForm().
                getLoginFkField());

        if (loginFkFieldValue instanceof MyBaseRecord) {
            loginFkFieldValue = ((MyBaseRecord) loginFkFieldValue).getObjectId();
        }

        boolean ok = loginController.isUserInRole(formService.getMyForm().
                getMyProject().
                getAdminRole());

        ok = ok || loginController.getLoggedUserDetail().
                getDbo().
                getObjectId().
                equals(loginFkFieldValue);

        if (!ok) {
            for (UserDetail.EimzaPersonel ep : loginController.
                    getLoggedUserDetail().
                    getEimzaPersonels()) {
                if (ep.getDelegatingMember() != null && ep.getDelegatingMember().
                        equals(loginFkFieldValue)) {
                    ok = true;
                    break;
                }
            }
        }

        if (!ok) {
            throw new UserException(
                    "Sisteme girş yapan kullanıcı yalnızca kendisine ait veri ekleyip değiştirebilir.");
        }

        Document operatedObject = new Document(crudObject);

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) facesContext.
                getExternalContext().
                getRequest();
        String sessionId = ((HttpSession) facesContext.getExternalContext().
                getSession(false)).getId();

        ObjectId returnID = saveOneDimensionObject(operatedObject, loginMB.
                getLoggedUserDetail().
                getUsername(),
                formService.getMyForm(), request.getRemoteAddr(), sessionId);
        crudObject.put(STATE, "saved");

        return returnID;
    }

    @Override
    public List<Map> findLazyData(int first, int pageSize, Map sortMap) throws
            NullNotExpectedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int findDataCount() throws NullNotExpectedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private transient UploadedFile uploadedFileKpbDb;

    public UploadedFile getUploadedFileKpbDb() {
        return uploadedFileKpbDb;
    }

    public void setUploadedFileKpbDb(UploadedFile uploadedFileKpbDb) {
        this.uploadedFileKpbDb = uploadedFileKpbDb;
    }

    public void uploadSimple() {
        String fileName = uploadedFileKpbDb.getFileName();
        String contentType = uploadedFileKpbDb.getContentType();
        byte[] contents = uploadedFileKpbDb.getContent(); // Or getInputStream()
        // ... Save it, now!
    }

}
