var telmangrid = {};

if (!telmangrid.clarity) {
    telmangrid.clarity = {
        init: function (ccid, data, colHeaders, rowHeaders, colRenderers, colWidths, rowHeaderWidth) {

            if (typeof data === "string") {
                data = eval("(" + data + ')');
            }
            if (typeof colHeaders === "string") {
                colHeaders = eval("(" + colHeaders + ')');
            }
            if (typeof rowHeaders === "string") {
                rowHeaders = eval("(" + rowHeaders + ')');
            }
            if (typeof colRenderers === "string") {
                colRenderers = eval("(" + colRenderers + ')');
            }
            if (typeof colWidths === "string") {
                colWidths = eval("(" + colWidths + ')') + 20;
            }
            if (typeof rowHeaderWidth === "string") {
                rowHeaderWidth = eval("(" + rowHeaderWidth + ')') + 80;
            }
            var colCount = colHeaders.length;
            var colWidthsArray = [];
            for (var i = 0; i < colCount; i++) {
                colWidthsArray.push(colWidths);
            }
            var offset = 20;
            var rowHeigth = 24;
            var divWidth = offset + rowHeaderWidth + colCount * colWidths + "px";
            var divHeigth = 100 + offset + rowHeigth + rowHeaders.length * rowHeigth + "px";

            /**
             * jsf id include a char ":" wich is not recognized by jquery as $("#ccid")
             * A workaround is to use pure js function getElementById(ccid)
             * Retrived javasript object is converted then to jquery object just like an id
             */
            var container = document.getElementById(ccid);
            if (!container) return;

            if (container.__hotInstance) {
                try {
                    container.__hotInstance.destroy();
                } catch (e) {}
                container.__hotInstance = null;
            }

            var jqueryDiv = $(container);
            jqueryDiv.addClass("ht-theme-main");
            jqueryDiv.css({
//                "background-color": "blue",
                "width": divWidth,
                "height": divHeigth,
                "overflow": "hidden",
                "pointer-events": "auto"
            });

            var textRenderer = (Handsontable.renderers && Handsontable.renderers.getRenderer) ? 
                Handsontable.renderers.getRenderer('text') : Handsontable.renderers.TextRenderer;
            var checkboxRenderer = (Handsontable.renderers && Handsontable.renderers.getRenderer) ? 
                Handsontable.renderers.getRenderer('checkbox') : Handsontable.renderers.CheckboxRenderer;

            var hot = new Handsontable(container, {
                data: data,
                colHeaders: colHeaders,
                rowHeaders: rowHeaders,
                rowHeaderWidth: rowHeaderWidth,
                colWidths: colWidthsArray,
                manualColumnResize: true,
                width: divWidth,
                height: 500,
                afterGetRowHeader: function (col, TH) {
                    $(TH).css("text-align", "left");
                },
                afterChange: function (changes) {
                    if (!changes) return;
                    var jsfData = JSON.stringify(this.getData());
                    var hiddenTextJsonDataToModel = $(document.getElementById("id-tab-view:nd-form:id-hidden-text-json-data-to-model"));
                    hiddenTextJsonDataToModel.val(jsfData);

                    var hiddenTextStatusSaved = $(document.getElementById("id-tab-view:nd-form:id-hidden-text-saved-status"));
                    hiddenTextStatusSaved.prop("value", false);
                },
                cells: function (row, col, prop) {
                    var cellProperties = {};
                    var cellRender = (colRenderers && colRenderers[row]) ? colRenderers[row][col] : null;

                    if (!cellRender) {
                        return cellProperties;
                    }

                    var isReadOnly = cellRender.readonly === true || cellRender.readonly === "true";

                    if (cellRender.component === "inputText") {
                        cellProperties.editor = isReadOnly ? false : 'text';
                        cellProperties.readOnly = isReadOnly;
                        cellProperties.renderer = function (instance, td, row, col, prop, value, cellProps) {
                            textRenderer.apply(this, arguments);
                            $(td).css({
                                "background": cellRender.background || "white",
                                "text-align": "right"
                            });
                        };
                    } else if (cellRender.component === "checkBox") {
                        cellProperties.type = 'checkbox';
                        cellProperties.editor = 'checkbox';
                        cellProperties.readOnly = isReadOnly;
                        cellProperties.renderer = function (instance, td, row, col, prop, value, cellProps) {
                            checkboxRenderer.apply(this, arguments);
                            $(td).css({
                                "background": cellRender.background || "white",
                                "text-align": "right"
                            });
                        };
                    } else {
                        cellProperties.editor = isReadOnly ? false : 'text';
                        cellProperties.readOnly = isReadOnly;
                        cellProperties.renderer = function (instance, td, row, col, prop, value, cellProps) {
                            textRenderer.apply(this, arguments);
                            $(td).css({
                                "background": "white",
                                "text-align": "right"
                            });
                        };
                    }

                    console.log("Cell [" + row + "][" + col + "] properties:", cellProperties, "cellRender:", cellRender);
                    return cellProperties;
                },
                afterOnCellDblClick: function (event, coords, TD) {
                    if (!coords || coords.row < 0 || coords.col < 0) return;
                    var cellMeta = this.getCellMeta(coords.row, coords.col);
                    if (!cellMeta.readOnly) {
                        var activeEditor = this.getActiveEditor();
                        if (activeEditor && !activeEditor.isOpened()) {
                            activeEditor.beginEditing();
                        }
                    }
                },
                afterSelectionEnd: function (row, col) {
                    this.listen();
                },
                sanitizer: false, // Suppresses header HTML sanitizer warning
                licenseKey: "non-commercial-and-evaluation"
            });

            container.__hotInstance = hot;

            // Prevent PrimeFaces 16 parent containers (p:scrollPanel, p:tabView) from capturing or stopping mouse/touch events
            ['mousedown', 'pointerdown', 'touchstart', 'click', 'dblclick'].forEach(function (eventType) {
                container.addEventListener(eventType, function (e) {
                    e.stopPropagation();
                    if (hot && !hot.isDestroyed) {
                        hot.listen();
                    }
                }, false);
            });

            // Capture phase listeners to ensure focus and editing trigger
            container.addEventListener('click', function (e) {
                if (hot && !hot.isDestroyed) {
                    hot.listen();
                }
            }, true);

            container.addEventListener('dblclick', function (e) {
                if (hot && !hot.isDestroyed) {
                    hot.listen();
                    var selected = hot.getSelectedLast();
                    if (selected) {
                        var row = selected[0];
                        var col = selected[1];
                        var cellMeta = hot.getCellMeta(row, col);
                        if (cellMeta && !cellMeta.readOnly) {
                            var activeEditor = hot.getActiveEditor();
                            if (activeEditor && typeof activeEditor.beginEditing === 'function' && !activeEditor.isOpened()) {
                                activeEditor.beginEditing();
                            }
                        }
                    }
                }
            }, true);

            $('.handsontable table tbody tr th').css({"text-align": "left"});
            $('.handsontable table tbody tr td').css({"text-align": "right"});
        }
    };
}