package tr.org.tspb.outsider;

import jakarta.enterprise.context.SessionScoped;
import org.primefaces.model.StreamedContent;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.outsider.qualifier.DefaultReportDoor;

/**
 *
 * @author Telman Şahbazoğlu
 */
@SessionScoped
@DefaultReportDoor
public class ReportDoorDefaultImpl implements ReportDoor {

    @Override
    public StreamedContent getFile(FmsForm myForm, MyMap crud) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
