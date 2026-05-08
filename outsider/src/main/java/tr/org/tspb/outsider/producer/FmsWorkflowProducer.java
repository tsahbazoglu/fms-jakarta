package tr.org.tspb.outsider.producer;

import java.io.Serializable;
//
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
//
import tr.org.tspb.outsider.impl.FmsWorkflowDefaultImpl;
import tr.org.tspb.outsider.intr.FmsWorkFlow;

/**
 *
 * @author Telman Şahbazoğlu
 */
@SessionScoped
public class FmsWorkflowProducer implements Serializable {

    /**
     *
     * @return
     */
    @Produces
    public FmsWorkFlow getFmsWorkFlow() {
        return new FmsWorkflowDefaultImpl();
    }

}
