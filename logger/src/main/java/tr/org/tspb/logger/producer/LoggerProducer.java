package tr.org.tspb.logger.producer;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Telman Şahbazoğlu
 */
@Singleton
public class LoggerProducer {

    @Produces
    public Logger produceLogger(InjectionPoint injectionPoint) {

        Bean bean = injectionPoint.getBean();
        if (bean != null) {
            return LoggerFactory.getLogger(bean.getBeanClass());
        } else {
            //in case of njection from servlet 
            return LoggerFactory.getLogger(injectionPoint.getMember().
                    getDeclaringClass().
                    getName());
        }
    }
}
