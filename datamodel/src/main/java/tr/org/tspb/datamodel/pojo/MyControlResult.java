package tr.org.tspb.datamodel.pojo;

import static tr.org.tspb.constants.ProjectConstants.EXPRESSION;
import static tr.org.tspb.constants.ProjectConstants.HATA_VAR;
import static tr.org.tspb.constants.ProjectConstants.RESULT;
import static tr.org.tspb.constants.ProjectConstants.TAMAM;
import java.util.Map;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MyControlResult {

    private final String expression;
    private final String controlResult;
    private final boolean result;
    private final Boolean proceed;

    public MyControlResult(Map map) {

        this.result = Boolean.TRUE.equals(map.get(RESULT));
        Object exprObj = map.get(EXPRESSION);
        if (exprObj == null) {
            exprObj = map.get("message");
        }
        this.expression = exprObj != null ? exprObj.toString() : "";

        Object pObj = map.get("proceed");
        if (pObj instanceof Boolean b) {
            this.proceed = b;
        } else if (pObj != null) {
            this.proceed = Boolean.parseBoolean(pObj.toString());
        } else {
            this.proceed = this.result;
        }

        if (result) {
            this.controlResult = TAMAM;
        } else {
            this.controlResult = HATA_VAR;
        }

    }

    public String getExpression() {
        return expression;
    }

    public String getControlResult() {
        return controlResult;
    }

    public boolean isResult() {
        return result;
    }

    public Boolean getProceed() {
        return proceed;
    }

    public boolean isProceed() {
        return proceed != null ? proceed : result;
    }

}
