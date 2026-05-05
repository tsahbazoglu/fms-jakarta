package tr.org.tspb.common.services;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import static jakarta.xml.bind.Marshaller.JAXB_ENCODING;
import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.MessageFormat;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnectionFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.bson.Document;
import org.slf4j.Logger;
import tr.org.tspb.constants.ProjectConstants;
import tr.org.tspb.util.qualifier.KeepOpenQualifier;
import tr.org.tspb.util.service.JmsMessageDispatcher;
import tr.org.tspb.util.tools.MongoDbUtilIntr;
import tr.org.tspb.mail.EmailData;
import tr.org.tspb.util.stereotype.MyServices;

/**
 *
 * @author Telman Şahbazoğlu
 */
@MyServices
public class MailService implements Serializable {

    @Inject
    JmsMessageDispatcher jmsMessageDispatcher;

    @Inject
    BaseService baseService;

    @Inject
    private Logger logger;

    @Inject
    @KeepOpenQualifier
    private MongoDbUtilIntr mongoDbUtil;

    @Resource(lookup = "connFactory",
            mappedName = ProjectConstants.JMS_EMAIL_QUEUE_CONN_FACTORY)
    private QueueConnectionFactory queueConnectionFactory;

    @Resource(lookup = "jmsQueue", mappedName = ProjectConstants.JMS_EMAIL_QUEUE)
    private Queue queue;

    public void sendMailBcc(String subject, String message, String recipients,
            String bccRecipients) throws AddressException, MessagingException {
        if (getSendEmailDisabled()) {
            logger.warn("Eposta gönderim etkisiz.");
            return;
        }

        EmailData emailData = createEmailData(subject, message, recipients, null);
        emailData.setBccRecipients(bccRecipients);

        jmsMessageDispatcher.sendRequestData(queueConnectionFactory, queue,
                emailData);
    }

    public void sendMail(String subject, String message, String recipients)
            throws AddressException, MessagingException {
        sendMail(subject, message, recipients, null);
    }

    public void sendMail(String subject, String message, String recipients,
            byte[] inputStream) throws AddressException, MessagingException {

        if (getSendEmailDisabled()) {
            logger.warn("Eposta gönderim etkisiz.");
            return;
        }

        EmailData emailData = createEmailData(subject, message, recipients,
                inputStream);

        jmsMessageDispatcher.sendRequestData(queueConnectionFactory, queue,
                emailData);

    }

    private EmailData createEmailData(String subject, String message,
            String recipients, byte[] inputStream) {
        EmailData emailData = new EmailData(subject, message, recipients, null);
        //Log EmailData
        try {
            JAXBContext contextEmailData = JAXBContext.newInstance(
                    EmailData.class);
            Marshaller marshallEmailData = contextEmailData.createMarshaller();
            marshallEmailData.setProperty(JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshallEmailData.setProperty(JAXB_ENCODING, "UTF-8");
            StringWriter sw = new StringWriter();
            marshallEmailData.marshal(emailData, sw);
            logger.info(MessageFormat.format(
                    "\nTrying to put an email to JMS Queue, \n{0}", sw.
                            toString()));
        } catch (JAXBException ex) {
            logger.error("error occured", ex);
        }

        if (inputStream != null) {
            emailData.setAttachment(inputStream);
        }

        return emailData;
    }

    private boolean getSendEmailDisabled() {
        boolean sendEmailDisabled = true;
        try {
            sendEmailDisabled = "TRUE".equals(baseService.getProperties().
                    getSendEmailDisabled());
        } catch (NullPointerException ne) {
            logger.error(
                    "Eposta gönderim etkinleştirme ayarı eksik. Gönderim etkisizleştirildi (varsayılan).",
                    ne);
        }
        return sendEmailDisabled;
    }

    public byte[] createAttachment(Document doc, String gridfsdb) throws
            IOException {

        if (doc.get("attachmentQuery") == null) {
            return null;
        }

        Document dbObject = (Document) doc.get("attachmentQuery");

        GridFSFile gridFSFile = mongoDbUtil.findFiles(gridfsdb, dbObject.
                get("filename").
                toString()).
                get(0);

        GridFSBucket gridFSBucket = mongoDbUtil.createGridFSConnection(gridfsdb);

        InputStream attachmentStream = gridFSBucket.openDownloadStream(
                gridFSFile.getObjectId());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = attachmentStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();

    }

}
