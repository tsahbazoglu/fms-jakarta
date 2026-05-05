package tr.org.tspb.web.servlet;

import java.util.Date;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnectionFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import tr.org.tspb.fgtpswd.fp.FgtPswdTokenManager;
import tr.org.tspb.fgtpswd.fp.PPolicyProvider;
import tr.org.tspb.common.services.BaseService;
import tr.org.tspb.common.services.MailService;
import tr.org.tspb.datamodel.dao.Ppolicy;
import tr.org.tspb.fgtpswd.cdi.qualifier.BasePolicyProvider;
import tr.org.tspb.common.services.LdapService;
import tr.org.tspb.service.RepositoryService;
import tr.org.tspb.constants.ProjectConstants;

/**
 *
 * @author Telman Şahbazoğlu
 */
@SessionScoped
@BasePolicyProvider
public class PPolicyProviderImpl implements PPolicyProvider, Serializable {

    @Inject
    private LdapService ldapService;

    @Inject
    private MailService mailService;

    @Inject
    private BaseService baseService;

    @Inject
    private RepositoryService repositoryService;

    @Produces
    public void init() {
    }

    @Override
    public void sendMail(String subject, String message, String recipients,
            QueueConnectionFactory queueConnectionFactory, Queue queue
    ) throws AddressException, MessagingException {

        String env = (String) FgtPswdTokenManager.instance().getTokenData(
                "env-forget-pswd-email", 1);

        if ("TEST".equals(env)) {
            String emails = (String) FgtPswdTokenManager.instance().
                    getTokenData("test-forget-pswd-email", 1);
            if (emails != null) {
                recipients = emails;
            }
            subject = "TSPB : UYS : *** TEST *** : Parola Hatırlatma";
        }

        Map query = new HashMap();
        query.put(ProjectConstants.EMAIL, recipients);

        long count = repositoryService.count(baseService.getLoginDB(),
                baseService.getLoginTable(), query);

        if (count == 1) {
            mailService.sendMail(subject, message, recipients);
        } else {
            System.out.println("no email or more then one");
            System.out.println(subject);
        }

    }

    @Override
    public void updatePpolicy(String shortTokenEmail, String lastLoginIP) {

        Map query = new HashMap();
        query.put(ProjectConstants.EMAIL, shortTokenEmail);

        if (repositoryService.count(baseService.getLoginDB(), baseService.
                getLoginTable(), query) == 1) {

            Map document = repositoryService.one(baseService.getLoginDB(),
                    baseService.getLoginTable(), query);

            String ldapUID = (String) document.get(baseService.
                    getLoginUsernameField());

            if (ldapUID != null) {
                Date today = new Date();

                Ppolicy ppolicy = new Ppolicy.Builder()
                        .withDefault()
                        .withUid(ldapUID)
                        .withLastLoginTime(today)
                        .withChangePswdTime(today)
                        .withLastLoginIP(lastLoginIP)
                        .withTryCount(0)
                        .build();

                repositoryService.updateMany(ProjectConstants.CONFIG_DB,
                        ProjectConstants.PPOLICY, ppolicy.createQuery(),
                        ppolicy.createUpdateSet());
            }
        }
    }

    @Override
    public void updatePswd(String shortTokenEmail, String pswd) throws Exception {

        Map query = new HashMap();
        query.put(ProjectConstants.EMAIL, shortTokenEmail);

        long count = repositoryService.count(baseService.getLoginDB(),
                baseService.getLoginTable(), query);

        switch ((int) count) {
            case 0:
                throw new RuntimeException("Kullanıcı Adı veya E-posta yalnış.");
            case 1:
                Map memberDoc = repositoryService.one(baseService.getLoginDB(),
                        baseService.getLoginTable(), query);
                ldapService.updatePswd((String) memberDoc.get(baseService.
                        getLoginUsernameField()), pswd);
                break;
            default:
                throw new RuntimeException(
                        "Bu Kullanıcı e-postası ile birden falza kullanıcı tanımlı.");
        }

    }

    @Override
    public int emailCount(String email) {

        Map query = new HashMap();
        query.put(ProjectConstants.EMAIL, email);

        return (int) repositoryService.count(baseService.getLoginDB(),
                baseService.getLoginTable(), query);

    }
}
