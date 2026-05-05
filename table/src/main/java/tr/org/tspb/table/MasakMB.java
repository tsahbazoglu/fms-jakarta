package tr.org.tspb.table;

import com.mongodb.client.model.Filters;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import tr.org.tspb.common.qualifier.MyLoginQualifier;
import tr.org.tspb.common.services.LoginController;
import static tr.org.tspb.constants.ProjectConstants.MESSAGE_DIALOG;
import tr.org.tspb.service.RepositoryService;
import tr.org.tspb.util.service.DlgCtrl;
import tr.org.tspb.util.stereotype.MyController;

/**
 *
 * @author Telman Şahbazoğlu
 */
@MyController
public class MasakMB implements Serializable {

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
                "Yerli ve Yabancı Siyasi Nüfuz Sahibi kişiler (eşleri, birinci derece akrabaları veya yakınları dahil) ve/veya bu kişiler ile ilişkili kişiler ile iş ilişkisine girilmesi"));
        risks.add(new Risk("1.2",
                "Tüzel kişilerin ortaklarına ve/veya gerçek faydalanıcılarına ilişkin bilgi ve belgelerin kurumlar ile paylaşılmaması ve/veya gerçek faydalanıcının tespitinin zorluğu"));
        risks.add(new Risk("1.3",
                "Walk-in müşteri (Tanınmayan müşteri) oranlarının yüksek olması"));
        risks.add(new Risk("1.4",
                "Yasaklı kurum ve kişilerle (hakkında MVD kararı olanlar, işlem yasağı olanlar gibi) iş ilişkisine girilmesi"));
        risks.add(new Risk("1.5",
                "SGA/TF ile mücadele konusunda yeterli bilgi sahibi olmadığı değerlendirilen ve/veya küçük sermaye yapısına sahip kurumlarla iş ilişkisine girilmesi"));
        risks.add(new Risk("1.6",
                "Talep edilen işlemin nihai faydalanıcısının tespit edilememesi"));
        risks.add(new Risk("1.7",
                "Yeni faaliyete geçen tüzel kişi müşterilerin kısa süre içerisinde kendi ana faaliyetleri kapsamında yüksek kar göstermesi"));
        risks.add(new Risk("1.8",
                "Müşterinin mali profili ile uyumsuz işlem gerçekleştirmesi (mesleği ve faaliyetleri, gelir kaynakları ve gelir düzeyi ile ilgisi kurulamayan)"));
        risks.add(new Risk("1.9",
                "Yüksek riskli sektörlerde işlem yapan müşteriler (denizcilik, nakliye, inşaat, benzin istasyonu gibi)"));
        risks.add(new Risk("1.10",
                "Hakkında olumsuz medya haberleri çıkan müşteriler ile iş ilişkisine girilmesi (örneğin haklarında devam eden veya sonuçlanmış dava haberleri)"));
        risks.add(new Risk("1.11",
                "İzinsiz veya vekaletname olmaksızın portföy yöneticiliği yapılması (müşterinin çok sayıda hesabın sahibi, ortağı, vekili olması)"));
        risks.add(new Risk("2", "Ürüne/Hizmete İlişkin Risk Faktörleri", true));
        risks.add(new Risk("2.1",
                "Ürün yapısının/işlemlerinin çok aşamalı ve/veya karışık olması, şeffaf olmaması"));
        risks.add(new Risk("2.2",
                "Ürünlerin nakde çevrilebilirliğinin (likiditesinin) yüksek olması"));
        risks.add(new Risk("2.3",
                "Yüksek hacimli işlemlere ve tek seferde yüksek tutarlarda işlem yapılmasına olanak sağlayan ürünler"));
        risks.add(new Risk("2.4",
                "Üçüncü tarafa yapılan ve üçüncü taraftan alınan ödemeler, Menkul Kıymetler"));
        risks.add(new Risk("2.5", "Komisyona dayalı işlemler"));
        risks.add(new Risk("2.6",
                "Yurtdışı yerleşik gerçek veya tüzel kişilerin yüksek montanlı alım satım işlemleri"));
        risks.add(new Risk("2.7",
                "İşlem sıklığı ve hacmi yüksek işlem ve transferler"));
        risks.add(new Risk("2.8",
                "Müşterilerin nakit yatırma veya çekme işlemleri gerçekleştirebilmesi"));
        risks.add(new Risk("3", "Coğrafi Risk Faktörleri", true));
        risks.add(new Risk("3.1",
                "Yasaklı ve/veya riskli ülkelerde yerleşik ve/veya ilişkili müşterilerle (Gerçek Faydalanıcı dahil) faaliyet yürütülmesi"));
        risks.add(new Risk("3.2",
                "Sınır bölgelerinde faaliyet gösteren vakıf ve dernekler ile iş ilişkisine girilmesi"));
        risks.add(new Risk("3.3",
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
        risks.add(new Risk("4.5",
                "Aynı IP adresinden ancak birbiri ile ilişkisi olmayan farklı müşteriler adına işlem yapmak üzere erişim yapılması"));

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
                "SGA/TF/KİS kapsamında farkındalığı artırma amaçlı, uyum dışı birimlerle (özellikle operasyonel ve satış ile müşteri ilişkilerinden sorumlu birimlerle) bilgi paylaşılması"));
        tedbirs.add(new Tedbir("6",
                "SGA/TF/KİS konularında kuruluşların iç düzenlemelerine aykırı davranılması durumunda yaptırım uygulanmasına dönük yazılı kural olması"));
        tedbirs.add(new Tedbir("7",
                "Eğitim materyalinin riskli ürün/hizmetlere ilişkin hususlar gözetilerek periyodik olarak gözden geçirilmesi/güncellenmesi"));
        tedbirs.add(new Tedbir("8",
                "Yeni bir ürün, hizmet, kanal vs oluşumların, kullanıma sunulmadan önce SGA/TF/KİS riskleri açısından değerlendirmesinin yapılması ve Uyum görevlisinin ve uyum birimi personelinin ürün/hizmet riskleri konusunda gerekli güncel kaynaklara ulaşabilmesinin sağlanması"));
        tedbirs.add(new Tedbir("9",
                "Riskli durumlarda sıkılaştırılmış tedbirler kapsamında tespit edilen riskle orantılı olarak ek bilgi/belge alınması"));
        tedbirs.add(new Tedbir("10",
                "Riskli durumlarda tespit edilen riskle orantılı olarak sıkılaştırılmış tedbirler kapsamındaki diğer tedbirlerin uygulanması (Tedbirler Yönetmeliği Maddde 26/A d, e ve f bentleri)"));
        tedbirs.add(new Tedbir("11",
                "Müşteri edinimi/işlem öncesi veya mevcut müşteri veri tabanında periyodik olarak yapılan sakıncalı liste kontrolü sonucunda, listeyle eşleşme çıkması durumunda işlemin engellenmesi/iş ilişkisinin sonlandırılması"));
        tedbirs.add(new Tedbir("12",
                "Çift taraflı doğrulama kullanılacak şekilde ek güvenlik önlemleri alınması"));
        tedbirs.add(new Tedbir("13",
                "Müşteri bazında sınırlama/kısıtlama olması"));
        tedbirs.add(new Tedbir("14",
                "İzleme-kontrol faaliyetlerine ilişkin senaryoların periyodik olarak gözden geçirilmesi/güncellenmesi"));
        tedbirs.add(new Tedbir("15",
                "İç kontrol ve/veya iç denetim faaliyetlerinde SGA/TF/KİS risklerinin dikkate alınması"));
        tedbirs.add(new Tedbir("16",
                "Müşteri ediniminde müşteriler için kimlik tespit/teyit sürecine ilave KYC (Müşterini tanı) çerçevesinde risk değerlendirmesi yapılması (müşterilerin risk sınıflandırılmasına yönelik sorular sorulması, risk değerlendirmesi yapılması)"));
        tedbirs.add(new Tedbir("17",
                "Müşteri ediniminde yüksek riskli değerlendirilen müşteriler için ayrıntılı durum değerlendirmesi (ek bilgiler) yapılması, ek onay süreçleri uygulanması (uyum, bir üst yönetici vs)"));
        tedbirs.add(new Tedbir("18",
                "Yeni bir ürün, hizmet, kanal vs oluşumların, kullanıma sunulduktan sonra değişen risk algısıyla uyumlu bir şekilde SGA/TF/KİS riskleri açısından yeniden değerlendirmesinin yapılması"));
        tedbirs.add(new Tedbir("19",
                "Müşterinin tetiklediği işlemlerin yazılım veya manuel araçlar aracılığı ile özel senaryolar üzerinden takip edilmesi"));
        tedbirs.add(new Tedbir("20",
                "Gerçek faydalanıcının beyan edilenden farklı olduğu şüphesi oluştuğunda, gerçek faydalanıcının belirlenmesi için ek çalışma yapılması"));
        tedbirs.add(new Tedbir("21",
                "Müşteri edinimi öncesinde ve periyodik olarak mevcut müşteri veri tabanında sakıncalı liste kontrolü yapılması"));
        tedbirs.add(new Tedbir("22",
                "Müşteri işleminin uyum birimi dışındaki kurum personeli tarafından şüpheli bulunması durumunda konunun Uyum Birimi'ne bildirilmesi"));

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

        ObjectId loggedUserId = loginController.getLoggedUserDetail().
                getDbo().
                getObjectId();

        repositoryService.saveRisk(selectedRisk.toDocument(), loggedUserId);

        long count = repositoryService
                .count("anketdb", "masakRisk", Filters.eq("memberId",
                        loggedUserId));

        int limit = 27;

        if (count < limit) {
            dialogController.showPopupError(String
                    .format("%d risk faktöründen %d risk faktörü iletildi. Anket Tamamlanmadı.",
                            limit, count));
        }

        if (count == limit) {
            dialogController.showPopupInfo(
                    String.format(
                            "%d risk faktöründen %d risk faktörü iletildi. Anket Tamamlandı.",
                            limit, count),
                    MESSAGE_DIALOG);
        }

        if (count > limit) {
            String msg = String.format(
                    "Masak Hata Oluştu : logge-user-id:%s, count: %d,limit:%d",
                    loggedUserId.toString(), count, limit);
            Logger.getLogger(this.getClass().
                    getName()).
                    log(Level.SEVERE, msg);
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
