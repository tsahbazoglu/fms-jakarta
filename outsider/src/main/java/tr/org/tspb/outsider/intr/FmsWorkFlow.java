package tr.org.tspb.outsider.intr;

import java.io.Serializable;
import org.bson.Document;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.constants.exceptions.FormConfigException;

/**
 *
 * @author Telman Şahbazoğlu
 */
public interface FmsWorkFlow extends Serializable {

    public void init(FmsForm selectedForm, MyMap crudObject,
            Document searchObject) throws FormConfigException;

    public void reset();

}
