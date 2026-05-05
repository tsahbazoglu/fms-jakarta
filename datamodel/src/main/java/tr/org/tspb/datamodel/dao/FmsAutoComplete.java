package tr.org.tspb.datamodel.dao;

import java.util.List;
import java.util.Map;
import jakarta.faces.model.SelectItem;
import org.bson.Document;
import tr.org.tspb.datamodel.dao.refs.PlainRecord;
import tr.org.tspb.datamodel.pojo.RoleMap;

/**
 *
 * @author Telman Şahbazoğlu
 */
public interface FmsAutoComplete {

    public List<PlainRecord> completeMethod(Document query);

    public List<SelectItem> createSelectItems(Map searchObject, MyMap crudObject);

    public List<SelectItem> createSelectItemsHistory(Map searchObject,
            MyMap crudObject);

    public Map<String, Object> getFilter();

    public RoleMap getRoleMap();

}
