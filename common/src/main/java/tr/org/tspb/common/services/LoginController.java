package tr.org.tspb.common.services;

import com.mongodb.client.model.Filters;

import static tr.org.tspb.constants.ProjectConstants.*;

import htmlflow.HtmlFlow;
import htmlflow.HtmlView;
import javax.security.auth.Subject;
import tr.org.tspb.constants.exceptions.LdapException;
import tr.org.tspb.util.service.DlgCtrl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.mail.MessagingException;
import javax.naming.NamingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.LoggerFactory;
import org.bson.types.ObjectId;
import tr.org.tspb.util.stereotype.MyController;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import org.bson.Document;
import org.slf4j.Logger;
import tr.org.tspb.common.qualifier.MyLoginQualifier;
import tr.org.tspb.datamodel.dao.MyProject;
import tr.org.tspb.datamodel.pojo.DatabaseUser;
import tr.org.tspb.datamodel.pojo.RoleMap;
import tr.org.tspb.datamodel.pojo.UserDetail;
import tr.org.tspb.datamodel.pojo.html.HtmlCompany;
import tr.org.tspb.datamodel.pojo.html.HtmlRole;
import tr.org.tspb.datamodel.pojo.html.HtmlUsrDetail;
import tr.org.tspb.util.qualifier.KeepOpenQualifier;
import tr.org.tspb.util.tools.MongoDbUtilIntr;

/**
 *
 * @author Telman Şahbazoğlu
 */
@MyController
@MyLoginQualifier
public class LoginController implements Serializable {

    @Inject
    MailService mailService;

    @Inject
    LdapService ldapService;

    @Inject
    BaseService baseService;

    @Inject
    DlgCtrl dialogController;

    @Inject
    private Logger logger;

    @Inject
    @KeepOpenQualifier
    private MongoDbUtilIntr mongoDbUtil;

    @Inject
    private MyHtmlTemplates myHtmlTemplates;

    //    @Inject
//    //@DefaultEsignDoor
//    @OyasEsignDoorQualifier
//    private EsignDoor esignDoor;
//
//    @Inject
//    @MyQualifier(myEnum = ViewerController.twoDimModifyCtrl)
//    TwoDimModifyCtrl twoDimModifyCtrl;
    private static final int DIALOG_STEP = 2;
    private String username;
    private String userpassword;
    private UserDetail loggedUserDetail;
    private int dialogStep = 0;
    private String forgetUsername;
    private String oldPassword;
    private String newPassword;
    private String newPasswordAgain;
    private String newLdapUserUID;
    private String newLdapUserName;
    private String newLdapUserPassword;
    private List<String> newLdapUserRoles;
    private List<String> roleAsList;
    private String newLdapUserRole;
    private final transient Map dialogMap = new HashMap();
    private RoleMap roleMap;
    private String jaasLoginName;

    @PostConstruct
    public void init() {

        HttpServletRequest request = (HttpServletRequest) FacesContext.
                getCurrentInstance().
                getExternalContext().
                getRequest();

        if (jaasLoginName == null || jaasLoginName.trim().isEmpty()) {
            if (request.getSession(false) != null && request.getSession(false).getAttribute("jaasLoginName") != null) {
                jaasLoginName = (String) request.getSession(false).getAttribute("jaasLoginName");
            } else if (request.getRemoteUser() != null && !request.getRemoteUser().trim().isEmpty()) {
                jaasLoginName = request.getRemoteUser();
            } else if (request.getUserPrincipal() != null) {
                jaasLoginName = request.getUserPrincipal().getName();
            }
        }

        if (jaasLoginName != null && !jaasLoginName.trim().isEmpty() && request.getSession(false) != null) {
            request.getSession(false).setAttribute("jaasLoginName", jaasLoginName);
        }

        initUser(jaasLoginName, request);
    }


    private UserDetail userDetail(String userID) {

        UserDetail userDetail = new UserDetail();

        userDetail.setUsername(userID);
        userDetail.setFirstName(userID);
        userDetail.setLastName(userID);
        userDetail.setCommonName(userID);

        return userDetail;
    }

    private void initUser(String jaasLoginUsername, HttpServletRequest request) {
        if (jaasLoginUsername == null || jaasLoginUsername.trim().
                isEmpty()) {
            logBaseInfo(request);
            return;
        }
        loggedUserDetail = userDetail(jaasLoginUsername);
        if (loggedUserDetail == null || loggedUserDetail.getUsername() == null || loggedUserDetail.
                getUsername().
                trim().
                isEmpty()) {
            logBaseInfo(request);
            invalidateSession();
            return;
        }
        Object session = FacesContext.getCurrentInstance().
                getExternalContext().
                getSession(false);

        HttpSession httpSession = (HttpSession) session;
        if (httpSession != null && httpSession.getAttribute(LOGGED_USER_ROLES) != null) {
            roleAsList = (List<String>) httpSession.getAttribute(LOGGED_USER_ROLES);
        }

        if (roleAsList == null || roleAsList.isEmpty()) {
            logBaseInfo(request);
            invalidateSession();
            return;
        }

        roleMap = new RoleMap();
        roleMap.setLoggedUserRoles(roleAsList);

        String loginDB = baseService.getLoginDB();
        String loginTable = baseService.getLoginTable();
        String loginUsernameField = baseService.getLoginUsernameField();
        Document query = new Document(loginUsernameField, loggedUserDetail.
                getUsername());

        Document memberDbo = mongoDbUtil.findUserOne(loggedUserDetail.
                getUsername());

        if (memberDbo != null) {
            memberDbo.append("lastLoginTime", new Date());
            memberDbo.append("lastLoginIP", request.getRemoteAddr());
            mongoDbUtil.updateOne(loginDB, loginTable, query, memberDbo);

            DatabaseUser databaseUser = new DatabaseUser(memberDbo,
                    loginUsernameField);

            loggedUserDetail.initDatabaseUser(databaseUser);
            resolveDelegations(databaseUser);
        }
    }


    public static Subject getSubject(HttpServletRequest req) {
        if (req == null) return null;
        Object subj = req.getAttribute("javax.security.auth.subject");
        if (subj instanceof Subject) return (Subject) subj;
        subj = req.getAttribute("jakarta.security.auth.subject");
        if (subj instanceof Subject) return (Subject) subj;
        HttpSession session = req.getSession(false);
        if (session != null) {
            subj = session.getAttribute("javax.security.auth.subject");
            if (subj instanceof Subject) return (Subject) subj;
            subj = session.getAttribute("jakarta.security.auth.subject");
            if (subj instanceof Subject) return (Subject) subj;
        }
        return null;
    }


    private void logBaseInfo(HttpServletRequest request) {
        StringBuilder serverLog = new StringBuilder(
                "uys app log : jaas login is success but jaasLoginUsername is null");

        serverLog.append(COMMA);
        serverLog.append(
                request.getRemoteHost() == null ? "REMOTE HOST" : request.
                        getRemoteHost());

        serverLog.append(COMMA);
        serverLog.append(
                request.getRemoteAddr() == null ? "REMOTE ADDR" : request.
                        getRemoteAddr());

        String logMsg = serverLog.toString();

        logger.error(logMsg);

        try {
            mailService.sendMail("UYS URGENT ISSUE",
                    "please have a look to server.log\n ".concat(serverLog.
                            toString()), "tsahbazoglu@tspb.org.tr");
        } catch (MessagingException ex) {
            logger.error("error occured", ex);
        }
    }

    private void invalidateSession() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().
                getExternalContext().
                getSession(false);
        session.invalidate();
    }

    /**
     * Creates a new instance of LoginMB
     */
    public LoginController() {
        dialogMap.put(0, "loginDiaolg");
        dialogMap.put(2, "passwordForgetDiaolg");
        dialogMap.put(4, "loginDiaolg");
    }

    public String getWelcomeContent() {
        StringBuilder sb = new StringBuilder();

        String company = "DB";// TDUB
        String html = "";

        switch (company) {
            case "DB":
                html = baseService.getWelcomePage();
                break;
            case "TSPB":
                html = HtmlFlow.doc(System.out) //.view()
                        .
                        html().
                        head().
                        __() //head
                                .
                        body().
                        div().
                        attrClass("container").
                        p().
                        text("Sayın üyemiz,").
                        __().
                        br().
                        __().
                        br().
                        __().
                        p().
                        text("TSPB Üye Yönetim Sistemine hoş geldiniz.").
                        __().
                        br().
                        __().
                        br().
                        __().
                        p().
                        text(
                                "TSPB Üye Yönetim Sisteminin, Google Chrome ve  Mozilla Firefox tarayıcılarıyla kullanılması tavsiye edilmektedir.").
                        __().
                        br().
                        __().
                        br().
                        __().
                        p().
                        text(
                                "Giriş yapmak istediğiniz verileri, sağ mönüde yer alan ilgili başlıklara tıklayarak doldurabilirsiniz.").
                        __().
                        br().
                        __().
                        ul().
                        li().
                        text(
                                "<b>Bilgi Bankası</b> başlığında, Birliğimize üçer aylık dönemler itibariyle iletilen faaliyet ve finansal veriler yer almaktadır.").
                        __().
                        li().
                        text(
                                "<b>Komisyon ve Promosyon Bildirimi</b> mönüsünde, Birliğimize aylık olarak bildirilen aracılık komisyonu ve promosyon uygulamalarına ilişkin form bulunmaktadır.").
                        __().
                        li().
                        text(
                                "SPK’nın ilgili düzenlemeleri gereği yapılması gereken bildirimlere ilişkin formlar “<b>Bildirim Sistemi</b>” mönüsünde yer almaktadır.").
                        __().
                        li().
                        text(
                                "<b>Yabancı Piyasa İşlemleri</b> mönüsünde, Birliğimize bildirilen yurtdışı piyasalardaki işlemlere ilişkin bilgiler yer almaktadır.").
                        __().
                        __()//ul
                                .
                        br().
                        __().
                        __().
                        __() //body
                                .
                        __() //html
                                .
                        toString();
                break;
            case "TSPB2":
                html = HtmlFlow
                        .doc(System.out) //.view()
                                .
                        html().
                        head().
                        __() //head
                                .
                        body().
                        div().
                        attrClass("container").
                        p().
                        text("Sayın Üyemiz,").
                        __().
                        br().
                        __().
                        br().
                        __().
                        p().
                        text(
                                "T.C. Hazine ve Maliye Bakanlığı’nın Vergi Harcamaları raporunda kullanılmak üzere, yatırımcılarca, banka ve aracı kurumlar vasıtasıyla gerçekleştirilen hisse senedi alım ve satımlarından elde edilen kazançlara ilişkin yatırımcı bazında bilgilere ihtiyaç duyulmaktadır. Bu kapsamda, 2019 yılı verileri ile yatırımcı grubu bazında pay senedi ve menkul kıymet yatırım ortaklığı pay senedi alım ve satımlarından elde edilen kazanç verilerinin  “25 Mayıs 2020 Pazartesi gününe kadar Birliğimize iletilmesi gerekmektedir.").
                        __().
                        br().
                        __().
                        br().
                        __().
                        p().
                        text(
                                "Form doldurulurken dikkat edilmesi gereken hususlar aşağıdaki gibidir:").
                        __().
                        br().
                        __().
                        ul().
                        li().
                        text(
                                "Yıl içinde pay senedi ve menkul kıymet yatırım ortaklığı payı senedi alım satımından her bir yatırımcının net kazancı hesaplanır.").
                        __().
                        li().
                        text(
                                "Yatırımcı bazında hesaplanan bilgiler ilgili yatırımcı gruplarında konsolide edilerek form doldurulur.").
                        __().
                        li().
                        text(
                                "Yıl içinde net anlamda zarar eden yatırımcıların verileri dahil edilmez.").
                        __().
                        li().
                        text(
                                "Gayrimenkul Yatırım Ortaklıkları ile Girişim Sermayesi Yatırım Ortaklıklarının alım ve satımlarından elde edilen kazançlar “Pay” kırılımının altında takip edilir.").
                        __().
                        __()//ul
                                .
                        br().
                        __().
                        p().
                        text(
                                "Söz konusu veriler ile ilgili herhangi bir konuda tereddüt olması durumunda T.C. Hazine ve Maliye Bakanlığı Gelir İdaresi Başkanlığı Uzmanı Galip Haksever (galip.haksever@gelirler.gov.tr veya 0 312 415 30 89 ) ile, anket formunun gönderimiyle ilgili olarak tereddüt olması durumunda ise Araştırma ve İstatistik Bölümü ile arastirma@tspb.org.tr e-posta adresi üzerinden irtibat kurulabilir.").
                        __().
                        br().
                        __().
                        __().
                        __() //body
                                .
                        __() //html
                                .
                        toString();
                break;
            case "TDUB":
                sb.append("Sayın üyemiz,  <br/><br/>");
                sb.
                        append("TDUB Üye Yönetim Sistemine hoş geldiniz.  <br/><br/>");
                sb.append(
                        "TDUB Üye Yönetim Sisteminin, Google Chrome ve  Mozilla Firefox tarayıcılarıyla kullanılması tavsiye edilmektedir.<br/><br/>");
                sb.append(
                        "Giriş yapmak istediğiniz verileri, sağ mönüde yer alan ilgili başlıklara tıklayarak doldurabilirsiniz.<br/>");
                sb.append("<br/>");
                break;
            default:
                break;
        }
        return html;
    }

    public String getNewLdapUserUID() {
        return newLdapUserUID;
    }

    public void setNewLdapUserUID(String newLdapUserUID) {
        this.newLdapUserUID = newLdapUserUID;
    }

    public String getNewLdapUserName() {
        return newLdapUserName;
    }

    public void setNewLdapUserName(String newLdapUserName) {
        this.newLdapUserName = newLdapUserName;
    }

    public String getNewLdapUserPassword() {
        return newLdapUserPassword;
    }

    public void setNewLdapUserPassword(String newLdapUserPassword) {
        this.newLdapUserPassword = newLdapUserPassword;
    }

    public List<String> getNewLdapUserRoles() {
        return Collections.unmodifiableList(newLdapUserRoles);
    }

    public String getNewLdapUserRole() {
        return newLdapUserRole;
    }

    public void setNewLdapUserRole(String newLdapUserRole) {
        this.newLdapUserRole = newLdapUserRole;
    }

    public void setNewLdapUserRole(List<String> newLdapUserRoles) {
        this.newLdapUserRoles = newLdapUserRoles;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordAgain() {
        return newPasswordAgain;
    }

    public void setNewPasswordAgain(String newPasswordAgain) {
        this.newPasswordAgain = newPasswordAgain;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getForgetUsername() {
        return forgetUsername;
    }

    public void setForgetUsername(String forgetUsername) {
        this.forgetUsername = forgetUsername;
    }

    public UserDetail getLoggedUserDetail() {
        return loggedUserDetail;
    }

    public void resolveDelegations(DatabaseUser member) {
        BaseService.AppProperties serverProperties = baseService.getProperties();

        String delegateDB = serverProperties.getDelegateDbName();
        String delegateTable = serverProperties.getDelegateTableName();
        String delegatedField = serverProperties.getDelegatedMemberFieldName();
        String delegatingField = serverProperties.getDelegatingMemberFieldName();
        Map delegateInitialSearch = serverProperties.getDelegateInıtıailSearch();

        if (delegatedField == null || delegatingField == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Konfigürasyon Eksikliği : ");
            sb.append("<br/><br/>");
            sb.append("delegation config is missed");
            dialogController.showMsgDlgWarnWithB(sb.toString());
            return;
        }

        List<Document> eimzaInfo = mongoDbUtil.find(delegateDB, delegateTable,
                new Document(delegateInitialSearch)
                        .append(delegatedField, member.getObjectId()));

        List<UserDetail.EimzaPersonel> listofEligibale = new ArrayList<>();

        for (Document delegationRecord : eimzaInfo) {
            Document delegatedFieldRecord = mongoDbUtil.findOne(baseService.
                            getLoginDB(),
                    baseService.getLoginTable(),
                    Filters.eq(MONGO_ID, delegationRecord.get(delegatedField,
                            ObjectId.class)));

            UserDetail.EimzaPersonel eimzaPersonel
                    = new UserDetail().new EimzaPersonel(delegationRecord,
                    delegatedFieldRecord, delegatedField,
                    delegatingField);

            listofEligibale.add(eimzaPersonel);
        }

        loggedUserDetail.createEimzaPersonels(listofEligibale);
        loggedUserDetail.createLoginFkSearchMapInListOfValues(member.
                getObjectId());
    }

    public String showRoles() {

        List<HtmlRole> listOfRole = new ArrayList<>();
        for (String role : roleAsList) {
            listOfRole.add(myHtmlTemplates.getRole(role));
        }

        List<HtmlCompany> listCompany = new ArrayList<>();
        for (UserDetail.EimzaPersonel ep : loggedUserDetail.getEimzaPersonels()) {
            HtmlCompany htmlCompany = myHtmlTemplates.getCompany(ep.
                    getDelegatingMember());
            if (htmlCompany != null) {
                listCompany.add(htmlCompany);
            }
        }

        String userDetailHtml = myHtmlTemplates.buildUserDetailHtml(loggedUserDetail, listOfRole, listCompany);

        dialogController.showDlgUserInfo(userDetailHtml);

        return null;

    }

    public String login() throws IOException {
        FacesContext.getCurrentInstance().
                getExternalContext().
                redirect(
                        "j_security_check?j_username=" + username + "&j_password=" + userpassword);
        return null;
    }

    public int getLoginDialog() {
        return dialogStep;
    }

    public String forgetPassword() {
        dialogStep = 2;
        return null;
    }

    public String sendPassword() {
        try {
            localSendPassword();
        } catch (Exception ex) {
            logger.error("error occured", ex);
            dialogController.showPopupError("şifre güncelleme işlemi başarısız");
        }
        return null;
    }

    public void localSendPassword() throws NamingException, Exception {
        if (forgetUsername == null) {
            return;
        }

        ldapService.updatePswdAndNotifyUser(forgetUsername);
        dialogStep = 4;
        dialogController.showPopup((String) dialogMap.get(dialogStep));

    }

    public String diaologBack() {
        dialogStep -= DIALOG_STEP;
        dialogController.showPopup((String) dialogMap.get(dialogStep));
        return null;
    }

    public String goToLoginPage() {
        dialogStep = 0;
        return null;
    }

    public static final String DEFAULT_AUTH_LOGOUT_URL = "http://localhost:8088/api/auth/logout";
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(LoginController.class);

    private static final HttpClient LOGOUT_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public static String resolveAuthLogoutUrl() {
        HttpServletRequest req = null;
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc != null && fc.getExternalContext() != null) {
                Object r = fc.getExternalContext().getRequest();
                if (r instanceof HttpServletRequest) {
                    req = (HttpServletRequest) r;
                }
            }
        } catch (Exception ignored) {}
        return resolveAuthLogoutUrl(req);
    }

    public static String resolveAuthLogoutUrl(HttpServletRequest req) {
        String url = System.getProperty("cas.auth.logout.url");
        if (url == null || url.isBlank()) {
            url = System.getenv("CAS_AUTH_LOGOUT_URL");
        }
        if (url == null || url.isBlank()) {
            url = System.getProperty("cas.logout.url");
        }
        if (url == null || url.isBlank()) {
            url = System.getenv("CAS_LOGOUT_URL");
        }
        if (url != null && !url.isBlank()) {
            return url.trim();
        }
        String port = System.getProperty("cas.port");
        if (port == null || port.isBlank()) {
            port = System.getenv("CAS_PORT");
        }
        if (port == null || port.isBlank()) {
            port = "8088";
        }
        port = port.trim();
        if (req != null) {
            String fwdHost = req.getHeader("X-Forwarded-Host");
            String host = (fwdHost != null && !fwdHost.isBlank()) ? fwdHost.split(",")[0].trim() : req.getHeader("Host");
            if (host == null || host.isBlank()) {
                host = req.getServerName();
            }
            if (host != null && !host.isBlank()) {
                String hostOnly = host;
                int idx = hostOnly.indexOf(':');
                if (idx != -1) {
                    hostOnly = hostOnly.substring(0, idx);
                }
                hostOnly = hostOnly.trim().toLowerCase();
                boolean isLocal = "localhost".equals(hostOnly) || "127.0.0.1".equals(hostOnly) || "0.0.0.0".equals(hostOnly) || "::1".equals(hostOnly);
                if (!isLocal) {
                    String proto = req.getHeader("X-Forwarded-Proto");
                    String scheme = (proto != null && !proto.isBlank()) ? proto.split(",")[0].trim().toLowerCase() : (req.getScheme() != null ? req.getScheme().toLowerCase() : "http");
                    return scheme + "://" + hostOnly + ":" + port + "/api/auth/logout";
                }
            }
        }
        if (!"8088".equals(port)) {
            return "http://localhost:" + port + "/api/auth/logout";
        }
        return DEFAULT_AUTH_LOGOUT_URL;
    }

    public static boolean callAuthLogout(String token) {
        return callAuthLogout((HttpServletRequest) null, token);
    }

    public static boolean callAuthLogout(HttpServletRequest req, String token) {
        String logoutUrl = resolveAuthLogoutUrl(req);
        return callAuthLogout(logoutUrl, token);
    }

    public static boolean callAuthLogout(String logoutUrl, String token) {
        if (logoutUrl == null || logoutUrl.isBlank()) {
            logoutUrl = resolveAuthLogoutUrl();
        }
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(logoutUrl.trim()))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody());

            if (token != null && !token.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + token);
                reqBuilder.header("Cookie", "token=" + token + "; cas_token=" + token);
            }

            HttpResponse<String> response = LOGOUT_HTTP_CLIENT.send(
                    reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            STATIC_LOGGER.info("Auth logout POST called at {}, response status: {}", logoutUrl, response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            STATIC_LOGGER.warn("Failed to call auth logout POST {}: {}", logoutUrl, ex.getMessage());
            return false;
        }
    }

    public void logout() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpSession session = null;
        HttpServletRequest request = null;
        HttpServletResponse response = null;
        String token = null;

        if (facesContext != null && facesContext.getExternalContext() != null) {
            session = (HttpSession) facesContext.getExternalContext().getSession(false);
            Object reqObj = facesContext.getExternalContext().getRequest();
            if (reqObj instanceof HttpServletRequest) {
                request = (HttpServletRequest) reqObj;
            }
            Object respObj = facesContext.getExternalContext().getResponse();
            if (respObj instanceof HttpServletResponse) {
                response = (HttpServletResponse) respObj;
            }
        }

        if (session != null) {
            Object tokenObj = session.getAttribute("token");
            if (tokenObj instanceof String) {
                token = (String) tokenObj;
            }
        }
        if (token == null && request != null && request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("token".equalsIgnoreCase(c.getName())
                        || "cas_token".equalsIgnoreCase(c.getName())
                        || "auth_token".equalsIgnoreCase(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        callAuthLogout(request, token);

        if (response != null) {
            for (String cookieName : new String[]{"token", "cas_token", "auth_token"}) {
                Cookie cookie = new Cookie(cookieName, "");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
        }

        if (session != null) {
            session.removeAttribute("token");
            session.removeAttribute("jaasLoginName");
            session.removeAttribute("companyId");
            session.removeAttribute("gateVerifyResult");
            session.removeAttribute(LOGGED_USER);
            session.removeAttribute(LOGGED_USER_ROLES);
            session.removeAttribute("javax.security.auth.subject");
            session.removeAttribute("jakarta.security.auth.subject");
            session.removeAttribute("subject");
            session.removeAttribute("jaasSubject");
            session.removeAttribute("userPrincipal");
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
            }
        }

        if (facesContext != null && facesContext.getExternalContext() != null && request != null) {
            String redirectTarget = request.getContextPath();
            if (redirectTarget == null || redirectTarget.isBlank()) {
                redirectTarget = "/";
            }
            facesContext.getExternalContext().redirect(redirectTarget);
        }
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the userpassword
     */
    public String getUserpassword() {
        return userpassword;
    }

    /**
     * @param userpassword the userpassword to set
     */
    public void setUserpassword(String userpassword) {
        this.userpassword = userpassword;
    }

    public String getJaasLoginName() {
        return jaasLoginName;
    }

    public void setJaasLoginName(String jaasLoginName) {
        this.jaasLoginName = jaasLoginName;
        HttpServletRequest request = (HttpServletRequest) FacesContext.
                getCurrentInstance().
                getExternalContext().
                getRequest();
        if (request != null && request.getSession(false) != null && jaasLoginName != null) {
            request.getSession(false).setAttribute("jaasLoginName", jaasLoginName);
        }
        if (jaasLoginName != null && !jaasLoginName.trim().isEmpty()
                && (loggedUserDetail == null || !jaasLoginName.equals(loggedUserDetail.getUsername()))) {
            initUser(jaasLoginName, request);
        }
    }

    public RoleMap getRoleMap() {
        return roleMap;
    }

    public void setRoleMap(RoleMap roleMap) {
        this.roleMap = roleMap;
    }

    public boolean isUserInRole(Object roles) {
        return roleMap.isUserInRole(roles);
    }

    public Set getRolesAsSet() {
        return roleMap.keySet();
    }

    public List getRolesAsList() {
        return roleAsList;
    }

    public boolean isEmpty() {
        return roleMap.isEmpty();
    }

    void setLoggedUserRoles(List list) {
        roleMap.setLoggedUserRoles(list);
    }

    public boolean notMemberNotAdminNotViewer(MyProject myProject) {
        return loggedUserDetail.getDbo().
                getObjectId() == null && !isUserInRole(
                myProject.getAdminAndViewerRole());
    }

}
