package tr.org.tspb.table;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class Tedbir {

    private String code;
    private String name;
    private boolean caption;
    //
    private boolean check;
    private String kontrolTipi;
    private String uygulamaYonetimi;

    public Tedbir(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Tedbir(String code, String name, boolean caption) {
        this(code, name);
        this.caption = caption;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the caption
     */
    public boolean isCaption() {
        return caption;
    }

    /**
     * @param caption the caption to set
     */
    public void setCaption(boolean caption) {
        this.caption = caption;
    }

    /**
     * @return the check
     */
    public boolean isCheck() {
        return check;
    }

    /**
     * @param check the check to set
     */
    public void setCheck(boolean check) {
        this.check = check;
    }

    /**
     * @return the kontrolTipi
     */
    public String getKontrolTipi() {
        return kontrolTipi;
    }

    /**
     * @param kontrolTipi the kontrolTipi to set
     */
    public void setKontrolTipi(String kontrolTipi) {
        this.kontrolTipi = kontrolTipi;
    }

    /**
     * @return the uygulamaYonetimi
     */
    public String getUygulamaYonetimi() {
        return uygulamaYonetimi;
    }

    /**
     * @param uygulamaYonetimi the uygulamaYonetimi to set
     */
    public void setUygulamaYonetimi(String uygulamaYonetimi) {
        this.uygulamaYonetimi = uygulamaYonetimi;
    }

}
