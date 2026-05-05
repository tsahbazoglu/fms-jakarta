package tr.org.tspb.outsider.impl;

import jakarta.enterprise.context.SessionScoped;
import tr.org.tspb.outsider.PaymentDoor;
import tr.org.tspb.outsider.qualifier.DefaultPaymentDoor;

/**
 *
 * @author Telman Şahbazoğlu
 */
@SessionScoped
@DefaultPaymentDoor
public class PaymentDoorDefaultImpl implements PaymentDoor {

    @Override
    public void newOrder(String amount) {

    }

}
