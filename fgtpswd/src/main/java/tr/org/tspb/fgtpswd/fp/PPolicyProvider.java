package tr.org.tspb.fgtpswd.fp;

import jakarta.jms.Queue;
import jakarta.jms.QueueConnectionFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;

/**
 *
 * @author Telman Şahbazoğlu
 */
public interface PPolicyProvider {

    public int emailCount(String shortTokenEmail);

    public void updatePswd(String shortTokenEmail, String pswd) throws Exception;

    public void updatePpolicy(String shortTokenEmail, String lastLoginIP);

    public void sendMail(String subject, String message, String recipients,
            QueueConnectionFactory queueConnectionFactory, Queue queue) throws
            AddressException, MessagingException;

}
