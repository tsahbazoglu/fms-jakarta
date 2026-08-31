package tr.org.tspb.table;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.service.FormService;
import tr.org.tspb.util.qualifier.KeepOpenQualifier;
import tr.org.tspb.util.tools.MongoDbUtilIntr;

@Named
@ViewScoped
public class BulkCopyBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BulkCopyBean.class);

    @Inject
    private FormService formService;

    @Inject
    @KeepOpenQualifier
    private MongoDbUtilIntr mongoDbUtil;

    private String sourcePeriod;
    private String targetPeriod;
    private List<PeriodItem> periodList;

    @PostConstruct
    public void init() {
        periodList = new ArrayList<>();
        periodList.add(new PeriodItem("2026 / 1. Çeyrek (2026-03)", "202603"));
        periodList.add(new PeriodItem("2026 / 2. Çeyrek (2026-06)", "202606"));
        periodList.add(new PeriodItem("2026 / 3. Çeyrek (2026-09)", "202609"));
        periodList.add(new PeriodItem("2026 / 4. Çeyrek (2026-12)", "202612"));
    }

    public void copyData() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (sourcePeriod != null && sourcePeriod.equals(targetPeriod)) {
            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Hata", "Kaynak ve hedef dönem aynı olamaz."));
            }
            return;
        }

        FmsForm myForm = getFormDefinition();
        if (myForm == null) {
            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Hata", "Form tanımı (FormService) bulunamadı."));
            }
            logger.error("FormService or getMyForm() is null during bulk copy execution.");
            return;
        }

        String targetCollection = myForm.getTable();
        String dbName = myForm.getDb();

        logger.info("Executing bulk copy for form: {}, target collection: {}, db: {}, sourcePeriod: {}, targetPeriod: {}", 
                myForm.getName(), targetCollection, dbName, sourcePeriod, targetPeriod);

        try {
            Document filter = new Document("period", sourcePeriod);
            List<Document> sourceDocuments = mongoDbUtil.find(dbName, targetCollection, filter);

            if (sourceDocuments == null || sourceDocuments.isEmpty()) {
                if (context != null) {
                    context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, 
                            "Uyarı", "Kaynak dönemde (" + sourcePeriod + ") kopyalanacak veri bulunamadı."));
                }
                return;
            }

            int count = 0;
            for (Document doc : sourceDocuments) {
                Document newDoc = new Document(doc);
                newDoc.remove("_id");
                newDoc.put("period", targetPeriod);
                mongoDbUtil.insertOne(dbName, targetCollection, newDoc);
                count++;
            }

            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Başarılı", targetCollection + " koleksiyonu için " + count + " adet veri (" + sourcePeriod + " -> " + targetPeriod + ") kopyalandı."));
            }
            logger.info("Successfully bulk copied {} documents in collection {} from {} to {}", 
                    count, targetCollection, sourcePeriod, targetPeriod);

        } catch (Exception e) {
            logger.error("Error during bulk copy execution for collection {}: {}", targetCollection, e.getMessage(), e);
            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Sistem Hatası", "Kopyalama esnasında hata oluştu: " + e.getMessage()));
            }
        }
    }

    public FmsForm getFormDefinition() {
        return formService != null ? formService.getMyForm() : null;
    }

    public String getTargetCollection() {
        FmsForm myForm = getFormDefinition();
        return myForm != null ? myForm.getTable() : null;
    }

    // Getters and Setters
    public String getSourcePeriod() {
        return sourcePeriod;
    }

    public void setSourcePeriod(String sourcePeriod) {
        this.sourcePeriod = sourcePeriod;
    }

    public String getTargetPeriod() {
        return targetPeriod;
    }

    public void setTargetPeriod(String targetPeriod) {
        this.targetPeriod = targetPeriod;
    }

    public List<PeriodItem> getPeriodList() {
        return periodList;
    }

    public void setPeriodList(List<PeriodItem> periodList) {
        this.periodList = periodList;
    }

    // Nested Model Class
    public static class PeriodItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String label;
        private String value;

        public PeriodItem(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}