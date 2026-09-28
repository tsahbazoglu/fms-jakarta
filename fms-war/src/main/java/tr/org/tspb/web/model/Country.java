package tr.org.tspb.web.model;

public class Country {
    private String name;
    private String code;   // e.g. "+994"
    private String flag;   // e.g. "🇦🇿"
    private String mask;   // e.g. "(99) 999-99-99"

    public Country(String name, String code, String flag, String mask) {
        this.name = name;
        this.code = code;
        this.flag = flag;
        this.mask = mask;
    }

    // Getters and Setters
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getFlag() { return flag; }
    public String getMask() { return mask; }
    
    public String getDisplayLabel() {
        return flag + " " + name + " (" + code + ")";
    }
}