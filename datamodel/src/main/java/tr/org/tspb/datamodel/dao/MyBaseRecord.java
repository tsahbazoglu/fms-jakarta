package tr.org.tspb.datamodel.dao;

import jakarta.faces.model.SelectItem;
import org.bson.Document;
import org.bson.types.ObjectId;
import tr.org.tspb.constants.ProjectConstants;
import tr.org.tspb.datamodel.dao.refs.PlainRecord;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MyBaseRecord extends PlainRecord {

    private ObjectId objectId;
    private String objectIdStr;
    private String name;

    public MyBaseRecord() {
        /*
        
        the default constructor is called by jsf framework when we try to submit non succeeded form more than once
        So the default constructor have to be defined to avoid no method match exception
       
        NEVER PUT ANY CODE HERE   

         */
    }

    public MyBaseRecord(SelectItem item) {
        this.objectId = (ObjectId) item.getValue();
        this.objectIdStr = objectId.toString();
        this.name = item.getLabel();
    }

    public MyBaseRecord(Document doc, MyItems myItem) {

        StringBuilder view = new StringBuilder();
        for (String viewKey : myItem.getView()) {
            view.append(doc.get(viewKey));
            view.append(" - ");
        }
        this.objectId = (ObjectId) doc.get(ProjectConstants.MONGO_ID);
        this.objectIdStr = objectId.toString();
        this.name = view.toString();
    }

    public MyBaseRecord(String objectIdAsString) {
        this.objectId = new ObjectId(objectIdAsString);
    }

    @Override
    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    @Override
    public String getObjectIdStr() {
        return objectIdStr;
    }

    public void setObjectIdStr(String objectIdStr) {
        this.objectIdStr = objectIdStr;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

}
