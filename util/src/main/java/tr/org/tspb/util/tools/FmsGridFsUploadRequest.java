package tr.org.tspb.util.tools;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class FmsGridFsUploadRequest {

    private final String dbName;
    private final String filename;
    private final byte[] content;
    private Document metadata = new Document();
    private ObjectId id;
    private final int chunkSize = 1048576;

    public String getDbName() {
        return dbName;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getContent() {
        return content;
    }

    public Document getMetadata() {
        return metadata;
    }

    public ObjectId getId() {
        return id;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public FmsGridFsUploadRequest(String dbName, String filename, byte[] content) {
        if (dbName == null || dbName.isBlank()) {
            throw new IllegalArgumentException("Missing dbName");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Missing filename");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Empty content");
        }
        this.filename = filename;
        this.content = content;
        this.dbName = dbName;
    }

    public FmsGridFsUploadRequest withMetadata(Document metadata) {
        this.metadata = metadata != null ? metadata : new Document();
        return this;
    }

    public FmsGridFsUploadRequest withId(ObjectId id) {
        this.id = id;
        return this;
    }

}
