package tr.org.tspb.web.session.mb;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * GridBean — Demo backing bean for DhGridComponent showcase.
 *
 * @author Telman Shahbazov / Dadhawk (with Google DeepMind Antigravity AI)
 */
@Named("gridBean")
@SessionScoped
public class GridBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer rowCount = 5;
    private Integer colCount = 4;
    private String[][] content;
    private Object captions;
    private Map<String, String> componentMap = new HashMap<>();
    private Object readOnlyCells;
    private Object cellStyles;
    private String selectedPreset = "financial";

    @PostConstruct
    public void init() {
        loadFinancialPreset();
    }

    public void loadFinancialPreset() {
        this.selectedPreset = "financial";
        this.rowCount = 5;
        this.colCount = 5;
        this.captions = new String[] {
            "Financial Performance / H1 (Q1-Q2) / Revenue ($)",
            "Financial Performance / H1 (Q1-Q2) / Expenses ($)",
            "Financial Performance / H2 (Q3-Q4) / Revenue ($)",
            "Financial Performance / H2 (Q3-Q4) / Expenses ($)",
            "Overall Status"
        };
        this.content = new String[][] {
            {"Quarter", "Revenue ($)", "Expenses ($)", "Margin (%)", "Performance"},
            {"Q1 2026", "$120,000", "$85,000", "29.1%", "Completed"},
            {"Q2 2026", "$145,000", "$92,000", "36.5%", "Active"},
            {"Q3 2026", "$160,000", "$98,000", "38.7%", "Pending"},
            {"Q4 2026", "$210,000", "$110,000", "47.6%", "In Review"}
        };

        this.componentMap = new HashMap<>();
        this.componentMap.put("r1_c4", "status-selector");
        this.componentMap.put("r2_c4", "status-selector");
        this.componentMap.put("r3_c4", "status-selector");
        this.componentMap.put("r4_c4", "status-selector");

        this.readOnlyCells = Map.of("r1_c0", true, "r3", true);
        this.cellStyles = Map.of(
            "r1_c3", "background-color: rgba(34, 197, 94, 0.15); color: #15803d; font-weight: 700;",
            "c1", "color: #0284c7; font-weight: 600;"
        );
    }

    public void loadTaskBoardPreset() {
        this.selectedPreset = "taskboard";
        this.rowCount = 5;
        this.colCount = 5;
        this.captions = null;
        this.content = new String[][] {
            {"Task Name", "Owner", "Category", "Priority", "Status"},
            {"Upgrade JSF Library", "Alex Rivera", "Core Dev", "High", "Active"},
            {"Design Web Component", "Sarah Chen", "UI/UX", "Medium", "Completed"},
            {"QA Matrix Test Suite", "Telman G.", "Testing", "High", "Pending"},
            {"CI/CD Pipeline Setup", "DevOps Team", "Infrastructure", "Low", "In Review"}
        };

        this.componentMap = new HashMap<>();
        this.componentMap.put("r1_c3", "priority-badge-editor");
        this.componentMap.put("r2_c3", "priority-badge-editor");
        this.componentMap.put("r3_c3", "priority-badge-editor");
        this.componentMap.put("r4_c3", "priority-badge-editor");
        this.componentMap.put("r1_c4", "status-selector");
        this.componentMap.put("r2_c4", "status-selector");
        this.componentMap.put("r3_c4", "status-selector");
        this.componentMap.put("r4_c4", "status-selector");
    }

    public void loadProductCatalogPreset() {
        this.selectedPreset = "catalog";
        this.rowCount = 5;
        this.colCount = 5;
        this.captions = null;
        this.content = new String[][] {
            {"SKU Code", "Product Name", "Category", "User Rating", "Availability"},
            {"SKU-9021", "Quantum Matrix Display", "Monitors", "★★★★★", "Active"},
            {"SKU-4412", "Cyber Grid Keyboard", "Peripherals", "★★★★☆", "Active"},
            {"SKU-1089", "Neuron Headset Pro", "Audio", "★★★☆☆", "Pending"},
            {"SKU-3320", "UltraDock Station", "Accessories", "★★★★★", "Completed"}
        };

        this.componentMap = new HashMap<>();
        this.componentMap.put("r1_c3", "rating-editor");
        this.componentMap.put("r2_c3", "rating-editor");
        this.componentMap.put("r3_c3", "rating-editor");
        this.componentMap.put("r4_c3", "rating-editor");
        this.componentMap.put("r1_c4", "status-selector");
        this.componentMap.put("r2_c4", "status-selector");
        this.componentMap.put("r3_c4", "status-selector");
        this.componentMap.put("r4_c4", "status-selector");
    }

    public void addRow() {
        int r = (this.content != null) ? this.content.length : 0;
        int c = (r > 0 && this.content[0] != null) ? this.content[0].length : 1;
        if (r == 0) {
            this.content = new String[][] {{"Row 1 Col 1"}};
            this.rowCount = 1;
            this.colCount = 1;
            return;
        }
        String[][] newContent = new String[r + 1][c];
        for (int i = 0; i < r; i++) {
            System.arraycopy(this.content[i], 0, newContent[i], 0, c);
        }
        for (int j = 0; j < c; j++) {
            newContent[r][j] = "Row " + (r + 1) + " Col " + (j + 1);
        }
        this.content = newContent;
        this.rowCount = r + 1;
        this.colCount = c;
    }

    public void addColumn() {
        int r = (this.content != null) ? this.content.length : 1;
        int c = (r > 0 && this.content[0] != null) ? this.content[0].length : 0;
        if (c == 0) {
            this.content = new String[][] {{"Row 1 Col 1"}};
            this.rowCount = 1;
            this.colCount = 1;
            return;
        }
        String[][] newContent = new String[r][c + 1];
        for (int i = 0; i < r; i++) {
            System.arraycopy(this.content[i], 0, newContent[i], 0, c);
            newContent[i][c] = "Col " + (c + 1);
        }
        this.content = newContent;
        this.rowCount = r;
        this.colCount = c + 1;
    }

    // Getters and Setters
    public Integer getRowCount() {
        if (content != null && content.length > 0) {
            return content.length;
        }
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Integer getColCount() {
        if (content != null && content.length > 0 && content[0] != null) {
            return content[0].length;
        }
        return colCount;
    }

    public void setColCount(Integer colCount) {
        this.colCount = colCount;
    }

    public String[][] getContent() {
        return content;
    }

    public void setContent(String[][] content) {
        this.content = content;
    }

    public Object getCaptions() {
        return captions;
    }

    public void setCaptions(Object captions) {
        this.captions = captions;
    }

    public Map<String, String> getComponentMap() {
        return componentMap;
    }

    public void setComponentMap(Map<String, String> componentMap) {
        this.componentMap = componentMap;
    }

    public Object getReadOnlyCells() {
        return readOnlyCells;
    }

    public void setReadOnlyCells(Object readOnlyCells) {
        this.readOnlyCells = readOnlyCells;
    }

    public Object getCellStyles() {
        return cellStyles;
    }

    public void setCellStyles(Object cellStyles) {
        this.cellStyles = cellStyles;
    }

    public String getSelectedPreset() {
        return selectedPreset;
    }

    public void setSelectedPreset(String selectedPreset) {
        this.selectedPreset = selectedPreset;
    }
}
