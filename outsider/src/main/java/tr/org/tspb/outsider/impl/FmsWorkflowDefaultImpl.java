package tr.org.tspb.outsider.impl;

import org.bson.Document;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.outsider.FmsWorkFlow;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class FmsWorkflowDefaultImpl implements FmsWorkFlow {

    @Override
    public void init(FmsForm selectedForm, MyMap crudObject,
            Document searchObject) {
        //
    }

    @Override
    public void reset() {
        //
    }

}
