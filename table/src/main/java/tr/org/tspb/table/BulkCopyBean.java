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
import org.bson.types.ObjectId;
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
            addFacesMessage(context, FacesMessage.SEVERITY_ERROR, "Hata", "Kaynak ve hedef dönem aynı olamaz.");
            return;
        }

        FmsForm myForm = getFormDefinition();
        if (myForm == null) {
            addFacesMessage(context, FacesMessage.SEVERITY_ERROR, "Hata", "Form tanımı (FormService) bulunamadı.");
            logger.error("FormService or getMyForm() is null during bulk copy execution.");
            return;
        }

        String targetCollection = myForm.getTable();
        String dbName = myForm.getDb();

        MyField periodField = myForm.getField("period");
        MyItems periodItems = periodField != null ? periodField.getEditItemsAsMyItems() : null;
        String periodCollection = periodItems != null && periodItems.getTable() != null ? periodItems.getTable() : "gsy_pys_period";
        String periodDbName = periodItems != null && periodItems.getDb() != null ? periodItems.getDb() : "gsy_pys_db";

        logger.info("Executing bulk copy for form: {}, target collection: {}, db: {}, period collection: {}, period db: {}, sourcePeriod: {}, targetPeriod: {}",
                myForm.getName(), targetCollection, dbName, periodCollection, periodDbName, sourcePeriod, targetPeriod);

        try {
            Integer sourcePeriodInt = parseIntegerQuietly(sourcePeriod);
            Integer targetPeriodInt = parseIntegerQuietly(targetPeriod);

            Object sourcePeriodId = resolvePeriodId(sourcePeriod, sourcePeriodInt, periodDbName, periodCollection, dbName);
            Object targetPeriodId = resolvePeriodId(targetPeriod, targetPeriodInt, periodDbName, periodCollection, dbName);

            if (sourcePeriodId == null) {
                String errMsg = "Kaynak dönem '" + sourcePeriod + "' için döneme ait ID (" + periodCollection + " koleksiyonunda) bulunamadı.";
                logger.error(errMsg);
                addFacesMessage(context, FacesMessage.SEVERITY_ERROR, "Hata", errMsg);
                throw new IllegalStateException(errMsg);
            }

            if (targetPeriodId == null) {
                String errMsg = "Hedef dönem '" + targetPeriod + "' için döneme ait ID (" + periodCollection + " koleksiyonunda) bulunamadı.";
                logger.error(errMsg);
                addFacesMessage(context, FacesMessage.SEVERITY_ERROR, "Hata", errMsg);
                throw new IllegalStateException(errMsg);
            }

            logger.info("Period lookup successful - sourcePeriodId: {}, targetPeriodId: {}", sourcePeriodId, targetPeriodId);

            List<Document> sourceDocuments = mongoDbUtil.find(dbName, targetCollection, new Document("period", sourcePeriodId));
            if ((sourceDocuments == null || sourceDocuments.isEmpty()) && sourcePeriodId != null) {
                sourceDocuments = mongoDbUtil.find(dbName, targetCollection, new Document("period", sourcePeriod));
            }

            if (sourceDocuments == null || sourceDocuments.isEmpty()) {
                addFacesMessage(context, FacesMessage.SEVERITY_WARN, "Uyarı", "Kaynak dönemde (" + sourcePeriod + ") kopyalanacak veri bulunamadı.");
                return;
            }

            String loginFkField = myForm.getLoginFkField();
            if (loginFkField == null || loginFkField.trim().isEmpty()) {
                loginFkField = "member";
            }

            List<MyField> refFields = resolveReferenceFields(myForm, periodCollection, targetCollection, loginFkField);

            int count = 0;
            int skippedCount = 0;

            for (Document doc : sourceDocuments) {
                Object memberId = doc.get(loginFkField);

                if (!isMemberActive(memberId)) {
                    logger.warn("Skipping copy for document id {} because member {} status.code is not '001' in uysdb.common", doc.get("_id"), memberId);
                    skippedCount++;
                    continue;
                }

                Document newDoc = new Document(doc);
                newDoc.remove("_id");
                boolean validRef = true;

                for (MyField refField : refFields) {
                    if (!processReferenceField(doc, refField, newDoc, dbName, memberId,
                            sourcePeriodId, sourcePeriodInt, sourcePeriod,
                            targetPeriodId, targetPeriodInt, targetPeriod, loginFkField)) {
                        validRef = false;
                        break;
                    }
                }

                if (!validRef) {
                    skippedCount++;
                    continue;
                }

                newDoc.put("period", targetPeriodId);
                mongoDbUtil.insertOne(dbName, targetCollection, newDoc);
                count++;
            }

            String msgDetail = targetCollection + " koleksiyonu için " + count + " adet veri (" + sourcePeriod + " -> " + targetPeriod + ") kopyalandı.";
            if (skippedCount > 0) {
                msgDetail += " (" + skippedCount + " adet kayıt üye durumu aktif olmadığı veya ilişkili alan verisi hedef dönemde bulunamadığı için kopyalanmadı)";
            }

            addFacesMessage(context, FacesMessage.SEVERITY_INFO, "Başarılı", msgDetail);
            logger.info("Successfully bulk copied {} documents (skipped {}) in collection {} from {} to {}",
                    count, skippedCount, targetCollection, sourcePeriod, targetPeriod);

        } catch (Exception e) {
            logger.error("Error during bulk copy execution for collection {}: {}", targetCollection, e.getMessage(), e);
            addFacesMessage(context, FacesMessage.SEVERITY_ERROR, "Sistem Hatası", "Kopyalama esnasında hata oluştu: " + e.getMessage());
        }
    }

    private Object resolvePeriodId(String periodStr, Integer periodInt, String periodDbName, String periodCollection, String defaultDbName) {
        Document periodDoc = null;
        if (periodInt != null) {
            periodDoc = mongoDbUtil.findOne(periodDbName, periodCollection, new Document("value", periodInt));
        }
        if (periodDoc == null) {
            periodDoc = mongoDbUtil.findOne(periodDbName, periodCollection, new Document("value", periodStr));
        }
        if (periodDoc == null && !periodDbName.equals(defaultDbName)) {
            periodDoc = mongoDbUtil.findOne(defaultDbName, periodCollection, new Document("value", periodInt != null ? periodInt : periodStr));
        }
        return periodDoc != null ? periodDoc.get("_id") : null;
    }

    private boolean isMemberActive(Object memberId) {
        if (memberId == null) {
            return true;
        }
        Document activeMemberDoc = mongoDbUtil.findOne("uysdb", "common",
                new Document("_id", memberId).append("status.code", "001"));
        return activeMemberDoc != null;
    }

    private List<MyField> resolveReferenceFields(FmsForm myForm, String periodCollection, String targetCollection, String loginFkField) {
        List<MyField> refFields = new ArrayList<>();
        if (myForm == null || myForm.getFields() == null) {
            return refFields;
        }
        for (MyField field : myForm.getFields().values()) {
            if (field == null || field.getKey() == null) {
                continue;
            }
            String fKey = field.getKey();
            if (fKey.equalsIgnoreCase("period") || fKey.equalsIgnoreCase(loginFkField)) {
                continue;
            }
            MyItems items = field.getEditItemsAsMyItems();
            if (items != null && items.getTable() != null && !items.getTable().trim().isEmpty()) {
                String refTable = items.getTable();
                if (!refTable.equalsIgnoreCase(periodCollection) && !refTable.equalsIgnoreCase(targetCollection)) {
                    refFields.add(field);
                }
            }
        }
        return refFields;
    }

    private Document findDocumentWithPeriodFallback(String db, String collection, Document baseFilter, Object periodId, Integer periodInt, String periodStr) {
        Document filter = new Document(baseFilter);
        filter.put("period", periodId);
        Document doc = mongoDbUtil.findOne(db, collection, filter);

        if (doc == null && periodInt != null) {
            Document altFilter = new Document(baseFilter);
            altFilter.put("period", periodInt);
            doc = mongoDbUtil.findOne(db, collection, altFilter);
        }
        if (doc == null && periodStr != null) {
            Document altFilter = new Document(baseFilter);
            altFilter.put("period", periodStr);
            doc = mongoDbUtil.findOne(db, collection, altFilter);
        }
        return doc;
    }

    private boolean processReferenceField(Document doc, MyField refField, Document newDoc, String dbName,
                                         Object memberId, Object sourcePeriodId, Integer sourcePeriodInt, String sourcePeriod,
                                         Object targetPeriodId, Integer targetPeriodInt, String targetPeriod,
                                         String loginFkField) {
        String fieldKey = refField.getKey();
        Object fieldValue = doc.get(fieldKey);
        if (fieldValue == null && refField.getField() != null) {
            fieldValue = doc.get(refField.getField());
        }

        if (fieldValue == null) {
            return true; // Optional field, skip check
        }

        MyItems items = refField.getEditItemsAsMyItems();
        String refTable = items.getTable();
        String refDb = items.getDb() != null ? items.getDb() : dbName;

        // Query source reference document matching member and source period
        Document baseSourceFilter = new Document("_id", fieldValue);
        if (memberId != null) {
            baseSourceFilter.append(loginFkField, memberId);
        }
        Document sourceRefDoc = findDocumentWithPeriodFallback(refDb, refTable, baseSourceFilter, sourcePeriodId, sourcePeriodInt, sourcePeriod);

        // Fallback: Query by _id alone for static lookup tables
        if (sourceRefDoc == null) {
            Document staticQuery = new Document("_id", fieldValue);
            sourceRefDoc = mongoDbUtil.findOne(refDb, refTable, staticQuery);
            if (sourceRefDoc == null && fieldValue instanceof String && ObjectId.isValid((String) fieldValue)) {
                staticQuery = new Document("_id", new ObjectId((String) fieldValue));
                sourceRefDoc = mongoDbUtil.findOne(refDb, refTable, staticQuery);
            }
        }

        if (sourceRefDoc != null && sourceRefDoc.get("period") != null) {
            // Period-dependent table: find corresponding target period record
            Document targetFilter = new Document();
            if (memberId != null) {
                targetFilter.append(loginFkField, memberId);
            }

            // Copy business key constraint if available
            if (sourceRefDoc.get("code") != null) {
                targetFilter.append("code", sourceRefDoc.get("code"));
            } else if (sourceRefDoc.get("key") != null) {
                targetFilter.append("key", sourceRefDoc.get("key"));
            } else if (sourceRefDoc.get("name") != null) {
                targetFilter.append("name", sourceRefDoc.get("name"));
            }

            Document targetRefDoc = findDocumentWithPeriodFallback(refDb, refTable, targetFilter, targetPeriodId, targetPeriodInt, targetPeriod);

            if (targetRefDoc != null) {
                if (targetRefDoc.get("_id") != null) {
                    newDoc.put(fieldKey, targetRefDoc.get("_id"));
                }
                return true;
            } else {
                logger.warn("Skipping copy for document id {} because referenced field '{}' in table {} (db: {}) has no entry for member {} in target period {}",
                        doc.get("_id"), fieldKey, refTable, refDb, memberId, targetPeriod);
                return false;
            }
        } else if (sourceRefDoc == null) {
            // Target period direct check
            Document targetFilter = new Document();
            if (memberId != null) {
                targetFilter.append(loginFkField, memberId);
            }
            Document targetRefDoc = findDocumentWithPeriodFallback(refDb, refTable, targetFilter, targetPeriodId, targetPeriodInt, targetPeriod);
            if (targetRefDoc != null && targetRefDoc.get("_id") != null) {
                newDoc.put(fieldKey, targetRefDoc.get("_id"));
                return true;
            } else {
                logger.warn("Skipping copy for document id {} because referenced field '{}' value {} does not exist in table {} (db: {})",
                        doc.get("_id"), fieldKey, fieldValue, refTable, refDb);
                return false;
            }
        }

        // Static lookup table without period field: reference is valid
        return true;
    }

    private Integer parseIntegerQuietly(String str) {
        if (str == null) {
            return null;
        }
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            logger.warn("Could not parse string to int: {}", str);
            return null;
        }
    }

    private void addFacesMessage(FacesContext context, FacesMessage.Severity severity, String summary, String detail) {
        if (context != null) {
            context.addMessage(null, new FacesMessage(severity, summary, detail));
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