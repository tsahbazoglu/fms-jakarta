package tr.org.tspb.outsider.producer;

import tr.org.tspb.outsider.intr.EsignControllerIntr;
import java.io.Serializable;
//
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.enterprise.inject.Produces;
import tr.org.tspb.outsider.intr.EsignDoor;
//
import tr.org.tspb.outsider.qualifier.KamuSmEsignDoor;

/**
 *
 * @author Telman Şahbazoğlu
 */
@SessionScoped
public class EsignProducer implements Serializable {

    @Inject
    @KamuSmEsignDoor // Change this to one of [@KamuSmEsignDoor,@OyasEsignDoor,@DefaultEsignDoor] to switch globally
    private Instance<EsignControllerIntr> esignController;

    @Inject
    @KamuSmEsignDoor // Change this to one of [@KamuSmEsignDoor,@OyasEsignDoor,@DefaultEsignDoor] to switch globally
    private Instance<EsignDoor> esignDoor;

    @Produces
    @Named("esignController")
    @SessionScoped
    public EsignControllerIntr produceActiveController() {
        return esignController.get();
    }

    @Produces
    @SessionScoped
    public EsignDoor produceEsignDoor() {
        return esignDoor.get();
    }

}
