package tr.org.tspb.table;

import com.mongodb.client.model.Filters;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.bson.Document;
import tr.org.tspb.common.qualifier.MyLoginQualifier;
import tr.org.tspb.common.services.LoginController;
import static tr.org.tspb.constants.ProjectConstants.MESSAGE_DIALOG;
import tr.org.tspb.service.RepositoryService;
import tr.org.tspb.util.service.DlgCtrl;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MasakMB2024Ocak implements Serializable {

    private List<Risk> risks;
    private List<Tedbir> tedbirs = new ArrayList<>();

    private Risk selectedRisk;

    @Inject
    private RepositoryService repositoryService;

    @Inject
    @MyLoginQualifier
    private LoginController loginController;

    @Inject
    protected DlgCtrl dialogController;

    @PostConstruct
    private void init() {

        risks = new ArrayList<>();
        risks.add(new Risk("1", "Müşteri Profiline İlişkin Risk Faktörleri",
                true));
        risks.add(new Risk("1.1",
                "Yerli ve Yabancı Siyasi Nüfuz Sahibi kişiler ve/veya bu kişiler ile ilişkili tüzel kişiler ile iş ilişkisine girilmesi"));
        risks.add(new Risk("1.2",
                "Tüzel kişilerin ortaklarına ve/veya gerçek faydalanıcılarına ilişkin bilgi ve belgelerin kurumlar ile paylaşılmaması ve/veya gerçek faydalanıcının tespitinin zorluğu"));
        risks.add(new Risk("1.3",
                "Walk-in müşteri (Tanınmayan müşteri) oranlarının yüksek olması"));
        risks.add(new Risk("1.4",
                "Yasaklı kurum ve kişilerle iş ilişkisine girilmesi"));
        risks.add(new Risk("1.5",
                "SGA/TF ile mücadele konusunda yeterli bilgi sahibi olmadığı değerlendirilen ve/veya küçük sermaye yapısına sahip kurumlarla iş ilişkisine girilmesi"));
        risks.add(new Risk("1.6",
                "Talep edilen işlemin nihai faydalanıcısının tespit edilememesi"));
        risks.add(new Risk("2", "Ürüne/Hizmete İlişkin Risk Faktörleri", true));
        risks.add(new Risk("2.1",
                "Ürün yapısının/işlemlerinin çok aşamalı ve/veya karışık olması"));
        risks.add(new Risk("2.2",
                "Ürünlerin nakde çevrilebilirliğinin (likiditesinin) yüksek olması"));
        risks.add(new Risk("2.3",
                "Yüksek hacimli işlemlere ve tek seferde yüksek tutarlarda işlem yapılmasına olanak sağlayan ürünler"));
        risks.add(new Risk("2.4",
                "Üçüncü tarafa yapılan ve üçüncü taraftan alınan ödemeler"));
        risks.add(new Risk("2.5", "Komisyona dayalı işlemler"));
        risks.add(new Risk("2.6", "Nakit ile gerçekleştirilebilen işlemler"));
        risks.add(new Risk("2.7",
                "Yurtdışı yerleşik gerçek veya tüzel kişilerin yüksek montanlı alım satım işlemleri"));
        risks.add(new Risk("2.8",
                "İşlem hacmi (tutarı) yüksek işlem ve transferler"));
        risks.add(new Risk("2.9", "İşlem sıklığı yüksek işlem ve transferler"));
        risks.add(new Risk("2.10",
                "Karışık ve anlaşılırlığı zorlaştıran (şeffaf olmayan) işlemler"));
        risks.add(new Risk("2.11",
                "Müşterilerin nakit yatırma veya çekme işlemleri gerçekleştirebilmesi"));
        risks.add(new Risk("3", "Coğrafi Risk Faktörleri", true));
        risks.add(new Risk("3.1",
                "Yasaklı ve/veya riskli ülkelerde yerleşik ve/veya ilişkili müşterilerle faaliyet yürütülmesi"));
        risks.add(new Risk("3.2",
                "Uluslararası piyasalarda işlem sıklığının yüksek olması"));
        risks.add(new Risk("4", "Dağıtım Kanallarına İlişkin Risk Faktörleri",
                true));
        risks.add(new Risk("4.1",
                "Müşteri ile doğrudan iletişim sağlanmadan gerçekleştirilebilecek işlemler"));
        risks.add(new Risk("4.2", "Uzaktan gerçekleştirilen işlemler"));
        risks.add(new Risk("4.3",
                "Müşteri bilgileri ile üçüncü kişi adına kurum yetkilileri ile iletişim sağlanmadan gerçekleştirilebilecek işlemler"));
        risks.add(new Risk("4.4",
                "Mobil/internet kanallarında yaşanan güvenlik ihlalleri, sistem kesintileri"));

        tedbirs = new ArrayList<>();
        tedbirs.add(new Tedbir("1",
                "Sunulan ürün ve hizmetler için SGA/TF/KİS riskini dikkate alan yazılı politika ve/veya prosedürlerin bulunması"));
        tedbirs.add(new Tedbir("2",
                "MASAK ve yurtdışı SGA/TF/KİS, yaptırım kapsamındaki mevzuatın takibi yapılarak, sunulan ürün ve hizmetler için SGA/TF/KİS riskini dikkate alarak mevcut iç düzenlemelerin güncel tutulması"));
        tedbirs.add(new Tedbir("3",
                "İç düzenlemelerin mevzuat değişikliğinden bağımsız en az yılda bir kez gözden geçirilmesi"));
        tedbirs.add(new Tedbir("4",
                "SGA/TF/KİS eğitimlerinde riskli ürün/hizmetlerle ilgili alınması gereken tedbirlere ilişkin hususlara yer verilmesi"));
        tedbirs.add(new Tedbir("5",
                "SGA/TF/KİS kapsamında farkındalığı artırma amaçlı, uyum dışı birimlerle (özellikle operasyonel birimlerle) bilgi paylaşılması"));
        tedbirs.add(new Tedbir("6",
                "SGA/TF/KİS konularında kuruluşların iç düzenlemelerine aykırı davranılması durumunda yaptırım uygulanmasına dönük yazılı kural olması"));
        tedbirs.add(new Tedbir("7",
                "Eğitim materyalinin riskli ürün/hizmetlere ilişkin hususlar gözetilerek periyodik olarak gözden geçirilmesi/güncellenmesi"));
        tedbirs.add(new Tedbir("8",
                "Yeni bir ürün, hizmet, kanal vs oluşumların, kullanıma sunulmadan önce SGA/TF/KİS riskleri açısından değerlendirmesinin yapılması ve Uyum görevlisinin ve uyum birimi personelinin ürün/hizmet riskleri konusunda gerekli güncel kaynaklara ulaşabilmesinin sağlanması"));
        tedbirs.add(new Tedbir("9",
                "Riskli durumlarda sıkılaştırılmış tedbirler kapsamında tespit edilen riskle orantılı olarak ek bilgi/belge alınması"));
        //tedbirs.add(new Tedbir("10", "Riskli durumlarda tespit edilen riskle orantılı olarak sıkılaştırılmış tedbirler kapsamındaki diğer tedbirleri uygulamak (Tedbirler Yönetmeliği Maddde 26/A d, e ve f bentleri)"));
        tedbirs.add(new Tedbir("10",
                "Riskli durumlarda tespit edilen riskle orantılı olarak sıkılaştırılmış tedbirler kapsamındaki diğer tedbirlerin uygulanması (Tedbirler Yönetmeliği Maddde 26/A d, e ve f bentleri)"));
        tedbirs.add(new Tedbir("11",
                "Müşteri edinimi/işlem öncesi veya mevcut müşteri veri tabanında periyodik olarak yapılan sakıncalı liste kontrolü sonucunda, listeyle eşleşme çıkması durumunda işlemin engellenmesi/iş ilişkisinin sonlandırılması"));
        tedbirs.add(new Tedbir("12",
                "Müşteri bazında sınırlama/kısıtlama olması"));
        tedbirs.add(new Tedbir("13",
                "İzleme-kontrol faaliyetlerine ilişkin senaryoların periyodik olarak gözden geçirilmesi/güncellenmesi"));
        //tedbirs.add(new Tedbir("14", "İç kontrol ve/veya iç denetim faaliyetlerinde SGA/TF/KİS riskleri dikkate alınması"));
        tedbirs.add(new Tedbir("14",
                "İç kontrol ve/veya iç denetim faaliyetlerinde SGA/TF/KİS risklerinin dikkate alınması"));
        tedbirs.add(new Tedbir("15",
                "Müşteri ediniminde müşteriler için kimlik tespit/teyit sürecine ilave KYC (Müşterini tanı) çerçevesinde risk değerlendirmesi yapılması (müşterilerin risk sınıflandırılmasına yönelik sorular sorulması, risk değerlendirmesi yapılması)"));
        tedbirs.add(new Tedbir("16",
                "Müşteri ediniminde yüksek riskli değerlendirilen müşteriler için ayrıntılı durum değerlendirmesi (ek bilgiler) yapılması, ek onay süreçleri uygulanması (uyum, bir üst yönetici vs)"));
        tedbirs.add(new Tedbir("17",
                "Yeni bir ürün, hizmet, kanal vs oluşumların, kullanıma sunulduktan sonra değişen risk algısıyla uyumlu bir şekilde SGA/TF/KİS riskleri açısından yeniden değerlendirmesinin yapılması"));
        tedbirs.add(new Tedbir("18",
                "Müşterinin tetiklediği işlemlerin yazılım veya manuel araçlar aracılığı ile özel senaryolar üzerinden takip edilmesi"));
        tedbirs.add(new Tedbir("19",
                "Gerçek faydalanıcının beyan edilenden farklı olduğu şüphesi oluştuğunda, gerçek faydalanıcının belirlenmesi için ek çalışma yapılması"));
        tedbirs.add(new Tedbir("20",
                "Müşteri edinimi öncesinde ve periyodik olarak mevcut müşteri veri tabanında sakıncalı liste kontrolü yapılması"));
        //tedbirs.add(new Tedbir("21", "Müşteri işleminin uyum birimi dışındaki kurum personeli tarafından şüpheli bulunması durumunda, Uyum Birimine bildirilmesi"));
        tedbirs.add(new Tedbir("21",
                "Müşteri işleminin uyum birimi dışındaki kurum personeli tarafından şüpheli bulunması durumunda konunun Uyum Birimi’ne bildirilmesi"));

    }

    public void risklerigetir() {
        resetRisks();

        List<Document> docs = repositoryService
                .findRisks(loginController.getLoggedUserDetail().
                        getDbo().
                        getObjectId());

        if (docs != null) {
            initRisksFromDB(docs);
        }

    }

    void initRisksFromDB(List<Document> riskDocs) {

        if (riskDocs == null || riskDocs.isEmpty()) {
            return;
        }

        for (Risk risk : risks) {
            for (Document document : riskDocs) {
                if (risk.getCode().
                        equals(document.getString("risk-code"))) {
                    risk.setExist(true);
                }
            }
        }

    }

    public void tedbirlerigetir() {

        resetTedbirler();

        selectedRisk.setTedbirs(tedbirs);

        Document doc = repositoryService
                .findRiskTedbir(loginController.getLoggedUserDetail().
                        getDbo().
                        getObjectId(),
                        selectedRisk.getCode()
                );

        if (doc != null) {
            selectedRisk.initDataFromDB(doc);
        }
    }

    public String save() {
        repositoryService.saveRisk(selectedRisk.toDocument(), loginController.
                getLoggedUserDetail().
                getDbo().
                getObjectId());

        long count = repositoryService
                .count("anketdb", "masakRisk", Filters.eq("memberId",
                        loginController.getLoggedUserDetail().
                                getDbo().
                                getObjectId()));

        if (count < 23) {
            dialogController.showPopupError(String.format(
                    "23 risk faktöründen %d risk faktörü iletildi. Anket Tamamlanmadı.",
                    count));
        }

        if (count == 23) {
            dialogController.showPopupInfo(
                    String.format(
                            "23 risk faktöründen %d risk faktörü iletildi. Anket Tamamlandı.",
                            count),
                    MESSAGE_DIALOG);
        }

        if (count > 23) {
            dialogController.showPopupError("Hata Oluştu");
        }
        return null;
    }

    /**
     * @return the risks
     */
    public List<Risk> getRisks() {
        return risks;
    }

    /**
     * @param risks the risks to set
     */
    public void setRisks(List<Risk> risks) {
        this.risks = risks;
    }

    /**
     * @return the selectedRisk
     */
    public Risk getSelectedRisk() {
        return selectedRisk;
    }

    /**
     * @param selectedRisk the selectedRisk to set
     */
    public void setSelectedRisk(Risk selectedRisk) {
        this.selectedRisk = selectedRisk;
    }

    private void resetTedbirler() {
        for (Tedbir tedbir : tedbirs) {
            tedbir.setCheck(false);
            tedbir.setKontrolTipi(null);
            tedbir.setUygulamaYonetimi(null);
        }
    }

    private void resetRisks() {
        for (Risk risk : risks) {
            risk.setExist(false);
            risk.setObjectId(null);
        }
    }

}
