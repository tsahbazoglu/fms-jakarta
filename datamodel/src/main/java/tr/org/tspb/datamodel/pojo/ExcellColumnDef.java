package tr.org.tspb.datamodel.pojo;

import org.bson.Document;
import tr.org.tspb.datamodel.dao.MyField;
import tr.org.tspb.datamodel.dao.FmsForm;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class ExcellColumnDef {

    private final String type;
    private final boolean cache;
    private final int cellType;
    private final FmsWorkbookColumnConverter converter;
    private final MyField toMyField;

    public ExcellColumnDef(FmsForm myForm, Document dbo) {
        this.type = dbo.getString("type");
        this.cache = Boolean.TRUE.equals(dbo.get("cache"));
        this.cellType = resolveCellType();
        this.converter = initConverterFromDocument(dbo);

        String toFieldKey = dbo.getString("to");

        toMyField = myForm.getField(toFieldKey);
        if (toMyField == null) {
            throw new RuntimeException(String.format(
                    "form fields does not contain key %s mentioned in upload-config property",
                    toFieldKey));
        }
    }

    private FmsWorkbookColumnConverter initConverterFromDocument(Document fielDocument) {
        Document converterDbo = fielDocument.get("converter", Document.class);

        if (converterDbo == null) {
            return null;
        }

        return new FmsWorkbookColumnConverter.Builder()
                .type(converterDbo.getString("type"))
                .uri(converterDbo.getString("uri"))
                .op(converterDbo.getString("op"))
                .build();
    }

    public FmsWorkbookColumnConverter getConverter() {
        return converter;
    }

    public MyField getToMyField() {
        return toMyField;
    }

    public String getType() {
        return type;
    }

    public boolean isCache() {
        return cache;
    }

    public int getCellType() {
        return cellType;
    }

    private int resolveCellType() {
        switch (type) {
            case "CELL_TYPE_STRING":
                return 1;//apachi poi Cell.CELL_TYPE_STRING;
            case "CELL_TYPE_NUMERIC":
                return 0;//Cell.CELL_TYPE_NUMERIC;
            case "CELL_TYPE_DATE":
                return 0;//Cell.CELL_TYPE_NUMERIC;
        }
        return 0;//Cell.CELL_TYPE_NUMERIC;
    }

    @Override
    public String toString() {
        return this.toMyField.getKey();
    }

}
