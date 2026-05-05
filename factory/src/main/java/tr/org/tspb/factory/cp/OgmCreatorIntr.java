package tr.org.tspb.factory.cp;

import java.io.Serializable;
import tr.org.tspb.constants.exceptions.MongoOrmFailedException;
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.datamodel.dao.MyField;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.dao.MyProject;
import tr.org.tspb.datamodel.pojo.RoleMap;
import java.util.Map;
import org.bson.Document;
import tr.org.tspb.datamodel.dao.MyActions;
import tr.org.tspb.datamodel.dao.MyMap;
import tr.org.tspb.datamodel.dao.TagLogin;
import tr.org.tspb.datamodel.expected.FmsScriptRunner;
import tr.org.tspb.constants.exceptions.FormConfigException;
import tr.org.tspb.datamodel.pojo.UserDetail;

/**
 *
 * @author Telman Şahbazoğlu
 */
public interface OgmCreatorIntr extends Serializable {

    MyProject getMyProject(Document dbo, TagLogin tagLogin) throws
            NullNotExpectedException, FormConfigException;

    MyProject getMyProject(String projectKey, TagLogin login) throws
            NullNotExpectedException, FormConfigException;

    FmsForm getMyFormExternal(MyProject myProject, String configCollection,
            Map formSearch, Map searchObject,
            RoleMap loginController, UserDetail userDetail)
            throws NullNotExpectedException, MongoOrmFailedException;

    FmsForm getMyFormXsmall(MyProject myProject, Map searchObject,
            RoleMap loginController, UserDetail userDetail)
            throws NullNotExpectedException, MongoOrmFailedException;

    FmsForm getMyFormSmall(MyProject myProject, Document dboForm,
            Map searchObject,
            RoleMap loginController, UserDetail userDetail)
            throws NullNotExpectedException, MongoOrmFailedException;

    FmsForm getMyFormMedium(MyProject myProject, Document dboForm,
            Map searchObject,
            RoleMap loginController, UserDetail userDetail)
            throws NullNotExpectedException, MongoOrmFailedException;

    FmsForm getMyFormLarge(MyProject myProject, String configCollection,
            Map formSearch, Map searchObject,
            RoleMap loginController, UserDetail userDetail)
            throws NullNotExpectedException, MongoOrmFailedException;

    MyField getMyField(FmsForm myForm, Document docField, Map searchObject,
            RoleMap roleMap, UserDetail userDetail) throws FormConfigException;

    MyField getMyFieldPivot(FmsForm myForm, Document docField, Map searchObject,
            RoleMap roleMap, UserDetail userDetail) throws FormConfigException;

    public MyMap getCrudObject();

    public MyActions getMyActions(FmsForm myFormLarge, RoleMap roleMap,
            Document filter, UserDetail userDetail);

    public FmsScriptRunner getFmsScriptRunner();

}
