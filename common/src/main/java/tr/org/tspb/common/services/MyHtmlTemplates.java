package tr.org.tspb.common.services;

import com.mongodb.client.model.Filters;
import htmlflow.HtmlPage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;
import java.util.stream.Stream;
import org.bson.Document;
import org.bson.types.ObjectId;
import tr.org.tspb.datamodel.pojo.html.HtmlCompany;
import tr.org.tspb.datamodel.pojo.html.HtmlRole;
import tr.org.tspb.datamodel.pojo.html.HtmlUsrDetail;
import tr.org.tspb.util.qualifier.KeepOpenQualifier;
import tr.org.tspb.util.stereotype.MyServices;
import tr.org.tspb.util.tools.MongoDbUtilIntr;

/**
 *
 * @author Telman Şahbazoğlu
 */
@MyServices
public class MyHtmlTemplates implements Serializable {

    @Inject
    @KeepOpenQualifier
    private MongoDbUtilIntr mongoDbUtil;

    private static Map<String, HtmlRole> roleCache = new HashMap<>();
    private static Map<ObjectId, HtmlCompany> companyCache = new HashMap<>();

    HtmlCompany getCompany(ObjectId companyId) {

        HtmlCompany htmlCompany = companyCache.get(companyId);

        if (htmlCompany == null) {
            Document doc = mongoDbUtil.findOne("tdubdb", "corp_profile",
                    Filters.eq("_id", companyId));
            if (doc != null) {
                htmlCompany = new HtmlCompany(doc);
                companyCache.put(companyId, htmlCompany);
            }
        }

        return htmlCompany;
    }

    HtmlRole getRole(String role) {

        HtmlRole htmlRole = roleCache.get(role);

        if (htmlRole == null) {
            htmlRole = new HtmlRole(role, mongoDbUtil.findOne("configdb",
                    "defroles", Filters.eq("role", role)));
            roleCache.put(role, htmlRole);
        }

        return htmlRole;
    }

    public static void userDetailTemplate(HtmlPage view) {
        view.
                div().
                attrStyle("font-size:18px;") // First Name
                .
                dynamic((body, model) -> {
                    if (model instanceof HtmlUsrDetail htmlUsrDetail) {
                        body.span().
                                attrStyle("font-weight:bold;").
                                text("Ad : ").
                                __().
                                text(htmlUsrDetail.getFirstname());
                    }
                }).

                br().
                __().

                // Last Name
                dynamic((body, model) -> {
                    if (model instanceof HtmlUsrDetail htmlUsrDetail) {
                        body.span().
                                attrStyle("font-weight:bold;").
                                text("Soyad : ").
                                __().
                                text(htmlUsrDetail.getLastname());
                    }
                }).
                br().
                __().

                // Email
                dynamic((body, model) -> {
                    if (model instanceof HtmlUsrDetail htmlUsrDetail) {
                        body.span().
                                attrStyle("font-weight:bold;").
                                text("Eposta : ").
                                __().
                                text(htmlUsrDetail.getEmail());
                    }
                }).
                br().
                __().
                br().
                __().

                // Roles Section
                span().
                attrStyle("font-weight:bold;").
                text("Roller : ").
                __().
                dynamic((body, model) -> {
                    if (model instanceof HtmlUsrDetail htmlUsrDetail) {
                        body.text(htmlUsrDetail.getRolesTable());
                    }
                }).
                br().
                __().
                br().
                __().

                // Companies Section
                span().
                attrStyle("font-weight:bold;font-size:18px;").
                text("Yetkilendirildiği Kuruluşlar : ").
                __().
                br().
                __().
                dynamic((body, model) -> {
                    if (model instanceof HtmlUsrDetail htmlUsrDetail) {
                        body.text(htmlUsrDetail.getCompaniesTable());
                    }
                }).
                __(); // html

    }

    static void companyTableTemplate(HtmlPage view) {

        StringBuilder tableStyle = new StringBuilder(
                ".mytable {border-collapse: collapse;} ");
        tableStyle.append(".mytable td{border: 1px solid black;} ");
        tableStyle.append(
                ".mytable th{border: 1px solid black; background: #e4e2e2;} ");

        view
                .html().
                head() //.style().text(tableStyle.toString()).__()
                .
                title().
                text("Roller").
                __().
                __().
                body().
                div().
                attrClass("ui-datatable ui-widget ui-datatable-resizable").
                div().
                attrClass("ui-datatable-tablewrapper").
                table().
                addAttr("role", "grid") //.attrClass("mytable")
                .
                thead().
                tr().
                th().
                attrClass("ui-state-default ui-resizable-column").
                addAttr("role", "columnheader").
                span().
                attrClass("ui-column-title").
                text("Kuruluş Kodu").
                __().
                __().
                th().
                attrClass("ui-state-default ui-resizable-column").
                addAttr("role", "columnheader").
                span().
                attrClass("ui-column-title").
                text("Ünvan").
                __().
                __().
                __().
                __().
                tbody().
                attrClass("ui-datatable-data ui-widget-content").
                dynamic((tbody, model) -> {
                    if (model instanceof Stream<?> stream) {
                        stream.
                                map(item -> (HtmlCompany) item).
                                forEach(htmlCompany -> tbody
                                .tr().
                                attrClass(
                                        "ui-widget-content ui-datatable-even ui-datatable-selectable").
                                addAttr("role", "row").
                                td().
                                addAttr("role", "gridcell").
                                span().
                                attrStyle(
                                        "white-space:nowrap;font-family: monospace;text-align:left;").
                                text(htmlCompany.getCode()).
                                __().
                                __().
                                td().
                                addAttr("role", "gridcell").
                                span().
                                attrStyle(
                                        "white-space:nowrap;font-family: monospace;text-align:left;").
                                text(htmlCompany.getTitle()).
                                __().
                                __().
                                __() // tr
                                );
                    }
                }
                // forEach
                ) // dynamic
                .
                __() // tbody
                .
                __() // table
                .
                __() // div
                .
                __() // div
                .
                __() // body
                .
                __(); // html

    }

    static void roleTableTemplate(HtmlPage v) {

        StringBuilder tableStyle = new StringBuilder(
                ".mytable {border-collapse: collapse;} ");
        tableStyle.append(".mytable td{border: 1px solid black;} ");
        tableStyle.append(
                ".mytable th{border: 1px solid black; background: #e4e2e2;} ");

        v.html() //.style().text(tableStyle.toString()).__()
                .
                head().
                title().
                text("Roller").
                __().
                __().
                body().
                div().
                attrClass("ui-datatable ui-widget ui-datatable-resizable").
                div().
                attrClass("ui-datatable-tablewrapper").
                table().
                addAttr("role", "grid") //.attrClass("mytable")
                .
                thead().
                tr().
                th().
                attrClass("ui-state-default ui-resizable-column").
                addAttr("role", "columnheader").
                span().
                attrClass("ui-column-title").
                text("Rol Adı").
                __().
                __().
                th().
                attrClass("ui-state-default ui-resizable-column").
                addAttr("role", "columnheader").
                span().
                attrClass("ui-column-title").
                text("Rol Grubu").
                __().
                __().
                th().
                attrClass("ui-state-default ui-resizable-column").
                addAttr("role", "columnheader").
                span().
                attrClass("ui-column-title").
                text("Ünvanı").
                __().
                __().
                th().
                attrClass("ui-state-default ui-resizable-column").
                addAttr("role", "columnheader").
                span().
                attrClass("ui-column-title").
                text("Açıklama").
                __().
                __().
                th().
                attrClass("ui-state-default ui-resizable-column").
                addAttr("role", "columnheader").
                span().
                attrClass("ui-column-title").
                text("Ek Bilgi").
                __().
                __().
                __().
                __().
                tbody().
                attrClass("ui-datatable-data ui-widget-content").
                dynamic((tbody, model) -> {

                    if (model instanceof Stream<?> stream) {
                        stream.
                                map(item -> (HtmlRole) item).
                                forEach((role) -> tbody
                                .tr().
                                attrClass(
                                        "ui-widget-content ui-datatable-even ui-datatable-selectable").
                                addAttr("role", "row").
                                td().
                                addAttr("role", "gridcell").
                                span().
                                attrStyle(
                                        "white-space:nowrap;font-family: monospace;text-align:left;").
                                text(role.getRole()).
                                __().
                                __().
                                td().
                                addAttr("role", "gridcell").
                                span().
                                attrStyle(
                                        "white-space:nowrap;font-family: monospace;text-align:left;").
                                text(role.getGroup()).
                                __().
                                __().
                                td().
                                addAttr("role", "gridcell").
                                span().
                                attrStyle(
                                        "white-space:nowrap;font-family: monospace;text-align:left;").
                                text(role.getTitle()).
                                __().
                                __().
                                td().
                                addAttr("role", "gridcell").
                                span().
                                attrStyle(
                                        "white-space:nowrap;font-family: monospace;text-align:left;").
                                text(role.getDesc()).
                                __().
                                __().
                                td().
                                addAttr("role", "gridcell").
                                span().
                                attrStyle(
                                        "white-space:nowrap;font-family: monospace;text-align:left;").
                                text(role.getInfo()).
                                __().
                                __().
                                __() // tr
                                );
                    }
                }
                // forEach
                ) // dynamic
                .
                __() // tbody
                .
                __() // table
                .
                __() // div
                .
                __() // div
                .
                __() // body
                .
                __(); // html

    }

}
