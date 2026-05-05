package tr.org.tspb.util.stereotype;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;

/**
 *
 * @author Telman Şahbazoğlu
 */
@Named
@SessionScoped
@Stereotype
@Target(TYPE)
@Retention(RUNTIME)
public @interface MyController {

}
