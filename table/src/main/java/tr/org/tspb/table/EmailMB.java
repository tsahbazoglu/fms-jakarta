package tr.org.tspb.table;

import com.mongodb.client.gridfs.GridFSBucket;
import static tr.org.tspb.constants.ProjectConstants.*;
//
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.*;
//
import jakarta.faces.model.SelectItem;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
//
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
//
import org.bson.types.Code;
import org.bson.types.ObjectId;
import org.bson.Document;
import org.slf4j.Logger;
//
import tr.org.tspb.common.qualifier.MyQualifier;
import tr.org.tspb.common.qualifier.ViewerController;
import tr.org.tspb.common.services.BaseService;
import tr.org.tspb.common.services.LdapService;
import tr.org.tspb.common.services.MailService;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.converter.base.BsonConverter;
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.datamodel.pojo.MyLdapUser;
import tr.org.tspb.service.FilterService;
import tr.org.tspb.service.FormService;
import tr.org.tspb.util.qualifier.KeepOpenQualifier;
import tr.org.tspb.util.service.DlgCtrl;
import tr.org.tspb.util.tools.MongoDbUtilIntr;
import tr.org.tspb.util.stereotype.MyController;

/*
 * @author Telman Şahbazoğlu
 */
@MyController
@MyQualifier(myEnum = ViewerController.emailMB)
public class EmailMB implements Serializable {

    @Inject
    protected Logger logger;

    @Inject
    private FormService formService;

    @Inject
    private LdapService ldapService;

    @Inject
    private MailService mailService;

    @Inject
    private DlgCtrl dialogController;

    @Inject
    protected FilterService filterService;

    @Inject
    protected BaseService baseService;

    @Inject
    @KeepOpenQualifier
    private MongoDbUtilIntr mongoDbUtil;

    private Map emailData = new HashMap();
    private static final String SUBJECT = "subject";
    private static final String DEADLINE = "deadline";
    private MyMap crudObject;
    private Map transparentProperties = new HashMap();

    public void init(MyMap crudObject) {
        this.crudObject = crudObject;
    }

    public Map getTransparentProperties() {
        return transparentProperties;
    }

    public void setTransparentProperties(Map transparentProperties) {
        this.transparentProperties = transparentProperties;
    }

    public Map getEmailData() {
        return Collections.unmodifiableMap(emailData);
    }

    public void setEmailData(Map emailData) {
        this.emailData = emailData;
    }

    public List<SelectItem> getEmailTypes() {
        List<SelectItem> list = new ArrayList<>();
        list.add(new SelectItem("PLEASE_SELECT", "Lütfen Seçiniz ..."));

        Document objectEmail = (Document) ((Document) formService.getMyForm().
                getActions()).get(EMAIL);

        Object emailTypes = objectEmail.get("emailTypes");

        if (emailTypes != null) {
            for (Object obj : (Iterable<? extends Object>) emailTypes) {
                Document dbo = (Document) obj;
                list.
                        add(new SelectItem(dbo.get(VALUE), (String) dbo.get(
                                LABEL)));
            }
        }
        return list;
    }

    public String gonder(MyMap crudObject) {
        try {
            String subject = emailData.get(SUBJECT).
                    toString();
            String content = (String) emailData.get("content").
                    toString();
            String email = emailData.get(EMAIL).
                    toString();
            byte[] attachment = (byte[]) emailData.get("attachment");
            Date deadline = (Date) emailData.get(DEADLINE);

            mailService.sendMail(subject, content, email, attachment);
            if (deadline != null) {
                //save deadline on crudObject
            }
        } catch (MessagingException ex) {
            logger.error("error occured", ex);
        }

        return null;
    }

    public String actionEmail() throws IOException, Exception {

        String emailType = (String) transparentProperties.get("emailType");
        String email;
        String content = null;
        String subject;
        byte[] attachment = null;

        Map<String, Object> updateMap = new HashMap(crudObject);

        Object objectEmail = ((Document) formService.getMyForm().
                getActions()).get(EMAIL);

        if (!(objectEmail instanceof Document)) {
            throw new Exception("email field is not instance of DBObject");
        }

        String applicationJavaFunction = ((Document) objectEmail)//
                .getString("applicationJavaFunction");

        if ("updateLdapRecord(updateMap)".equals(applicationJavaFunction)) {
            ldapService.updatePswd(new MyLdapUser(updateMap).getUid());
        }

        Document enrichedCrudObject = new Document();
        for (String key : updateMap.keySet()) {
            if (formService.getMyForm().
                    getField(key) != null) { //"approve" durumu
                if (formService.getMyForm().
                        getField(key).
                        getMyconverter() instanceof BsonConverter) {
                    try {
                        Document dbObject = Document.parse((String) updateMap.
                                get(key));
                        enrichedCrudObject.put(key, dbObject);
                    } catch (Exception e) {
                        enrichedCrudObject.put(key, updateMap.get(key));
                    }
                } else if (updateMap.get(key) instanceof ObjectId) {
                    Object obj = mongoDbUtil.findOne(formService.getMyForm().
                            getDb(), formService.getMyForm().
                                    getField(key).
                                    getItemsAsMyItems().
                                    getTable(),
                            new Document(MONGO_ID, updateMap.get(key)));
                    enrichedCrudObject.put(key, obj);
                } else {
                    enrichedCrudObject.put(key, updateMap.get(key));
                }
            } else {
                enrichedCrudObject.put(key, updateMap.get(key));
            }
        }

        Object msgFormatContent = ((Document) objectEmail).get(
                "msgFormatContent");
        if (msgFormatContent instanceof Code) {
            String code = ((Code) msgFormatContent).getCode();
            //begin
            //otherwise we get undefined on crudObject.workflowStatus.name

            //end
            Document commandResult = mongoDbUtil.runCommand(formService.
                    getMyForm().
                    getDb(), code, enrichedCrudObject);
            msgFormatContent = commandResult.getString(RETVAL);
        }

        if (!"PLEASE_SELECT".equals(emailType)) {
            msgFormatContent = emailType;
        }

        Document Document = mongoDbUtil.findOne(CONFIG_DB,
                COLLECTION_SYSTEM_MESSAGES, new Document(CODE, msgFormatContent));

        if (Document == null) {
            throw new Exception(
                    "Bu duruma uygun gönderilcek email içeriği tespit edilemedi.");
        }

        Object to = ((Document) objectEmail).get("to");
        if (to instanceof Code) {
            String code = ((Code) to).getCode();
            //begin
            //otherwise we get undefined on crudObject.workflowStatus.name

            //end
            Document commandResult = mongoDbUtil.runCommand(formService.
                    getMyForm().
                    getDb(), code, enrichedCrudObject);
            email = commandResult.getString(RETVAL);
        } else {
            throw new Exception("\"to\" alanı tanımlı değil");
        }

        subject = Document.getString(SUBJECT);

        List attrs = new ArrayList();
        List msgFormatAttrs = (List) Document.get("msgFormatAttrs");

        if (msgFormatAttrs != null) {
            for (Object key : msgFormatAttrs) {
                if ("new Date()".equals(key)) {
                    attrs.add(SIMPLE_DATE_FORMAT__0.format(new Date()));
                } else if ("new Date()+1".equals(key)) {
                    Date nextDate = calculateDayIntervalLater(1);
                    attrs.add(String.format("%s tarihli saat %s",//
                            SIMPLE_DATE_FORMAT__0.format(nextDate),
                            SIMPLE_DATE_FORMAT__7.format(nextDate)//
                    ));
                    emailData.put(DEADLINE, nextDate);
                } else {
                    String[] subKeys = ((String) key).split("[.]");
                    Object value = enrichedCrudObject;
                    for (String subKey : subKeys) {
                        value = ((Map) value).get(subKey);
                    }
                    if (value instanceof Number) {
                        NumberFormat nf = NumberFormat.getNumberInstance();
                        nf.setGroupingUsed(false); //otherwise it put comma. we dont want comma here
                        value = nf.format(value);
                    }
                    attrs.add(value);
                }
            }
        }

        if (Document != null && Document.get("contentQuery") != null) {

            Document fileQuery = (Document) Document.get("contentQuery");

            String gridfsDbName = (String) fileQuery.get(FORM_DB);
            String filename = (String) fileQuery.get("filename");

            List<GridFSFile> gridFSFiles = mongoDbUtil.findFiles(gridfsDbName,
                    filename);

            if (gridFSFiles.isEmpty()) {
                throw new Exception("no email file on gridfs with name ".concat(
                        filename));
            }

            GridFSFile fileMetadata = gridFSFiles.get(0);
            StringWriter sw = new StringWriter();

            GridFSBucket gridFSBucket = mongoDbUtil.createGridFSConnection(
                    gridfsDbName);

            try (InputStream inputStream = gridFSBucket.openDownloadStream(
                    fileMetadata.getObjectId());
                    InputStreamReader reader = new InputStreamReader(inputStream,
                            StandardCharsets.UTF_8)) {

                // Efficiently transfer the data using the modern transferTo method (Java 9+)
                // or use a buffer if on older Java versions.
                char[] buffer = new char[8192];
                int length;
                while ((length = reader.read(buffer)) != -1) {
                    sw.write(buffer, 0, length);
                }
            }

            content = MessageFormat.format(sw.toString(), attrs.toArray());
            attachment = mailService.createAttachment(Document, gridfsDbName);
        }

        emailData.put(EMAIL, email);
        emailData.put(SUBJECT, subject);
        emailData.put("content", content);
        emailData.put("attachment", attachment);

        dialogController.showPopup("Email Önizleme", "dummy", "emailPreview");

        return null;
    }

    public Date calculateDayIntervalLater(int dayInterval) {

        List offDays = Arrays.asList("30.8");//dd.MM bayram gunleri

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        while (dayInterval != 0) {
            dayInterval--;
            cal.add(Calendar.DAY_OF_YEAR, 1);
            while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY//
                    || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY//
                    || offDays.contains(//
                            MessageFormat.format("{0}.{1}", cal.get(
                                    Calendar.DAY_OF_MONTH), cal.get(
                                            Calendar.MONTH) + 1))) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 0);

        return cal.getTime();
    }

    public String gonder() {
        gonder(crudObject);
        return null;
    }

    public void localSendEmailToAll() throws NullNotExpectedException,
            IOException {

        Object objectEmail = ((Document) formService.getMyForm().
                getActions()).get(EMAIL_TO_ALL);
        Code recipientListOfMembers2 = (Code) ((Document) objectEmail).get(
                "recipientListOfMembers_2");

        Object msgFormatContent = getTransparentProperties().
                get("emailType");

        Document Document = mongoDbUtil.findOne(CONFIG_DB,
                COLLECTION_SYSTEM_MESSAGES,
                new Document(CODE, msgFormatContent));

        if (Document == null) {
            throw new NullNotExpectedException("content is not defined yet");
        }

        Document fileQuery = (Document) Document.get("contentQuery");

        String gridfsDb = (String) fileQuery.get(FORM_DB);
        String filename = (String) fileQuery.get("filename");

        List<GridFSFile> gridFSFiles = mongoDbUtil.findFiles(gridfsDb,
                filename);

        GridFSFile fileMetadata = gridFSFiles.get(0);

        StringWriter sw = new StringWriter();

        GridFSBucket gridFSBucket = mongoDbUtil.createGridFSConnection(gridfsDb);

        try (InputStream inputStream = gridFSBucket.openDownloadStream(
                fileMetadata.getObjectId());
                InputStreamReader reader = new InputStreamReader(inputStream,
                        StandardCharsets.UTF_8)) {

            // Efficiently transfer the data using the modern transferTo method (Java 9+)
            // or use a buffer if on older Java versions.
            char[] buffer = new char[8192];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                sw.write(buffer, 0, length);
            }
        }

        Document commandResult = mongoDbUtil
                .runCommand(formService.getMyForm().
                        getDb(), recipientListOfMembers2.getCode(),
                        filterService.getTableFilterCurrent());

        List<Document> listOf = mongoDbUtil.find(
                baseService.getLoginDB(),
                baseService.getLoginTable(),
                Filters.in(MONGO_ID, commandResult.get(RETVAL)));

        for (Document loginRecord : listOf) {
            String email = (String) loginRecord.get(EMAIL);
            logger.
                    info(String.
                            format("%50s : %s", loginRecord.get(NAME), email));
        }

    }

}
