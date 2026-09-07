/**
 * TelmanGrid - PrimeFaces Client Widget for Handsontable 18.1.0
 * Encapsulates Handsontable initialization, lifecycle, theme variables, and AJAX compatibility.
 */
if (window.PrimeFaces) {
    PrimeFaces.widget.TelmanGrid = PrimeFaces.widget.BaseWidget.extend({

        init: function (cfg) {
            this._super(cfg);
            this.id = cfg.id;
            this.jqEl = document.getElementById(this.id);

            if (!this.jqEl) {
                return;
            }

            this.cfg = cfg || {};
            this.parseConfig();
            this.initGrid();
        },

        parseConfig: function () {
            var cfg = this.cfg || {};
            this.data = this.parseJson(cfg.data, []);
            this.colHeaders = this.parseJson(cfg.colHeaders, []);
            this.rowHeaders = this.parseJson(cfg.rowHeaders, []);
            this.colRenderers = this.parseJson(cfg.colRenderers, []);

            var rawColWidth = (cfg.colWidths !== undefined && cfg.colWidths !== null) ? cfg.colWidths : 100;
            var numColWidth = (typeof rawColWidth === "number") ? rawColWidth : parseInt(rawColWidth, 10);
            this.colWidths = (!isNaN(numColWidth) ? numColWidth : 100) + 20;

            var rawRowHeaderWidth = (cfg.rowHeaderWidth !== undefined && cfg.rowHeaderWidth !== null) ? cfg.rowHeaderWidth : 150;
            var numRowHeaderWidth = (typeof rawRowHeaderWidth === "number") ? rawRowHeaderWidth : parseInt(rawRowHeaderWidth, 10);
            this.rowHeaderWidth = (!isNaN(numRowHeaderWidth) ? numRowHeaderWidth : 150) + 80;

            this.isReadOnly = cfg.readOnly === true || cfg.readOnly === "true";
            this.hiddenDataInputId = cfg.hiddenDataInputId || "id-tab-view:nd-form:id-hidden-text-json-data-to-model";
            this.hiddenSavedStatusId = cfg.hiddenSavedStatusId || "id-tab-view:nd-form:id-hidden-text-saved-status";
        },

        parseJson: function (val, fallback) {
            if (!val) return fallback;
            if (typeof val === "object") return val;
            if (typeof val === "string") {
                try {
                    return JSON.parse(val);
                } catch (e) {
                    try {
                        return eval("(" + val + ")");
                    } catch (err) {
                        console.warn("TelmanGrid: Failed to parse JSON config", e);
                        return fallback;
                    }
                }
            }
            return fallback;
        },

        initGrid: function () {
            var self = this;
            var container = this.jqEl;

            // Destroy previous Handsontable instance if existing
            if (container.__hotInstance) {
                try {
                    container.__hotInstance.destroy();
                } catch (e) {}
                container.__hotInstance = null;
            }

            var colCount = (this.colHeaders && this.colHeaders.length) ? this.colHeaders.length : 0;
            var colWidthsArray = [];
            for (var i = 0; i < colCount; i++) {
                colWidthsArray.push(this.colWidths);
            }

            var offset = 20;
            var rowHeight = 30;
            var rowCount = (this.data && this.data.length) ? this.data.length : ((this.rowHeaders && this.rowHeaders.length) ? this.rowHeaders.length : 1);
            var calcWidth = offset + this.rowHeaderWidth + (colCount * this.colWidths);
            var divWidth = (!isNaN(calcWidth) && calcWidth > 0 ? calcWidth : 800) + "px";
            var calcHeight = 60 + offset + (rowCount * rowHeight);
            var divHeight = (!isNaN(calcHeight) && calcHeight > 0 ? calcHeight : 400) + "px";

            var $container = $(container);
            $container.addClass("ht-theme-main telmangrid-container");
            $container.css({
                "width": divWidth,
                "height": divHeight,
                "overflow": "hidden",
                "pointer-events": "auto"
            });

            var textRenderer = (Handsontable.renderers && Handsontable.renderers.getRenderer) ?
                Handsontable.renderers.getRenderer('text') : Handsontable.renderers.TextRenderer;
            var checkboxRenderer = (Handsontable.renderers && Handsontable.renderers.getRenderer) ?
                Handsontable.renderers.getRenderer('checkbox') : Handsontable.renderers.CheckboxRenderer;

            var options = {
                data: this.data,
                colHeaders: this.colHeaders,
                rowHeaders: this.rowHeaders,
                rowHeaderWidth: this.rowHeaderWidth,
                colWidths: colWidthsArray,
                manualColumnResize: true,
                width: divWidth,
                height: 'auto',
                afterRender: function () {
                    self.adjustDynamicHeight();
                },
                afterGetRowHeader: function (col, TH) {
                    $(TH).css("text-align", "left");
                },
                afterChange: function (changes) {
                    if (!changes) return;
                    self.onDataChange();
                },
                cells: function (row, col, prop) {
                    var cellProperties = {};
                    var cellRender = (self.colRenderers && self.colRenderers[row]) ? self.colRenderers[row][col] : null;

                    if (!cellRender) {
                        return cellProperties;
                    }

                    var cellReadOnly = self.isReadOnly || cellRender.readonly === true || cellRender.readonly === "true";

                    if (cellRender.component === "inputText") {
                        cellProperties.editor = cellReadOnly ? false : 'text';
                        cellProperties.readOnly = cellReadOnly;
                        cellProperties.renderer = function (instance, td, r, c, p, value, cellProps) {
                            textRenderer.apply(this, arguments);
                            $(td).css({
                                "background": cellRender.background || "var(--surface-card, #ffffff)",
                                "text-align": "right"
                            });
                        };
                    } else if (cellRender.component === "checkBox") {
                        cellProperties.type = 'checkbox';
                        cellProperties.editor = cellReadOnly ? false : 'checkbox';
                        cellProperties.readOnly = cellReadOnly;
                        cellProperties.renderer = function (instance, td, r, c, p, value, cellProps) {
                            checkboxRenderer.apply(this, arguments);
                            $(td).css({
                                "background": cellRender.background || "var(--surface-card, #ffffff)",
                                "text-align": "right"
                            });
                        };
                    } else {
                        cellProperties.editor = cellReadOnly ? false : 'text';
                        cellProperties.readOnly = cellReadOnly;
                        cellProperties.renderer = function (instance, td, r, c, p, value, cellProps) {
                            textRenderer.apply(this, arguments);
                            $(td).css({
                                "background": "var(--surface-card, #ffffff)",
                                "text-align": "right"
                            });
                        };
                    }

                    return cellProperties;
                },
                afterOnCellDblClick: function (event, coords, TD) {
                    if (!coords || coords.row < 0 || coords.col < 0) return;
                    var cellMeta = this.getCellMeta(coords.row, coords.col);
                    if (!cellMeta.readOnly) {
                        var activeEditor = this.getActiveEditor();
                        if (activeEditor && typeof activeEditor.beginEditing === 'function' && !activeEditor.isOpened()) {
                            activeEditor.beginEditing();
                        }
                    }
                },
                afterSelectionEnd: function (row, col) {
                    this.listen();
                },
                sanitizer: false,
                licenseKey: "non-commercial-and-evaluation"
            };

            this.hot = new Handsontable(container, options);
            container.__hotInstance = this.hot;

            this.setupEventIsolation();

            $('.handsontable table tbody tr th').css({ "text-align": "left" });
            $('.handsontable table tbody tr td').css({ "text-align": "right" });
        },

        setupEventIsolation: function () {
            var self = this;
            var container = this.jqEl;

            // Isolation to prevent PrimeFaces 16 container widgets from swallowing grid events
            ['mousedown', 'pointerdown', 'touchstart', 'click', 'dblclick'].forEach(function (eventType) {
                container.addEventListener(eventType, function (e) {
                    e.stopPropagation();
                    if (self.hot && !self.hot.isDestroyed) {
                        self.hot.listen();
                    }
                }, false);
            });

            container.addEventListener('click', function (e) {
                if (self.hot && !self.hot.isDestroyed) {
                    self.hot.listen();
                }
            }, true);

            container.addEventListener('dblclick', function (e) {
                if (self.hot && !self.hot.isDestroyed) {
                    self.hot.listen();
                    var selected = self.hot.getSelectedLast();
                    if (selected) {
                        var cellMeta = self.hot.getCellMeta(selected[0], selected[1]);
                        if (cellMeta && !cellMeta.readOnly) {
                            var activeEditor = self.hot.getActiveEditor();
                            if (activeEditor && typeof activeEditor.beginEditing === 'function' && !activeEditor.isOpened()) {
                                activeEditor.beginEditing();
                            }
                        }
                    }
                }
            }, true);
        },

        adjustDynamicHeight: function () {
            var self = this;
            setTimeout(function () {
                if (!self.hot || self.hot.isDestroyed) return;
                var wtHider = self.jqEl.querySelector('.wtHider');
                if (wtHider && wtHider.offsetHeight > 0) {
                    var neededHeight = wtHider.offsetHeight + 20;
                    $(self.jqEl).css("height", neededHeight + "px");
                    $(self.jqEl).parent('#parentContainer').css("height", neededHeight + "px");
                }
            }, 50);
        },

        onDataChange: function () {
            if (!this.hot) return;
            var jsfData = JSON.stringify(this.hot.getData());
            var hiddenTextJsonDataToModel = $(document.getElementById(this.hiddenDataInputId));
            if (hiddenTextJsonDataToModel.length) {
                hiddenTextJsonDataToModel.val(jsfData);
            }

            var hiddenTextStatusSaved = $(document.getElementById(this.hiddenSavedStatusId));
            if (hiddenTextStatusSaved.length) {
                hiddenTextStatusSaved.prop("value", false);
            }
        },

        destroy: function () {
            if (this.jqEl && this.jqEl.__hotInstance) {
                try {
                    this.jqEl.__hotInstance.destroy();
                } catch (e) {}
                this.jqEl.__hotInstance = null;
            }
            this.hot = null;
            this._super();
        }
    });
}
