package tr.org.tspb.table;

import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class Risk {

    private ObjectId objectId;
    private boolean exist;
    private String code;
    private String name;
    private boolean caption;
    private List<Tedbir> tedbirs;

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    public List<Tedbir> getTedbirs() {
        return tedbirs;
    }

    public void setTedbirs(List<Tedbir> tedbirs) {
        this.tedbirs = tedbirs;
    }

    public Risk(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Risk(String code, String name, boolean caption) {
        this(code, name);
        this.caption = caption;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the caption
     */
    public boolean isCaption() {
        return caption;
    }

    /**
     * @param caption the caption to set
     */
    public void setCaption(boolean caption) {
        this.caption = caption;
    }

    public boolean isExist() {
        return exist;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    Document toDocument() {

        Document doc = new Document();

        List<Bson> tedbirDocs = new ArrayList<>();
        for (Tedbir tedbir : tedbirs) {
            Bson filter = and(eq(FIELD_TEDBIR_CODE, tedbir.getCode()),
                    eq(FIELD_KONTROL, tedbir.getKontrolTipi()),
                    eq(FIELD_UYGULAMA, tedbir.getUygulamaYonetimi()),
                    eq(FIELD_CHECK, tedbir.isCheck()));
            tedbirDocs.add(filter);
        }

        if (this.objectId != null) {
            doc.append(_ID, this.objectId);
        }
        doc.append(FIELD_RISK_CODE, this.code);
        doc.append(TABLE_TEDBIRS, tedbirDocs);

        return doc;
    }

    void initDataFromDB(Document doc) {

        if (doc == null) {
            return;
        }

        List<Document> tedbirler = doc.getList(TABLE_TEDBIRS, Document.class);

        for (Tedbir tedbir : tedbirs) {
            for (Document document : tedbirler) {
                if (tedbir.getCode().
                        equals(document.getString(FIELD_TEDBIR_CODE))) {
                    tedbir.setUygulamaYonetimi(document.
                            getString(FIELD_UYGULAMA));
                    tedbir.setCheck(document.getBoolean(FIELD_CHECK));
                    tedbir.setKontrolTipi(document.getString(FIELD_KONTROL));
                }
            }
        }

        this.objectId = doc.get(_ID, ObjectId.class);
    }

    private static final String _ID = "_id";
    private static final String FIELD_RISK_CODE = "risk-code";
    private static final String FIELD_TEDBIR_CODE = "tedbir-code";
    private static final String FIELD_KONTROL = "kontrol";
    private static final String FIELD_CHECK = "check";
    private static final String FIELD_UYGULAMA = "uygulama";
    private static final String TABLE_TEDBIRS = "tedbirs";

    public void setExist(boolean exist) {
        this.exist = exist;
    }

}
