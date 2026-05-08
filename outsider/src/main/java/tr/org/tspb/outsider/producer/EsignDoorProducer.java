package tr.org.tspb.outsider.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import tr.org.tspb.outsider.intr.EsignDoor;
import tr.org.tspb.outsider.qualifier.KamuSmEsignDoor;

@ApplicationScoped
public class EsignDoorProducer {

    @Inject
    @KamuSmEsignDoor // Change this to one of [@KamuSmEsignDoor,@OyasEsignDoor,@Default] to switch globally
    private Instance<EsignDoor> esignDoor;

    @Produces
    @SessionScoped
    public EsignDoor produceEsignDoor() {
        return esignDoor.get();
    }
}
