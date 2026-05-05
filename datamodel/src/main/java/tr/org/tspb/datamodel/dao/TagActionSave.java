package tr.org.tspb.datamodel.dao;

import java.util.Map;
import org.bson.Document;
import tr.org.tspb.datamodel.expected.FmsScriptRunner;
import tr.org.tspb.datamodel.pojo.UserDetail;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class TagActionSave extends TagActionsAction {

    public TagActionSave(boolean enable, ActionEnableResult enableResult,
            Document eventAction, Document registredFunctions, Map myfilter,
            UserDetail userDetail, FmsScriptRunner fmsScriptRunner) {
        super(enable, enableResult, eventAction, registredFunctions, myfilter,
                userDetail, fmsScriptRunner);
    }

}
