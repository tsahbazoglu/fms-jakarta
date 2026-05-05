package tr.org.tspb.datamodel.dao;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.bson.Document;

/**
 *
 * @author Telman Şahbazoğlu
 *
 */
public class MyFile implements FmsFile {

    private final String id;
    private final String mimeType;
    private final String hash;
    private final String hashFile;
    private final String name;
    private final String content;
    private final Document metadata;
    private final String uploadDateAsString;
    private byte[] bytes;
    private final InputStream inputStream;

    public static final DateTimeFormatter FORMATTER_1 = DateTimeFormatter.
            ofPattern("dd/MM/yyyy HH:mm:ss").
            withZone(ZoneId.systemDefault());

    public MyFile(GridFSBucket gridFSBucket, GridFSFile gridFSDBFile) throws
            IOException {
        this.id = gridFSDBFile.getObjectId().
                toHexString();

        this.metadata = gridFSDBFile.getMetadata() != null ? gridFSDBFile.
                getMetadata() : new Document();

        // In modern drivers, contentType is usually a field in metadata or retrieved via specific logic
        this.mimeType = metadata.getString("contentType");

        // 3. Handling the Hash (MD5)
        // NOTE: gridFSDBFile.getMD5() is REMOVED in 5.x. 
        // If you need the MD5, you must calculate it from the stream or store it in metadata manually.
        this.hash = metadata.getString("md5");

        this.hashFile = "UYS_SHA256";
        this.name = gridFSDBFile.getFilename();

        this.inputStream = gridFSBucket.openDownloadStream(gridFSDBFile.
                getObjectId());

        // 5. Content Calculation
        // Warning: Reading the stream here consumes it. 
        // If you need 'inputStream' later, you should reset it or re-open it.
        try (InputStream tempStream = gridFSBucket.openDownloadStream(
                gridFSDBFile.getObjectId())) {
            this.content = DigestUtils.sha256Hex(tempStream);
        }

        // 6. Date formatting using java.time
        this.uploadDateAsString = gridFSDBFile.getUploadDate() == null
                ? null
                : FORMATTER_1.format(gridFSDBFile.getUploadDate().
                        toInstant());

    }

    public MyFile withBytes() throws IOException {
        this.bytes = IOUtils.toByteArray(this.inputStream);
        return this;
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

    public byte[] getBytes() {
        return bytes;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

}
