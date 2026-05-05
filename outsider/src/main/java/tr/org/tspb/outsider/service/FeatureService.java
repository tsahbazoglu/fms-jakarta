package tr.org.tspb.outsider.service;

import java.io.Serializable;
//
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
//
import tr.org.tspb.outsider.EsignDoor;
import tr.org.tspb.outsider.qualifier.OyasEsignDoor;

/**
 *
 * @author Telman Şahbazoğlu
 */
@Named
@SessionScoped
public class FeatureService implements Serializable {

    @Inject
    //@DefaultEsignDoor
    @OyasEsignDoor
    private EsignDoor esignDoor;

    public EsignDoor getEsignDoor() {
        return esignDoor;
    }

}
