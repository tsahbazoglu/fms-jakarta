package tr.org.tspb.outsider.service;

import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import tr.org.tspb.outsider.EsignDoor;

/**
 *
 * @author Telman Şahbazoğlu
 */
@Named
@SessionScoped
public class FeatureService implements Serializable {

    @Inject
    private EsignDoor esignDoor;

    public EsignDoor getEsignDoor() {
        return esignDoor;
    }

}
