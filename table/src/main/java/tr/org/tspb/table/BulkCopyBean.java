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
import tr.org.tspb.datamodel.dao.MyField;
import tr.org.tspb.datamodel.dao.MyItems;
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
        periodList.add(new PeriodItem("2026-03", "202603"));
        periodList.add(new PeriodItem("2026-06", "202606"));
        periodList.add(new PeriodItem("2026-09", "202609"));
        periodList.add(new PeriodItem("2026-12", "202612"));
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

        // Dynamically resolve period field metadata (table/collection and database) via MyField
        MyField periodField = myForm.getField("period");
        MyItems periodItems = periodField != null ? periodField.getItemsAsMyItems() : null;

        String periodCollection = periodItems != null && periodItems.getTable() != null ? periodItems.getTable() : "gsy_pys_period";
        String periodDbName = periodItems != null && periodItems.getDb() != null ? periodItems.getDb() : "gsy_pys_db";

        logger.info("Executing bulk copy for form: {}, target collection: {}, db: {}, period collection: {}, period db: {}, sourcePeriod: {}, targetPeriod: {}",
                myForm.getName(), targetCollection, dbName, periodCollection, periodDbName, sourcePeriod, targetPeriod);

        try {
            Integer sourcePeriodInt = null;
            try {
                sourcePeriodInt = Integer.parseInt(sourcePeriod);
            } catch (Exception e) {
                logger.warn("Could not parse sourcePeriod to int: {}", sourcePeriod);
            }

            Integer targetPeriodInt = null;
            try {
                targetPeriodInt = Integer.parseInt(targetPeriod);
            } catch (Exception e) {
                logger.warn("Could not parse targetPeriod to int: {}", targetPeriod);
            }

            // Retrieve source period document from period collection
            Document sourcePeriodDoc = null;
            if (sourcePeriodInt != null) {
                sourcePeriodDoc = mongoDbUtil.findOne(periodDbName, periodCollection, new Document("value", sourcePeriodInt));
            }
            if (sourcePeriodDoc == null) {
                sourcePeriodDoc = mongoDbUtil.findOne(periodDbName, periodCollection, new Document("value", sourcePeriod));
            }
            if (sourcePeriodDoc == null && !periodDbName.equals(dbName)) {
                sourcePeriodDoc = mongoDbUtil.findOne(dbName, periodCollection, new Document("value", sourcePeriodInt != null ? sourcePeriodInt : sourcePeriod));
            }
            Object sourcePeriodId = sourcePeriodDoc != null ? sourcePeriodDoc.get("_id") : null;

            // Retrieve target period document from period collection
            Document targetPeriodDoc = null;
            if (targetPeriodInt != null) {
                targetPeriodDoc = mongoDbUtil.findOne(periodDbName, periodCollection, new Document("value", targetPeriodInt));
            }
            if (targetPeriodDoc == null) {
                targetPeriodDoc = mongoDbUtil.findOne(periodDbName, periodCollection, new Document("value", targetPeriod));
            }
            if (targetPeriodDoc == null && !periodDbName.equals(dbName)) {
                targetPeriodDoc = mongoDbUtil.findOne(dbName, periodCollection, new Document("value", targetPeriodInt != null ? targetPeriodInt : targetPeriod));
            }
            Object targetPeriodId = targetPeriodDoc != null ? targetPeriodDoc.get("_id") : null;

            if (sourcePeriodId == null) {
                String errMsg = "Kaynak dönem '" + sourcePeriod + "' için döneme ait ID (" + periodCollection + " koleksiyonunda) bulunamadı.";
                logger.error(errMsg);
                if (context != null) {
                    context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata", errMsg));
                }
                throw new IllegalStateException(errMsg);
            }

            if (targetPeriodId == null) {
                String errMsg = "Hedef dönem '" + targetPeriod + "' için döneme ait ID (" + periodCollection + " koleksiyonunda) bulunamadı.";
                logger.error(errMsg);
                if (context != null) {
                    context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Hata", errMsg));
                }
                throw new IllegalStateException(errMsg);
            }

            logger.info("Period lookup successful - sourcePeriodId: {}, targetPeriodId: {}", sourcePeriodId, targetPeriodId);

            Document filter = new Document("period", sourcePeriodId);
            List<Document> sourceDocuments = mongoDbUtil.find(dbName, targetCollection, filter);

            if ((sourceDocuments == null || sourceDocuments.isEmpty()) && sourcePeriodId != null) {
                // Fallback attempt with raw string value if ObjectId filter returned 0 records
                sourceDocuments = mongoDbUtil.find(dbName, targetCollection, new Document("period", sourcePeriod));
            }

            if (sourceDocuments == null || sourceDocuments.isEmpty()) {
                if (context != null) {
                    context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Uyarı", "Kaynak dönemde (" + sourcePeriod + ") kopyalanacak veri bulunamadı."));
                }
                return;
            }

            String loginFkField = myForm.getLoginFkField();
            if (loginFkField == null || loginFkField.trim().isEmpty()) {
                loginFkField = "member";
            }

            int count = 0;
            int skippedCount = 0;

            for (Document doc : sourceDocuments) {
                Object memberId = doc.get(loginFkField);

                if (memberId != null) {
                    Document activeMemberDoc = mongoDbUtil.findOne("uysdb", "common",
                            new Document("_id", memberId).append("status.code", "001")); // FAALİYETTE
                    if (activeMemberDoc == null) {
                        logger.warn("Skipping copy for document id {} because member {} status.code is not '000' in uysdb.common", doc.get("_id"), memberId);
                        skippedCount++;
                        continue;
                    }
                }

                Document newDoc = new Document(doc);
                newDoc.remove("_id");
                newDoc.put("period", targetPeriodId);
                mongoDbUtil.insertOne(dbName, targetCollection, newDoc);
                count++;
            }

            String msgDetail = targetCollection + " koleksiyonu için " + count + " adet veri (" + sourcePeriod + " -> " + targetPeriod + ") kopyalandı.";
            if (skippedCount > 0) {
                msgDetail += " (" + skippedCount + " adet üye durumu '000' olmadığı için kopyalanmadı)";
            }

            if (context != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Başarılı", msgDetail));
            }
            logger.info("Successfully bulk copied {} documents (skipped {}) in collection {} from {} to {}",
                    count, skippedCount, targetCollection, sourcePeriod, targetPeriod);

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