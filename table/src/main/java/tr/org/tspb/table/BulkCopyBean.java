package tr.org.tspb.table;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class BulkCopyBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sourcePeriod;
    private String targetPeriod;
    private List<PeriodItem> periodList;

    @PostConstruct
    public void init() {
        periodList = new ArrayList<>();
        periodList.add(new PeriodItem("2026 / 1. Çeyrek (2026-03)", "202603"));
        periodList.add(new PeriodItem("2026 / 2. Çeyrek (2026-06)", "202606"));
        periodList.add(new PeriodItem("2026 / 3. Çeyrek (2026-09)", "202609"));
        periodList.add(new PeriodItem("2026 / 4. Çeyrek (2026-12)", "202612"));
    }

    public void copyData() {
        if (sourcePeriod != null && sourcePeriod.equals(targetPeriod)) {
            // Add faces error message: Source and target cannot be identical
            return;
        }
        // Bulk copy logic here
    }

    // Getters and Setters
    public String getSourcePeriod() {
        return sourcePeriod;
    }

    public void setSourcePeriod(String sourcePeriod) {
        this.sourcePeriod = sourcePeriod;
    }

    public String getTargetPeriod() {
        return targetPeriod;
    }

    public void setTargetPeriod(String targetPeriod) {
        this.targetPeriod = targetPeriod;
    }

    public List<PeriodItem> getPeriodList() {
        return periodList;
    }

    public void setPeriodList(List<PeriodItem> periodList) {
        this.periodList = periodList;
    }

    // Nested Model Class
    public static class PeriodItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String label;
        private String value;

        public PeriodItem(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}