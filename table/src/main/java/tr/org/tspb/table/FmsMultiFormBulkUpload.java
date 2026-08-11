package tr.org.tspb.table;

import com.mongodb.MongoWriteException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
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
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import tr.org.tspb.common.qualifier.MyCtrlServiceQualifier;
import tr.org.tspb.common.qualifier.MyLoginQualifier;
import tr.org.tspb.common.qualifier.MyQualifier;
import tr.org.tspb.common.qualifier.ViewerController;
import tr.org.tspb.common.services.AppScopeSrvCtrl;
import tr.org.tspb.common.services.BaseService;
import tr.org.tspb.common.services.LoginController;
import tr.org.tspb.common.services.MailService;

import static tr.org.tspb.constants.ProjectConstants.ADMIN_METADATA;
import static tr.org.tspb.constants.ProjectConstants.CREATE_DATE;
import static tr.org.tspb.constants.ProjectConstants.CREATE_SESSIONID;
import static tr.org.tspb.constants.ProjectConstants.CREATE_USER;
import static tr.org.tspb.constants.ProjectConstants.FORMS;
import static tr.org.tspb.constants.ProjectConstants.INODE;
import static tr.org.tspb.constants.ProjectConstants.JAVALANG_DATE;
import static tr.org.tspb.constants.ProjectConstants.JAVALANG_INTEGER;
import static tr.org.tspb.constants.ProjectConstants.JAVALANG_STRING;
import static tr.org.tspb.constants.ProjectConstants.JAVAUTIL_DATE;
import static tr.org.tspb.constants.ProjectConstants.JAVAUTIL_OBJECTID;
import static tr.org.tspb.constants.ProjectConstants.MESSAGE_DIALOG;
import static tr.org.tspb.constants.ProjectConstants.MONGO_ID;
import static tr.org.tspb.constants.ProjectConstants.OPERATOR_LDAP_UID;
import static tr.org.tspb.constants.ProjectConstants.RETVAL;
import static tr.org.tspb.constants.ProjectConstants.STATE;
import static tr.org.tspb.constants.ProjectConstants.TYPE;
import static tr.org.tspb.constants.ProjectConstants.UPDATE_DATE;
import static tr.org.tspb.constants.ProjectConstants.UPDATE_USER;
import static tr.org.tspb.constants.ProjectConstants.UYS_EASY_FIND_KEY;
import static tr.org.tspb.constants.ProjectConstants.VALUE;

import tr.org.tspb.datamodel.dao.*;
import tr.org.tspb.constants.exceptions.FormConfigException;
import tr.org.tspb.constants.exceptions.LdapException;
import tr.org.tspb.constants.exceptions.MongoOrmFailedException;
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.constants.exceptions.UserException;
import tr.org.tspb.datamodel.pojo.*;
import tr.org.tspb.factory.cp.OgmCreatorIntr;
import tr.org.tspb.factory.qualifier.OgmCreatorQualifier;
import tr.org.tspb.service.CalcService;
import tr.org.tspb.service.CtrlService;
import tr.org.tspb.service.FilterService;
import tr.org.tspb.service.RepositoryService;
import tr.org.tspb.util.crypt.RandomString;
import tr.org.tspb.util.qualifier.KeepOpenQualifier;
import tr.org.tspb.util.service.DlgCtrl;
import tr.org.tspb.util.stereotype.MyController;
import tr.org.tspb.util.tools.MongoDbUtilIntr;
import tr.org.tspb.util.tools.MongoDbVersion;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;

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
@MyQualifier(myEnum = ViewerController.fmsMultiFormBulkUpload)
public class FmsMultiFormBulkUpload implements Serializable {

    @Inject
    protected DlgCtrl dialogController;

    @Inject
    @KeepOpenQualifier
    protected MongoDbUtilIntr mongoDbUtil;

    @Inject
    @MyLoginQualifier
    protected LoginController loginController;

    @Inject
    protected AppScopeSrvCtrl uysApplicationMB;

    @Inject
    protected Logger logger;

    @Inject
    private RepositoryService repositoryService;

    @Inject
    protected BaseService baseService;

    @Inject
    @MyCtrlServiceQualifier
    CtrlService ctrlService;

    @Inject
    protected CalcService calcService;

    @Inject
    MailService mailService;

    @Inject
    protected FilterService filterService;

    private boolean enableHistoryOnSave = true;

    private final int fileLimit = 1;
    private final String invalidFileMessage = "Geçersiz Dosya Tipi (Sadece *.xslx uzantılı Excel dosyalar eklenebilir)";
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.mm.yyyy");

    private UploadedFile uploadedFile;
    private List<Document> listOfToBeUpsert1 = new ArrayList<>();
    private List<Document> listOfToBeUpsert2 = new ArrayList<>();
    private List<Document> listOfToBeUpsert3 = new ArrayList<>();
    private List<Document> listOfToBeUpsert4 = new ArrayList<>();
    private List<Document> listOfToBeUpsert5 = new ArrayList<>();
    private List<Document> listOfToBeUpsert6 = new ArrayList<>();
    private List<Document> listOfToBeUpsert7 = new ArrayList<>();

    private FmsForm myForm1;
    private FmsForm myForm2;
    private FmsForm myForm3;
    private FmsForm myForm4;
    private FmsForm myForm5;
    private FmsForm myForm6;
    private FmsForm myForm7;

    private static final long TIMEOUT = 30;

    private Cache<String, Object> tokens;

    @PostConstruct
    public void init() {
        tokens = CacheBuilder.newBuilder().
                expireAfterWrite(TIMEOUT, TimeUnit.DAYS).
                build();
    }

    public String createToken(Object data) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, data);
        return token;
    }

    public Boolean isTokenValid(String token) {
        return tokens.getIfPresent(token) != null;
    }

    private void resetForms() throws NullNotExpectedException,
            MongoOrmFailedException {
        MyProject myProject = uysApplicationMB.getProject("gyonadvd");
        myForm1 = repositoryService.getMyFormLarge(myProject, "varlik_turu_bir");
        myForm2 = repositoryService.getMyFormLarge(myProject, "varlik_turu_iki");
        myForm3 = repositoryService.getMyFormLarge(myProject, "varlik_turu_uc");
        myForm4 = repositoryService.
                getMyFormLarge(myProject, "varlik_turu_dort");
        myForm5 = repositoryService.getMyFormLarge(myProject, "varlik_turu_bes");
        myForm6 = repositoryService.
                getMyFormLarge(myProject, "varlik_turu_alti");
        myForm7 = repositoryService.
                getMyFormLarge(myProject, "varlik_turu_yedi");
    }

    private Map<String, Object> uploadedMergeObject = new HashMap<>();

    @Inject
    @OgmCreatorQualifier
    private OgmCreatorIntr ogmCreator;

    public String bulkLoadExcell() {
        try {
            localBulkLoadExcell();
        } catch (Exception ex) {
            dialogController.showPopupError(ex.getMessage());
        }
        return null;
    }

    public void uploadExcell(FileUploadEvent event) {

        uysApplicationMB.initKpbMemberCache();

        UploadedFile uploadedFile = event.getFile();

        try {

            InputStream is = uploadedFile.getInputStream();
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(is);

            resetForms();
            resetUpsertList();

            StringBuilder sb = new StringBuilder();

            writeToList(sb, listOfToBeUpsert1, myForm1, xssfWorkbook, 0);
            writeToList(sb, listOfToBeUpsert2, myForm2, xssfWorkbook, 1);
            writeToList(sb, listOfToBeUpsert3, myForm3, xssfWorkbook, 2);
            writeToList(sb, listOfToBeUpsert4, myForm4, xssfWorkbook, 3);
            writeToList(sb, listOfToBeUpsert5, myForm5, xssfWorkbook, 4);
            writeToList(sb, listOfToBeUpsert6, myForm6, xssfWorkbook, 5);
            writeToList(sb, listOfToBeUpsert7, myForm7, xssfWorkbook, 6);

            if (!sb.toString().
                    isEmpty()) {
                dialogController.showPopupError(
                        "Dosya Yükleme Esnasında oluşan hatalar :<br/><br/>".
                                concat(sb.toString()));
            }

        } catch (Exception ex) {
            resetUpsertList();
            logger.error("error occured", ex);
            dialogController.showPopupError(
                    "Dosya Yükleme Esnasında hata oluştu.<br/><br/>".concat(ex.
                            getMessage()));
        }

    }

    record BulkLoadTask(FmsForm form, List<Document> list) {
    }

    private boolean isExcelFieldEmpty(String str) {
        if (str == null) {
            return true;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty()
                || "null".equalsIgnoreCase(trimmed)
                || "BSON_CONVERTER_NULL_VALUE".equalsIgnoreCase(trimmed);
    }


    private void crossCheckOverAllLists() {
        // -------------------------------------------------------------------------
        // ==== [NAD403] GYO-NAD EXCEL 3 SAYFA (VARLIK 1, 2, 3) BOŞ OLMA KONTROLÜ (RAM SEVİYESİNDE)
        // -------------------------------------------------------------------------
        int varlik1SatirSayisi = (listOfToBeUpsert1 != null) ? listOfToBeUpsert1.size() : 0;
        int varlik2SatirSayisi = (listOfToBeUpsert2 != null) ? listOfToBeUpsert2.size() : 0;
        int varlik3SatirSayisi = (listOfToBeUpsert3 != null) ? listOfToBeUpsert3.size() : 0;

        int toplamYuklenenSatir = varlik1SatirSayisi + varlik2SatirSayisi + varlik3SatirSayisi;

        // Eğer 3 listeden de (sayfadan da) hafızaya tek bir satır dahi gelmediyse aktarımı durdur!
        if (toplamYuklenenSatir == 0) {
            // Katı blokaj popup uyarısını fırlatıyoruz
            dialogController.showPopupError("""
                    [NAD-403] Excel Yükleme Reddedildi! Yüklemeye çalıştığınız dosyada<br/>
                    'Arsa ve Araziler', 'Yapılmakta Olan Yatırımlar' ve 'Binalar' sayfalarının tümü boş görünmektedir.<br/> 
                    Sisteme aktarım yapılabilmesi için bu 3 sayfadan en az birinde, en az 1 satır veri bulunmalıdır!<br/>
                    """);
            resetUpsertList();
            return; // Aşağıdaki loadAllSheets döngüsüne girmeden çık
        }

        // -------------------------------------------------------------------------
        // ===== [NAD402] GYO-NAD EXCEL 175 CİNSİ İÇİN ÇİFT YÖNLÜ ALAN KONTROLÜ (RAM SEVİYESİNDE)
        // -------------------------------------------------------------------------

        //writeToList içinde excelden gelen cinsi kodunu yakalıp karşılaştıralım
        if (listOfToBeUpsert2 != null && !listOfToBeUpsert2.isEmpty()) {
            int rowNum = 1; // Satır numarasını kullanıcıya doğru göstermek için
            String gelirPaylasimliProje = "175"; // Excelde girilen cins kodu

            for (org.bson.Document doc : listOfToBeUpsert2) {
                rowNum++; // Başlık satırını hesaba katarak artırıyoruz, exceldeki satırı verir, referansı değil

                // writeToList içinde dökümana enjekte ettiğimiz ham değeri doğrudan çekiyoruz
                String hamCinsKodu = doc.getString("ham_cinsi_kodu");
                if (hamCinsKodu == null) {
                    hamCinsKodu = "";
                }

                String yatirimModeli = (doc.get("yatirim_modeli") != null) ? doc.get("yatirim_modeli").toString() : null;
                String yatirimTuru = (doc.get("yatirim_turu") != null) ? doc.get("yatirim_turu").toString() : null;

                if (!hamCinsKodu.isEmpty()) {
                    // Tamamen kurulum ve veritabanı bağımsız "175" kod kontrolü
                    if (gelirPaylasimliProje.equals(hamCinsKodu)) {
                        // -----------------------------------------------------------------
                        // SENARYO A: Gelir Paylaşımlı Projeler (175) ise -> Alanlar DOLU OLMALI
                        // -----------------------------------------------------------------
                        if (isExcelFieldEmpty(yatirimModeli) || isExcelFieldEmpty(yatirimTuru)) {
                            String msg = String.format("""
                                    [NAD-402]  <br/><br/>
                                    Excel Yükleme Reddedildi! Yapılmakta Olan Yatırımlar sayfasında (Satır: %d)  <br/>
                                    seçilen 'Gelir Paylaşımlı Projeler' (%s) cinsi için Yatırım Modeli ve  <br/><br/>
                                    Yatırım Türü alanları boş bırakılamaz!  <br/>
                                    Lütfen dosyanızı kontrol ediniz. <br/>""", rowNum, gelirPaylasimliProje);
                            dialogController.showPopupError(msg);
                            resetUpsertList();
                            return; // Aşağıdaki loadAllSheets döngüsüne girmeden çık
                        }
                    } else {
                        // -----------------------------------------------------------------
                        // SENARYO B: 175 DIŞINDA bir cins ise -> Alanlar KESİNLİKLE BOŞ OLMALI
                        // -----------------------------------------------------------------
                        if (!isExcelFieldEmpty(yatirimModeli) || !isExcelFieldEmpty(yatirimTuru)) {
                            String msg = String.format("""
                                    [NAD-402]  <br/><br/>
                                    Excel Yükleme Reddedildi! Yapılmakta Olan Yatırımlar sayfasında (Satır: %d) <br/>
                                    seçilen varlık cinsi için Yatırım Modeli ve Yatırım Türü alanları doldurulamaz! <br/> <br/>
                                    Bu alanlar sadece 'Gelir Paylaşımlı Projeler' (" + %s + ") cinsi için geçerlidir. <br/><br/>
                                    Lütfen hücreyi temizleyiniz. <br/>""", rowNum, gelirPaylasimliProje);
                            dialogController.showPopupError(msg);
                            resetUpsertList();
                            return; // Aşağıdaki loadAllSheets döngüsüne girmeden çık
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // ==== [NAD401] GYO-NAD EXCEL İÇİN BELLEK (RAM) SEVİYESİNDE %10 DENETİMİ
        // -----------------------------------------------------------------
        double excelDigerTabloToplamDeger = 0.0;
        if (listOfToBeUpsert5 != null && !listOfToBeUpsert5.isEmpty()) {
            for (org.bson.Document doc : listOfToBeUpsert5) {
                if (doc.get("deger") != null) {
                    excelDigerTabloToplamDeger += ((Number) doc.get("deger")).doubleValue();
                }
            }
        }

        // Eğer "Diğer" tablosunda Excel'den gelen ham bir tutar varsa denetimi simüle et
        if (excelDigerTabloToplamDeger > 0) {
            double toplamArsaArazi = 0.0;
            double toplamYapilmakta = 0.0;
            double toplamBinalar = 0.0;
            double toplamIstirak = 0.0;

            // Excel'deki 1, 2, 3 ve 4. sekmelerden (RAM'den) güncel ham değerleri topluyoruz
            if (listOfToBeUpsert1 != null) {
                for (org.bson.Document d : listOfToBeUpsert1) {
                    if (d.get("deger") != null) {
                        toplamArsaArazi += ((Number) d.get("deger")).doubleValue();
                    }
                }
            }
            if (listOfToBeUpsert2 != null) {
                for (org.bson.Document d : listOfToBeUpsert2) {
                    if (d.get("deger") != null) {
                        toplamYapilmakta += ((Number) d.get("deger")).doubleValue();
                    }
                }
            }
            if (listOfToBeUpsert3 != null) {
                for (org.bson.Document d : listOfToBeUpsert3) {
                    if (d.get("deger") != null) {
                        toplamBinalar += ((Number) d.get("deger")).doubleValue();
                    }
                }
            }
            if (listOfToBeUpsert4 != null) {
                for (org.bson.Document d : listOfToBeUpsert4) {
                    if (d.get("deger") != null) {
                        toplamIstirak += ((Number) d.get("deger")).doubleValue();
                    }
                }
            }

            // ARINDIRILMIŞ NİHAİ PAYDA SİMÜLASYONU: Excel'deki H2 hücresinin formülüyle birebir aynı kümülatif matematiksel zincir:
            double predictedOverallDenominator = toplamArsaArazi + toplamYapilmakta + toplamBinalar + toplamIstirak + excelDigerTabloToplamDeger;

            double predictedRatio = 0.0;
            if (predictedOverallDenominator > 0) {
                predictedRatio = excelDigerTabloToplamDeger / predictedOverallDenominator;
            }

            // KÜMÜLATİF LİMİT KONTROLÜ: Eğer oran %10 sınırını aşıyorsa Excel'i kapıda reddet!
            if (predictedRatio > 0.10001) {
                double roundedTotal = Math.round(predictedRatio * 100.0 * 100.0) / 100.0;

                logger.error("======> [NAD-401] localBulkLoadExcell Tetiklendi:  Diğer varlık değerleri %10 u aştı. Hesaplanan kümülatif pay: %" + roundedTotal);
                // Katı blokaj popup uyarısını fırlatıyoruz
                String msg = String.format("""
                        [NAD-401] <br/>
                        Excel Yükleme Reddedildi! Diğer varlık değerleri,<br/>
                        toplam portföy payının %10'unu geçemez. <br/>
                        Lütfen verilerinizi kontrol ediniz. <br/>
                        Hesaplanan kümülatif pay: %%%d <br/>""" + roundedTotal);
                dialogController.showPopupError(msg);

                resetUpsertList(); // Bellekteki temizle
                return; // Aşağıdaki loadAllSheets döngüsüne girmeden çık
            }
        }
    }

    public String localBulkLoadExcell() {

        crossCheckOverAllLists();

        try {
            List<BulkLoadTask> tasks = List.of(
                    new BulkLoadTask(myForm1, listOfToBeUpsert1),
                    new BulkLoadTask(myForm2, listOfToBeUpsert2),
                    new BulkLoadTask(myForm3, listOfToBeUpsert3),
                    new BulkLoadTask(myForm4, listOfToBeUpsert4),
                    new BulkLoadTask(myForm5, listOfToBeUpsert5),
                    new BulkLoadTask(myForm6, listOfToBeUpsert6),
                    new BulkLoadTask(myForm7, listOfToBeUpsert7)
            );
            // 3. Process the tasks cleanly without casting
            Map<String, Document> allCollectUpdates = loadAllSheets(tasks);

            for (Map.Entry<String, Document> entry : allCollectUpdates.entrySet()) {

                TagEvent eventPostSaveFile = myForm1.getUploadMerge().getEventPostSaveFile();
                if (eventPostSaveFile == null) {
                    String errorMsg = String
                            .format("eventPostSaveFile is not defined on \"%s\".", myForm1.getName());
                    throw new Exception(errorMsg);
                }

                String eventPostSaveFileApiUri = eventPostSaveFile.getUri();

                Document dbo = entry.getValue();
                PostSaveResult postSaveResult = repositoryService.
                        runEventPostSaveByGivenTagEvent(
                                myForm1.getMyProject().getKey(),
                                myForm1.getMyProject().getApiToken(),
                                eventPostSaveFileApiUri,
                                dbo);
                String msg = postSaveResult.getMsg();
                if (msg != null) {
                    StringBuilder dlgSb = new StringBuilder();
                    dlgSb.append("*Kayıt Sonrası* tetikleyici çalıştırılıyor iken hata oluştu. ");
                    dlgSb.append("<br/><br/>");
                    dlgSb.append(msg);
                    dialogController.showPopupInfoWithOk(postSaveResult.
                            getMsg(), MESSAGE_DIALOG);
                }
            }


        } catch (Exception ex) {
            logger.error("Error occurred during bulk excel load", ex);
            String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            dialogController.showPopupError("Dosya Yükleme Esnasında hata oluştu.<br/><br/>" + errorMsg);
        }
        return null;
    }

    private Map<String, Document> loadAllSheets(List<BulkLoadTask> tasks) throws Exception {
        Map<String, Document> allCollectUpdates = new HashMap<>();

        for (BulkLoadTask task : tasks) {
            // Optional: Skip if data isn't initialized yet
            if (task.form() == null || task.list() == null) continue;

            Map<String, Document> collectUpdates = localBulkLoadExcellBy(task.form(), task.list());
            allCollectUpdates.putAll(collectUpdates);
            logger.info("{} completed", task.form().getName());
        }

        return allCollectUpdates;

    }

    public Map<String, Document> localBulkLoadExcellBy(FmsForm fmsForm, List<Document> listOfToBeUpsert)
            throws Exception {

        TagEvent eventPostSaveSheet = fmsForm.getUploadMerge().getEventPostSaveSheet();
        if (eventPostSaveSheet == null) {
            throw new Exception("eventPostSaveSheet is not defined");
        }

        String eventPostSaveSheetApiUri = eventPostSaveSheet.getUri();

        List<String> upsertKeys = null;
        if (!fmsForm.getUploadMerge().
                getUpsertFields().
                isEmpty()) {
            upsertKeys = new ArrayList<>();
            for (MyField field : fmsForm.getUploadMerge().
                    getUpsertFields()) {
                upsertKeys.add(field.getKey());
            }
        }

        Map<String, Document> collectUpdates = new HashMap<>();

        if (fmsForm.getUploadMerge().isInsert()) {
            for (Document dbo : listOfToBeUpsert) {
                dbo.putAll(uploadedMergeObject);
                MyMap mm = ogmCreator.getCrudObject();
                mm.putAll(dbo);
                saveObject(fmsForm, loginController, mm, fmsForm);
            }
        } else if (fmsForm.getUploadMerge().isUpdate()) {
            int no = 0;

            for (Document dbo : listOfToBeUpsert) {
                logger.info(
                        String.format("%s : %s : upsert row : %03d",
                                loginController.getLoggedUserDetail().getCommonName(),
                                fmsForm.getName(),
                                no++
                        ));
                dbo.putAll(uploadedMergeObject);
                Document search = new Document(FORMS, fmsForm.getKey());
                if (upsertKeys != null) {
                    for (String key : upsertKeys) {
                        search.put(key, dbo.get(key));
                    }
                } else {
                    search.putAll(dbo);
                }

                try {
                    MyMap myMap = new MyMap();
                    myMap.putAll(dbo);
                    PreSaveResult preSaveResult = repositoryService.
                            runEventPreSave(search, fmsForm, myMap);
                    if (!preSaveResult.isResult()) {
                        mongoDbUtil
                                .updateMany(
                                        fmsForm.getUploadMerge().getToDb(),
                                        fmsForm.getUploadMerge().getToCollection(),
                                        search,
                                        dbo,
                                        new UpdateOptions().upsert(true));

                        String uniqueKey = String.format("m:%s|p:%s",
                                search.get("member").toString(),
                                search.get("period").toString()
                        );
                        collectUpdates.put(uniqueKey, dbo);
                    } else {
                        StringBuilder dlgSb = new StringBuilder();
                        dlgSb.append(
                                "*Kayıt Öncesi* tetikleyici çalıştırılıyor iken hata oluştu. ");
                        dlgSb.append("<br/><br/>");
                        dlgSb.append(fmsForm.getName());
                        dlgSb.append("<br/><br/>");
                        dlgSb.append(preSaveResult.getMsg());
                        throw new Exception(dlgSb.toString());
                    }
                } catch (Exception ex) {
                    logger.error("error occured", ex);
                    FacesContext.getCurrentInstance().
                            addMessage(null,
                                    new FacesMessage(FacesMessage.SEVERITY_FATAL,
                                            "Hata", "Hata"));
                }
            }

            for (Map.Entry<String, Document> entry : collectUpdates.entrySet()) {
                Document dbo = entry.getValue();
                dbo.append("table", fmsForm.getTable());
                PostSaveResult postSaveResult = repositoryService.
                        runEventPostSaveByGivenTagEvent(
                                fmsForm.getMyProject().getKey(),
                                fmsForm.getMyProject().getApiToken(),
                                eventPostSaveSheetApiUri, dbo);

                String msg = postSaveResult.getMsg();

                if (msg != null) {
                    StringBuilder dlgSb = new StringBuilder();
                    dlgSb.append("*Kayıt Sonrası* tetikleyici çalıştırılıyor iken hata oluştu. ");
                    dlgSb.append("<br/><br/>");
                    dlgSb.append(msg);
                    dialogController.showPopupInfoWithOk(postSaveResult.
                            getMsg(), MESSAGE_DIALOG);
                }
            }

        }

        return collectUpdates;
    }

    private void resetUpsertList() {
        listOfToBeUpsert1 = new ArrayList<>();
        listOfToBeUpsert2 = new ArrayList<>();
        listOfToBeUpsert3 = new ArrayList<>();
        listOfToBeUpsert4 = new ArrayList<>();
        listOfToBeUpsert5 = new ArrayList<>();
        listOfToBeUpsert6 = new ArrayList<>();
        listOfToBeUpsert7 = new ArrayList<>();
    }

    private void writeToList(StringBuilder sb, List<Document> listOfToBeUpsert,
                             FmsForm myForm, XSSFWorkbook xssfWorkbook, int sheetNo)
            throws IOException, Exception {

        MyMerge myMerge = myForm.getUploadMerge();
        if (myMerge == null) {
            throw new Exception("upload merge config has not been defined");
        }

        XSSFSheet sheet = xssfWorkbook.getSheetAt(sheetNo);

        int rowcount = sheet.getLastRowNum() + 1;
        int startrow = myMerge.getWorkbookSheetStartRow();
        List<String> listOfNotFoundMembers = new ArrayList<>();
        for (int i = startrow; i < rowcount; i++) {
            XSSFRow row = sheet.getRow(i);
            //empty rows is resolved to null
            if (row == null) {
                continue;
            }

            Document record = new Document().append(FORMS, myForm.getKey());
            try {
                if (!appendField(myMerge, row, sheet, i, record, myForm)) {
                    break;
                }
                // 🚀===== [NAD402] HAM EXCEL DEĞERİNİ DÖKÜMANA ENJEKTE ETME (175=175 YAKALAMAK İÇİN) =====
                // Sadece 2. sekme (Yapılmakta Olan Yatırımlar - sheetNo: 1) için bu yedekleme kuralını çalıştır
                if (sheetNo == 1) {
                    org.apache.poi.xssf.usermodel.XSSFCell cellC = row.getCell(2); // C Sütunu (Cinsi)
                    String hamCinsKodu = "";
                    if (cellC != null) {
                        if (cellC.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            hamCinsKodu = String.valueOf((int) cellC.getNumericCellValue()).trim();
                        } else {
                            hamCinsKodu = cellC.getStringCellValue().trim();
                        }
                    }
                    // Yakaladığımız ham string "175" veya diğer değerleri dökümana yedek alan olarak yazıyoruz
                    record.put("ham_cinsi_kodu", hamCinsKodu);
                }
                // -------------------------------------------------------------------------------------

            } catch (NullPointerException e) {
                sb.append(String.format("NullPointerException at sheet %s row %s </br>", sheetNo, i));
                continue;
            } catch (Exception e) {
                sb.append(e.getMessage().concat("</br>"));
                continue;
            }

            if (!loginController.isUserInRole(myForm.getMyProject().
                    getAdminAndViewerRole())) {
                record.put(myForm.getLoginFkField(), loginController.
                        getLoggedUserDetail().
                        getDbo().
                        getObjectId());
            }

            listOfToBeUpsert.add(record);
        }
        if (!listOfNotFoundMembers.isEmpty()) {
            throw new Exception(
                    "Kayıtlı üye listesinde aşağıdaki kurum isimleri tespit edilemedi");
        }
        for (String message : listOfNotFoundMembers) {
            throw new Exception(message);
        }
    }

    private boolean appendField(MyMerge myMerge, XSSFRow row, XSSFSheet sheet,
                                int i, Document record, FmsForm myForm) throws Exception {

        XSSFCell cellll;
        int columnn = 0;

        for (ExcellColumnDef excellColumnDef : myMerge.getWorkbookSheetColumnList()) {

            cellll = row.getCell(columnn++);

            if (cellll == null || cellll.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK) {

                // we use referansNo emty check as an end_of_sheet
                if ("referansNo".equals(excellColumnDef.getToMyField().
                        getKey())) {
                    //addtional check for next column existing
                    int nextColumn = columnn + 1;
                    cellll = row.getCell(nextColumn);
                    if (cellll != null && cellll.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK) {
                        throw new Exception(
                                String.format(
                                        "sayfa '%s' : satır '%d' : sütun '%d' : %s' alanı zorunlu alandır",
                                        sheet.getSheetName(), i + 1, columnn,
                                        excellColumnDef.getToMyField().
                                                getName()));
                    }
                    return false;
                }

                if (excellColumnDef.getToMyField().
                        isRequired()) {
                    throw new Exception(
                            String.format(
                                    "sayfa '%s' : satır '%d' : sütun '%d' : %s' alanı zorunlu alandır",
                                    sheet.getSheetName(), i + 1, columnn,
                                    excellColumnDef.getToMyField().
                                            getName()));
                }
                record.append(excellColumnDef.getToMyField().
                        getKey(), null);
                continue;
            }

            Object obtainedValue = null;

            if (cellll.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                obtainedValue = switch (excellColumnDef.getToMyField().
                        getValueType()) {
                    case JAVAUTIL_DATE, JAVALANG_DATE -> dateFormat.parse(cellll.
                            getStringCellValue());
                    case JAVALANG_STRING -> cellll.getStringCellValue();
                    default -> cellll.getStringCellValue();
                };
            } else if (cellll.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                obtainedValue = switch (excellColumnDef.getToMyField().
                        getValueType()) {
                    case JAVAUTIL_DATE -> cellll.getDateCellValue();
                    case JAVALANG_DATE -> cellll.getDateCellValue();
                    case JAVALANG_STRING -> String.valueOf(((Number) cellll.
                            getNumericCellValue()).longValue());
                    case JAVAUTIL_OBJECTID -> cellll.getNumericCellValue();
                    case JAVALANG_INTEGER -> Double.valueOf(cellll.getNumericCellValue());
                    default -> cellll.getNumericCellValue();
                }; //obtainedValue = new Double(cellll.getNumericCellValue()).intValue();//max value 2^31-1=2147483647
            }

            // we use referansNo emty check as an end_of_sheet
            if (obtainedValue instanceof String
                    && obtainedValue.toString().
                    trim().
                    isEmpty()
                    && "referansNo".equals(excellColumnDef.getToMyField().
                    getKey())) {
                return false;
            }

            FmsWorkbookColumnConverter converter = excellColumnDef.getConverter();

            Object resolvedValue = handleConverter(sheet, i, myForm, excellColumnDef, converter, columnn, obtainedValue);

            record.append(excellColumnDef.getToMyField().getKey(), resolvedValue);

        }
        return true;
    }

    private Object handleConverter(XSSFSheet sheet, int i, FmsForm myForm, ExcellColumnDef excellColumnDef,
                                   FmsWorkbookColumnConverter converter, int columnn, Object obtainedValue)
            throws Exception {

        if (converter == null) {
            return obtainedValue;
        }

        String token = sheet.getSheetName().
                concat("__").
                concat(Integer.toString(columnn).
                        concat("__").
                        concat(obtainedValue.toString()));

        Object resolvedValue = tokens.getIfPresent(token);

        if (resolvedValue != null) {
            return resolvedValue;
        }

        String db = myForm.getUploadMerge().getToDb();
        String table = myForm.getTable();
        String memberType = loginController.getLoggedUserDetail().getDbo().getMemberType();
        String convertType = converter.getType();
        try {
            if ("external-api".equals(convertType)) {

                String uri = "http://localhost:8080" + converter.getUri();

                Map<String, String> payload = new HashMap<>();

                payload.put("form", table);
                payload.put("value", obtainedValue == null ? "" : obtainedValue.toString());
                payload.put("memberType", memberType);

                try (Client client = ClientBuilder.newClient()) {
                    Response response = client.target(uri)
                            .request(MediaType.APPLICATION_JSON)
                            .header("X-API-KEY", myForm.getMyProject().getApiToken()).header("X-API-PROJECT", myForm.getMyProject().getKey())
                            .post(Entity.entity(payload, MediaType.APPLICATION_JSON));

                    if (response.getStatus() == 200) {
                        Document targetInfo = response.readEntity(Document.class);
                        String type = targetInfo.getString("type"); // Pulls out the raw database hex string identifier
                        String val = targetInfo.getString("value"); // Pulls out the raw database hex string identifier

                        switch (type) {
                            case "objectId" -> resolvedValue = new ObjectId(val);
                            case "Integer" -> resolvedValue = Integer.valueOf(val);
                            case "String" -> resolvedValue = val;
                            default -> resolvedValue = val;
                        }
                    } else {
                        throw new Exception("Can not resolve data for : " + obtainedValue);
                    }
                    response.close();
                }
            } else if ("aggregate".equals(convertType)) {
                String converterCode = excellColumnDef.getConverter().getOp();
                Document commandResult = mongoDbUtil
                        .runCommand(db, converterCode, obtainedValue, memberType);
                resolvedValue = commandResult.get(RETVAL);
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

        } catch (Exception ex) {
            throw new Exception(
                    String.format(
                            "sayfa / %s / satır / <b>%03d</b> / sütun / <b>%02d</b>  ->  [%s = %s] sistemde tanımlı değil.",
                            sheet.getSheetName(), i, columnn,
                            excellColumnDef.getToMyField().
                                    getName(), obtainedValue));
        }

        if (resolvedValue != null) {
            tokens.put(token, resolvedValue);
        }

        return resolvedValue;
    }

    /**
     * @return the fileLimit
     */
    public int getFileLimit() {
        return fileLimit;
    }

    /**
     * @return the invalidFileMessage
     */
    public String getInvalidFileMessage() {
        return invalidFileMessage;
    }

    /**
     * @return the uploadedFile
     */
    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    /**
     * @param uploadedFile the uploadedFile to set
     */
    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
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

    private ObjectId saveObject(FmsForm myForm, LoginController loginMB,
                                MyMap crudObject, FmsForm fmsForm)
            throws UserException, MessagingException, NullNotExpectedException,
            LdapException, FormConfigException, MongoOrmFailedException {

        Object loginFkFieldValue = crudObject.get(fmsForm.getLoginFkField());

        if (loginFkFieldValue instanceof MyBaseRecord) {
            loginFkFieldValue = ((MyBaseRecord) loginFkFieldValue).getObjectId();
        }

        boolean ok = loginController.isUserInRole(fmsForm.getMyProject().
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
                fmsForm, request.getRemoteAddr(), sessionId);
        crudObject.put(STATE, "saved");

        return returnID;
    }

    private ObjectId saveOneDimensionObject(Document operatedObject,
                                            String username,
                                            FmsForm myForm, String ip, String sessionId)
            throws MessagingException, NullNotExpectedException, LdapException,
            FormConfigException, MongoOrmFailedException, UserException {

        FmsForm inode = (FmsForm) operatedObject.get(INODE);
        operatedObject.remove(INODE);//just to sutisfy the icefaces
        if (inode == null) {
            inode = myForm;
        }

        ctrlService.checkRecordConverterValueType(operatedObject, myForm);

        operatedObject = repositoryService.expandCrudObject(myForm,
                operatedObject);

        operatedObject.put(OPERATOR_LDAP_UID, username);
        operatedObject.put(FORMS, myForm.getForm());

        if (myForm.getFindAndSaveFilter() != null) {
            operatedObject.putAll(myForm.getFindAndSaveFilter());
        }

        Document uysAdditionalMetaData = (Document) operatedObject.get(
                ADMIN_METADATA);
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
                operatedObject.put(myField.getKey(), filterService.
                        getTableFilterCurrent().
                        get(myField.getKey()));
            }
        }

        for (String fieldKey : operatedObject.keySet()) {
            MyField fieldStriucture = myForm.getField(fieldKey);
            if (fieldStriucture == null) {
                continue;
            }
            Object fieldValue = operatedObject.get(fieldKey);
            Object defaultValue = fieldStriucture.getDefaultValue();
            if (defaultValue != null && (fieldValue == null || "".equals(
                    fieldValue))) {
                operatedObject.put(fieldKey, defaultValue);
            }
        }

        for (String fieldKey : myForm.getFieldsKeySet()) {
            MyField myField = myForm.getField(fieldKey);
            if (myField.getCalculateOnSave()) {
                operatedObject.put(fieldKey, calcService.calculateValue(
                        operatedObject, myField, FacesContext.
                                getCurrentInstance()));
            }
        }

        String operatorLdapUID = username;

        Document result;

        if (operatedObject.get(MONGO_ID) != null) {

            Bson query = Filters.eq(MONGO_ID, operatedObject.get(MONGO_ID));

            mongoDbUtil.updateOne(inode.getDb(), inode.getTable(), query,
                    operatedObject);

            result = mongoDbUtil.findOne(inode.getDb(), inode.getTable(), query);
        } else {
            // still no way to get the just inserted object id.
            // we dont wont to create id on java side. we want to leave this job to mongodb.
            // for ease retrieving the just inserted object we add an additonal retrieve InsertId to object
            // it can be easly removed later.

            String toBeRetrivedValue = String.format("s:%s_r:%s_t:%s_u:%s_c:%s",
                    sessionId,//
                    new RandomString(32).nextString(),//
                    new Date().getTime(),//
                    username,//
                    myForm.getTable()
            );

            Document record = new Document(operatedObject).append(
                    UYS_EASY_FIND_KEY, toBeRetrivedValue);

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

            result = mongoDbUtil.findOne(inode.getDb(), inode.getTable(),
                    new Document(UYS_EASY_FIND_KEY, toBeRetrivedValue));

            operatedObject.append(MONGO_ID, record.get(MONGO_ID));

        }

        for (String fieldKey : myForm.getFieldsKeySet()) {
            MyField myField = myForm.getField(fieldKey);
            if (myField.getCalculateAfterSave()) {
                result.put(fieldKey, calcService.calculateValue(operatedObject,
                        myField, FacesContext.getCurrentInstance()));
            }
        }

        if (enableHistoryOnSave) {
            try {
                MongoDbVersion.instance(mongoDbUtil).
                        archive(//
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

        try {
            PostSaveResult postSaveResult = repositoryService.runEventPostSave(
                    operatedObject, myForm, null);
            //FIXME messagebundle
            if (postSaveResult.getMsg() != null) {
                dialogController.showPopupInfoWithOk(postSaveResult.getMsg(),
                        MESSAGE_DIALOG);
            }
        } catch (Exception ex) {
            logger.error("error occured", ex);
            StringBuilder dlgSb = new StringBuilder();
            dlgSb.append(
                    "Kayıt Sonrası tetikleyici çalıştırılıyor iken bir hata oluştu. ");
            dlgSb.append("<br/><br/>");
            dlgSb.append("Lütfen bu durumu sistem yöneticisine bildiriniz.");
//            dialogController.showPopupError(dlgSb.toString());
            FacesContext.getCurrentInstance().
                    addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_FATAL,
                                    "Hata",
                                    dlgSb.toString().
                                            replace("<br/>", "")));
        }

//        if (myForm.getMyNotifies() != null) {
//            for (MyNotifies myNotifies : myForm.getMyNotifies().getList()) {
//                myNotifies.reEnable(crudObject);
//                myNotifies.reTo(crudObject);
//                myNotifies.reSubject(crudObject);
//                myNotifies.reContent(crudObject);
//                if (myNotifies.isEnable() && myNotifies.isEmail()) {
//                    mailService.sendMail(myNotifies.getSubject(), myNotifies.getContent(), myNotifies.getTo());
//                }
//            }
//        }
        return (ObjectId) result.get(MONGO_ID);
    }

}