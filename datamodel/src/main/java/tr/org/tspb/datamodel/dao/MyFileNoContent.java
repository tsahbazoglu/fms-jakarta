package tr.org.tspb.datamodel.dao;

import com.mongodb.client.gridfs.model.GridFSFile;
import java.io.IOException;
import org.bson.Document;
import static tr.org.tspb.constants.ProjectConstants.SIMPLE_DATE_FORMAT__1;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class MyFileNoContent implements FmsFile {

    private final String id;
    private final String mimeType;
    private final String hash;
    private final String hashFile;
    private final String name;
    private final String content;
    private final Document metadata;
    private final String uploadDateAsString;

    public MyFileNoContent(GridFSFile gridFSDBFile) throws IOException {
        this.id = gridFSDBFile.getObjectId().
                toHexString();
        this.metadata = gridFSDBFile.getMetadata();
        this.mimeType = metadata.getString("contentType");
        this.hash = metadata.getString("md5");
        this.hashFile = "UYS_SHA256";
        this.name = gridFSDBFile.getFilename();
        this.content = "no content";
        this.uploadDateAsString = gridFSDBFile.getUploadDate() == null ? null : SIMPLE_DATE_FORMAT__1.
                format(gridFSDBFile.getUploadDate());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public String getHashFile() {
        return hashFile;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public Document getMetadata() {
        return metadata;
    }

    @Override
    public String getUploadDateAsString() {
        return uploadDateAsString;
    }

}
