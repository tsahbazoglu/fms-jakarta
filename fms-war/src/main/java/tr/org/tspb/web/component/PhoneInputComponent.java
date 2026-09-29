package tr.org.tspb.web.component;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.context.FacesContext;
import java.io.IOException;
import tr.org.tspb.web.model.Country;
import tr.org.tspb.web.session.mb.CountryRegistry;

@FacesComponent("phoneInput")
public class PhoneInputComponent extends UIInput implements NamingContainer {

    @Override
    public String getFamily() {
        return UINamingContainer.COMPONENT_FAMILY;
    }

    private UIInput countryInput()    { return (UIInput) findComponent("countryCode"); }
    private UIInput subscriberInput() { return (UIInput) findComponent("subscriber"); }

    private CountryRegistry registry(FacesContext ctx) {
        return ctx.getApplication().evaluateExpressionGet(ctx, "#{countryRegistry}", CountryRegistry.class);
    }

    /** Used by the mask EL in the composite: mask follows the currently selected country. */
    public String getCurrentMask() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Object code = countryInput().getSubmittedValue() != null
                ? countryInput().getSubmittedValue()
                : countryInput().getValue();
        return registry(ctx).getMask(code == null ? null : code.toString());
    }

    /** Runs after the children are decoded: merge country code + digits into ONE submitted value. */
    @Override
    public void decode(FacesContext context) {
        UIInput ci = countryInput();
        UIInput si = subscriberInput();
        Object c = ci.getSubmittedValue();
        Object s = si.getSubmittedValue();

        if (c == null && s == null) {
            setSubmittedValue(null);
            return;
        }
        Object code = c != null ? c : ci.getValue();
        String digits = s == null ? "" : s.toString().replaceAll("\\D", "");
        setSubmittedValue(digits.isEmpty() || code == null ? "" : code.toString() + digits);
    }

    @Override
    protected Object getConvertedValue(FacesContext context, Object submittedValue) {
        return submittedValue; // plain String
    }

    /** Split the model value (e.g. +994501234567) back into country + masked number. */
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        CountryRegistry reg = registry(context);
        UIInput ci = countryInput();
        UIInput si = subscriberInput();

        Object v = getSubmittedValue() != null ? getSubmittedValue() : getValue();
        String full = v == null ? "" : v.toString();

        Country country = full.isEmpty() ? null : reg.findByE164(full);

        if (country != null) {
            String digits = full.substring(country.getCode().length());
            ci.setValue(country.getCode());
            si.setValue(country.applyMask(digits));
        } else {
            // empty value: keep the user's country selection if there is one, else use default
            if (ci.getValue() == null) {
                Object def = getAttributes().get("defaultCode");
                Country dc = def != null ? reg.findByCode(def.toString()) : null;
                ci.setValue((dc != null ? dc : reg.getDefaultCountry()).getCode());
            }
            si.setValue(null);
        }
        ci.setSubmittedValue(null);
        si.setSubmittedValue(null);

        super.encodeBegin(context);
    }
}