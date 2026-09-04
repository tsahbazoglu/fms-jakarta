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
            var colCount = colHeaders ? colHeaders.length : 0;
            var colWidthsArray = [];
            for (var i = 0; i < colCount; i++) {
                colWidthsArray.push(colWidths);
            }
            var offset = 20;
            var rowHeigth = 30; // Handsontable 18.1.0 row height (~28-30px)
            var rowCount = (data && data.length) ? data.length : (rowHeaders ? rowHeaders.length : 1);
            var divWidth = offset + rowHeaderWidth + colCount * colWidths + "px";
            var divHeigth = 60 + offset + (rowCount * rowHeigth) + "px";

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
                height: 'auto',
                afterRender: function () {
                    var instance = this;
                    setTimeout(function () {
                        if (!instance || instance.isDestroyed) return;
                        var wtHider = container.querySelector('.wtHider');
                        if (wtHider && wtHider.offsetHeight > 0) {
                            var neededHeight = wtHider.offsetHeight + 20;
                            jqueryDiv.css("height", neededHeight + "px");
                            jqueryDiv.parent('#parentContainer').css("height", neededHeight + "px");
                        }
                    }, 50);
                },
                afterGetRowHeader: function (col, TH) {
                    $(TH).css("text-align", "left");
                },
                afterChange: function (changes) {
                    var jsfData = JSON.stringify(this.getData());
                    var hiddenTextJsonDataToModel = $(document.getElementById("id-tab-view:nd-form:id-hidden-text-json-data-to-model"));
                    hiddenTextJsonDataToModel.val(jsfData);

                    var hiddenTextStatusSaved = $(document.getElementById("id-tab-view:nd-form:id-hidden-text-saved-status"));
                    hiddenTextStatusSaved.prop("value", false);

                },
                cells: function (row, col, prop) {
                    // Conditional formatting
                    // https ://handsontable.com/docs/6.1.1/demo-conditional-formatting.html
                    // press F12 go to console and type Handsontable.renderers
                    var cellRender = colRenderers[row][col];
                    if (cellRender.component === "inputText") {
                        return {
                            renderer: function (instance, td, row, col, prop, value, cellProperties) {
                                // Handsontable.TextCell.renderer.apply(this, arguments); -version 0.10.5
                                Handsontable.renderers.TextRenderer.apply(this, arguments);
                                $(td).css({
                                    "background": cellRender.background,
                                    "text-align": "right"
                                });
                            },
                            readOnly: cellRender.readonly
                        };
                    } else if (cellRender.component === "checkBox") {
                        return {
                            renderer: function (instance, td, row, col, prop, value, cellProperties) {
                                Handsontable.renderers.CheckboxRenderer.apply(this, arguments);
                                $(td).css({
                                    background: cellRender.background,
                                    "text-align": "right"
                                });
                            },
                            readOnly: false//cellRender.readonly
                        };
                    } else {
                        return {
                            renderer: function (instance, td, row, col, prop, value, cellProperties) {
                                Handsontable.renderers.TextRenderer.apply(this, arguments);
                                $(td).css({
                                    "background": "white",
                                    "text-align": "right"
                                });
                            }
                        };
                    }
                },
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