package tr.org.tspb.util.tools;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import tr.org.tspb.datamodel.dao.FmsFile;
import tr.org.tspb.datamodel.dao.MyFile;

/**
 *
 * @author Telman Şahbazoğlu
 */
public interface FmsMongoGridFsIntr extends Serializable {

    public MyFile findFileAsMyFile(String db, ObjectId objectId) throws
            IOException;

    public MyFile findFileAsMyFileInputStream(String db, ObjectId objectId)
            throws IOException;

    public List<MyFile> findFilesAsMyFile(String db, DBObject filter) throws
            IOException;

    public GridFSFile findFile(String db, Bson filter) throws RuntimeException;

    public GridFSFile findFile(String db, ObjectId objectId) throws
            RuntimeException;

    public List<MyFile> findFileList(String db, BasicDBObject basicDBObject,
            int skip, int limit);

    public List<FmsFile> findFilesAsFmsFileNoContent(String db,
            BasicDBObject basicDBObject, int skip, int limit);

    public List<GridFSFile> findFiles(String db, Bson filter);

    public List<GridFSFile> findFiles(String db, String filename);

    public int countFile(String db, Bson basicDBObject);

    public ObjectId createFile(String gridFsDbName, String filename,
            File file, Document externalMetadata) throws IOException;

    public ObjectId createFile(String gridFsDbName, String filename,
            InputStream inputStream, Document externalMetadata);

    public ObjectId createFile(FmsGridFsUploadRequest fmsGridFsUploadRequest);

    public void removeFile(String db, Bson filter) throws RuntimeException;

    public void removeFile(String db, ObjectId objectId) throws RuntimeException;

    public void copyFiles(String fromDb, String toDb, Bson fromSearch) throws
            IOException;

}
