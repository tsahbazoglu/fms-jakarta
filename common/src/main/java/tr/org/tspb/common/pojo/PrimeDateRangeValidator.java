package tr.org.tspb.common.pojo;

import java.util.Date;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

/**
 *
 * @author Telman Şahbazoğlu
 */
@FacesValidator(value = "primeDateRangeValidator")
public class PrimeDateRangeValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {
        if (value == null) {
            return;
        }

        //Leave the null handling of startDate to required="true"
        Object dateRangeValidate = component.getAttributes().
                get("dateRangeValidate");
        if (Boolean.TRUE.equals(dateRangeValidate)) {
            Object startDateValue = component.getAttributes().
                    get("dateRangeStartValue");
            if (startDateValue instanceof Date) {
                Date startDate = (Date) startDateValue;
                Date endDate = (Date) value;
                if (endDate.before(startDate)) {
                    throw new ValidatorException(
                            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                    "Bitiş tarihi, başlangıç tarihinden büyük olmalıdır.",
                                    "*"));
                }
            }
        }
    }
}
