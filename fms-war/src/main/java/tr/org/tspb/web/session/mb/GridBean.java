package com.dadhawk.faces.demo;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import tr.org.tspb.common.qualifier.MyQualifier;
import tr.org.tspb.common.qualifier.ViewerController;
import tr.org.tspb.pivot.ctrl.PivotModifierCtrl;
import tr.org.tspb.pivot.datamodel.PivotDataModelHandson;
import tr.org.tspb.pivot.event.PivotDataModelChangeEvent;
import tr.org.tspb.service.FormService;

/**
 * GridBean — Demo backing bean for DhGridComponent showcase.
 *
 * @author Telman Shahbazov / Dadhawk (with Google DeepMind Antigravity AI)
 */
@Named("gridBean")
@SessionScoped
public class GridBean implements Serializable {

    @Inject
    @MyQualifier(myEnum = ViewerController.crudPivot)
    PivotModifierCtrl pivotModifierCtrl;

    @Inject
    protected FormService formService;

    private static final long serialVersionUID = 1L;

    private Integer rowCount = 5;
    private Integer colCount = 4;
    private String[][] content;
    private Object captions;
    private Map<String, String> componentMap = new HashMap<>();
    private Boolean readOnly = false;
    private Object readOnlyCells;
    private Object cellStyles;
    private String selectedPreset = "financial";
    private String cssCompatible = "primethemes";
    private String locale = "en-US";

    private Map<String, String> componentMatch = Map.of(
            "inputText", "input-number",
            "inputNumber", "input-number"
    );

    @PostConstruct
    public void init() {
        loremIpsumInit();
    }

    public void onPivotDataModelChanged(@Observes PivotDataModelChangeEvent event) {
        fmsInit();
    }

    public void loremIpsumInit() {
        this.selectedPreset = "loremipsum";

        this.captions = new String[]{
                "*",
                "Lorem Ipsum / Sector Alpha / Revenue ($)",
                "Lorem Ipsum / Sector Alpha / Expenses ($)",
                "Lorem Ipsum / Sector Beta / Profit ($)",
                "Lorem Ipsum / Sector Beta / Growth (%)",
                "Dolor Sit Amet / Status"
        };

        this.colCount = ((String[]) this.captions).length;

        this.content = new String[][]{
                {"Lorem Row 1", "125000", "84000", "41000", "32.8", "Active"},
                {"Ipsum Row 2", "240000", "150000", "90000", "37.5", "Completed"},
                {"Dolor Row 3", "310000", "195000", "115000", "37.1", "Pending"},
                {"Sit Amet Row 4", "450000", "280000", "170000", "37.7", "In Review"},
                {"Consectetur Row 5", "520000", "310000", "210000", "40.3", "Active"}
        };

        this.rowCount = this.content.length;

        this.componentMap = new HashMap<>();
        this.componentMap.put("c1", "input-money");
        this.componentMap.put("c2", "input-money");
        this.componentMap.put("c3", "input-money");
        this.componentMap.put("c4", "input-number");
        this.componentMap.put("r0_c5", "status-selector");
        this.componentMap.put("r1_c5", "status-selector");
        this.componentMap.put("r2_c5", "status-selector");
        this.componentMap.put("r3_c5", "status-selector");
        this.componentMap.put("r4_c5", "status-selector");

        this.readOnlyCells = Map.of("c0", true);
        this.cellStyles = Map.of(
                "c1", "color: #0284c7; font-weight: 600;",
                "c3", "background-color: rgba(34, 197, 94, 0.15); color: #15803d; font-weight: 700;"
        );
    }

    public void fmsInit() {
        this.selectedPreset = "financial";

        PivotDataModelHandson pivotDataModelHandson = (pivotModifierCtrl != null)
                ? (PivotDataModelHandson) pivotModifierCtrl.getPivotDataModelEdit()
                : null;

        if (pivotDataModelHandson == null) {
            loremIpsumInit();
            return;
        }

        if (pivotDataModelHandson.getColHeaders() != null) {
            this.captions = Stream.concat(Stream.of(""), pivotDataModelHandson.getColHeaders().stream())
                    .toArray(String[]::new);

            if ("ume_form_02".equals(formService.getMyForm().getKey())) {
                this.captions = new String[]{
                        "*",
                        "T.C. Vatandaşı / Kadın",
                        "T.C. Vatandaşı / Erkek",
                        "Yabancı Uyruklu / Kadın",
                        "Yabancı Uyruklu / Erkek",
                        "Toplam"
                };
            }
        } else {
            this.captions = new String[]{
                    "Financial Performance / H1 (Q1-Q2) / Revenue ($)",
                    "Financial Performance / H1 (Q1-Q2) / Expenses ($)",
                    "Financial Performance / H2 (Q3-Q4) / Revenue ($)",
                    "Financial Performance / H2 (Q3-Q4) / Expenses ($)",
                    "Overall Status"
            };
        }

        this.colCount = (this.captions instanceof String[]) ? ((String[]) this.captions).length : 5;

        if (pivotDataModelHandson.getRowHeaders() != null && !pivotDataModelHandson.getRowHeaders().isEmpty()) {
            List<String> rowHeaders = pivotDataModelHandson.getRowHeaders();
            int numRows = rowHeaders.size();
            List<List<String>> data = pivotDataModelHandson.getData();

            this.content = new String[numRows][this.colCount];
            for (int i = 0; i < numRows; i++) {
                this.content[i][0] = rowHeaders.get(i);
                List<String> rowData = (data != null && i < data.size()) ? data.get(i) : null;
                for (int j = 1; j < this.colCount; j++) {
                    int dataColIndex = j - 1;
                    if (rowData != null && dataColIndex < rowData.size() && rowData.get(dataColIndex) != null) {
                        this.content[i][j] = String.valueOf(rowData.get(dataColIndex));
                    } else {
                        this.content[i][j] = "";
                    }
                }
            }
        } else {
            this.content = new String[][]{
                    {"Quarter", "Revenue ($)", "Expenses ($)", "Margin (%)", "Performance"},
                    {"Q1 2026", "120000", "85000", "29.1", "Completed"},
                    {"Q2 2026", "145000", "92000", "36.5", "Active"},
                    {"Q3 2026", "160000", "98000", "38.7", "Pending"},
                    {"Q4 2026", "210000", "110000", "47.6", "In Review"}
            };
        }

        this.rowCount = (this.content != null) ? this.content.length : 5;

        List<PivotDataModelHandson.HandsonTableColRenderer> renderer = pivotDataModelHandson.getRenderer();

        Map<String, String> compMap = new HashMap<>();
        Map<String, Boolean> readOnlyMap = new HashMap<>();

        if (renderer != null && !renderer.isEmpty()) {
            int dataCols = (pivotDataModelHandson.getColHeaders() != null && !pivotDataModelHandson.getColHeaders().isEmpty())
                    ? pivotDataModelHandson.getColHeaders().size()
                    : (this.colCount != null ? this.colCount - 1 : 0);

            if (dataCols > 0 && renderer.size() > dataCols) {
                for (int i = 0; i < renderer.size(); i++) {
                    int r = i / dataCols;
                    int c = i % dataCols;
                    PivotDataModelHandson.HandsonTableColRenderer item = renderer.get(i);
                    if (item != null) {
                        if (item.getComponent() != null && !item.getComponent().isEmpty()) {
                            compMap.put("r" + r + "_c" + (c + 1), componentMatch.get(item.getComponent()));
                        }
                        if (Boolean.TRUE.equals(item.getReadonly())) {
                            readOnlyMap.put("r" + r + "_c" + (c + 1), true);
                        }
                    }
                }
            } else {
                for (int c = 0; c < renderer.size(); c++) {
                    PivotDataModelHandson.HandsonTableColRenderer item = renderer.get(c);
                    if (item != null) {
                        if (item.getComponent() != null && !item.getComponent().isEmpty()) {
                            compMap.put("c" + (c + 1), item.getComponent());
                        }
                        if (Boolean.TRUE.equals(item.getReadonly())) {
                            readOnlyMap.put("c" + (c + 1), true);
                        }
                    }
                }
            }
            this.componentMap = compMap;
            this.readOnlyCells = readOnlyMap;
        } else {
            this.componentMap = new HashMap<>();
            this.componentMap.put("c1", "input-money");
            this.componentMap.put("c2", "input-money");
            this.componentMap.put("c3", "input-number");
            this.componentMap.put("r1_c4", "status-selector");
            this.componentMap.put("r2_c4", "status-selector");
            this.componentMap.put("r3_c4", "status-selector");
            this.componentMap.put("r4_c4", "status-selector");
            this.readOnlyCells = Map.of("r1_c0", true, "r3", true);
        }
        this.cellStyles = Map.of(
                "r1_c3", "background-color: rgba(34, 197, 94, 0.15); color: #15803d; font-weight: 700;",
                "c1", "color: #0284c7; font-weight: 600;"
        );
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
        Map<String, Object> map = new HashMap<>();
        if (readOnlyCells instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) readOnlyCells).entrySet()) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        map.put("c0", true);
        int rows = getRowCount() != null ? getRowCount() : 0;
        for (int i = 0; i < rows; i++) {
            map.put("r" + i + "_c0", true);
        }
        return map;
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

    private String lastSaveStatus;

    public String saveGrid() {
        int r = (this.content != null) ? this.content.length : 0;
        int c = (r > 0 && this.content[0] != null) ? this.content[0].length : 0;
        this.lastSaveStatus = "💾 Grid data (" + r + " rows × " + c + " cols) saved to GridBean at " + java.time.LocalTime.now().toString().substring(0, 8);
        return null;
    }

    public String saveData() {
        return saveGrid();
    }

    public String getLastSaveStatus() {
        return lastSaveStatus;
    }

    public void setLastSaveStatus(String lastSaveStatus) {
        this.lastSaveStatus = lastSaveStatus;
    }

    public String getCssCompatible() {
        return cssCompatible;
    }

    public void setCssCompatible(String cssCompatible) {
        this.cssCompatible = cssCompatible;
    }

    public Boolean isReadOnly() {
        return readOnly != null ? readOnly : false;
    }

    public Boolean getReadOnly() {
        return isReadOnly();
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String toggleReadOnly() {
        this.readOnly = !isReadOnly();
        return null;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
