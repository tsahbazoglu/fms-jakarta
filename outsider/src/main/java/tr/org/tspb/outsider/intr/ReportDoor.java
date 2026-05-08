package tr.org.tspb.outsider.intr;

import java.io.Serializable;
import org.primefaces.model.StreamedContent;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.MyMap;

/**
 *
 * @author Telman Şahbazoğlu
 */
public interface ReportDoor extends Serializable {

    public StreamedContent getFile(FmsForm myForm, MyMap crud);

}
