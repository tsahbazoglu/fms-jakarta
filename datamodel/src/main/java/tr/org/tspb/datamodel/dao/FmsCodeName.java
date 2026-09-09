package tr.org.tspb.datamodel.dao;

import java.util.Map;

import tr.org.tspb.datamodel.gui.DynamicTranslator;

/**
 *
 * @author Telman Şahbazoğlu
 */
public class FmsCodeName {

    private final String code;
    private final String name;

    public FmsCodeName(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public FmsCodeName(Map map) {
        this.code = map.get("code").
                toString();
        this.name = map.get("name").
                toString();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return DynamicTranslator.translate(name);
    }

    public String getRawName() {
        return name;
    }

}
