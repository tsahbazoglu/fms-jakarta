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

    /** Formats raw digits into this country's mask (used when re-rendering a stored value). */
    public String applyMask(String digits) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (char m : mask.toCharArray()) {
            if (i >= digits.length()) break;
            if (m == '9' || m == 'a' || m == '*') {
                sb.append(digits.charAt(i++));
            } else {
                sb.append(m);                                   // literal
                if (Character.isDigit(m) && digits.charAt(i) == m) {
                    i++;                                        // literal digit, e.g. the 5 in "(599)"
                }
            }
        }
        return sb.toString();
    }


}