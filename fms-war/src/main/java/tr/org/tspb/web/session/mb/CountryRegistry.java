package tr.org.tspb.web.session.mb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;
import tr.org.tspb.web.model.Country;

@Named
@ApplicationScoped
public class CountryRegistry {

    private final List<Country> countries;

    public CountryRegistry() {
        countries = new ArrayList<>();
        // Comprehensive list sample (expand or load from database/JSON config as needed)
        countries.add(new Country("Azerbaijan", "+994", "🇦🇿", "(99) 999-99-99"));
        countries.add(new Country("Turkey", "+90", "🇹🇷", "(599) 999-99-99"));
        countries.add(new Country("United States", "+1", "🇺🇸", "(999) 999-9999"));
        countries.add(new Country("United Kingdom", "+44", "🇬🇧", "9999 999999"));
        countries.add(new Country("Germany", "+49", "🇩🇪", "999 9999999"));
        countries.add(new Country("France", "+33", "🇫🇷", "9 99 99 99 99"));
        countries.add(new Country("Russia", "+7", "🇷🇺", "(999) 999-99-99"));
        countries.add(new Country("Georgia", "+995", "🇬🇪", "(999) 99-99-99"));
        // Add remaining international codes...
    }

    public List<Country> getCountries() {
        return countries;
    }
    public String getMask(String code) {
        if (code == null) {
            return "(99) 999-99-99"; // Fallback default
        }
        for (Country c : countries) {
            if (c.getCode().equals(code)) {
                return c.getMask();
            }
        }
        return "(99) 999-99-99";
    }

    public Country findByCode(String code) {
        for (Country c : countries) {
            if (c.getCode().equals(code)) return c;
        }
        return null;
    }

    /** Longest matching country-code prefix wins. */
    public Country findByE164(String number) {
        Country best = null;
        if (number == null) return null;
        for (Country c : countries) {
            if (number.startsWith(c.getCode())
                    && (best == null || c.getCode().length() > best.getCode().length())) {
                best = c;
            }
        }
        return best;
    }

    public Country getDefaultCountry() {
        return countries.get(0);
    }
}