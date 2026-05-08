package tr.org.tspb.outsider.intr;

/**
 *
 * @author Telman Şahbazoğlu
 */
public interface FmsEimzaAttachment {

    String getContent();

    String getFileName();

    String getHash();

    String getHashFile();

    String getId();

    String getMimeType();

    void setContent(String content);

    void setFileName(String fileName);

    void setHash(String hash);

    void setHashFile(String hashFile);

    void setId(String id);

    void setMimeType(String mimeType);

}
