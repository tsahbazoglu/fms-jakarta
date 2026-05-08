package tr.org.tspb.outsider.service;

import java.io.Serializable;
//
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
//
import tr.org.tspb.outsider.EsignDoor;
import tr.org.tspb.outsider.qualifier.KamuSmEsignDoor;

/**
 *
 * @author Telman Şahbazoğlu
 */
@Named
@SessionScoped
public class FeatureService implements Serializable {

    @Inject
    @KamuSmEsignDoor // Or [@OyasEsignDoor,@DefaultEsignDoor] depending on your toggle
    private EsignDoor esignDoor;

    public EsignDoor getEsignDoor() {
        return esignDoor;
    }

    public String getEsignIncludePath() {
        return esignDoor.getXhtmlPath();
    }

}
