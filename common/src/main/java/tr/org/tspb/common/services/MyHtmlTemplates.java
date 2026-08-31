package tr.org.tspb.common.services;

import com.mongodb.client.model.Filters;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.types.ObjectId;
import tr.org.tspb.datamodel.pojo.UserDetail;
import tr.org.tspb.datamodel.pojo.html.HtmlCompany;
import tr.org.tspb.datamodel.pojo.html.HtmlRole;
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

    public HtmlCompany getCompany(ObjectId companyId) {
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

    public HtmlRole getRole(String role) {
        HtmlRole htmlRole = roleCache.get(role);

        if (htmlRole == null) {
            htmlRole = new HtmlRole(role, mongoDbUtil.findOne("configdb",
                    "defroles", Filters.eq("role", role)));
            roleCache.put(role, htmlRole);
        }

        return htmlRole;
    }

    public String buildUserDetailHtml(UserDetail userDetail, List<HtmlRole> roles, List<HtmlCompany> companies) {
        StringBuilder sb = new StringBuilder();

        String uidStr = userDetail != null && userDetail.getUsername() != null ? userDetail.getUsername() : "";
        String cnStr = userDetail != null && userDetail.getCommonName() != null ? userDetail.getCommonName() : "";
        if (cnStr.isEmpty() && userDetail != null) {
            String fn = userDetail.getFirstName() != null ? userDetail.getFirstName() : "";
            String ln = userDetail.getLastName() != null ? userDetail.getLastName() : "";
            cnStr = (fn + " " + ln).trim();
        }

        // Top Card: User Profile Details
        sb.append("<div class=\"surface-card p-3 border-round border-1 surface-border mb-3\">");
        sb.append("<div class=\"text-lg font-bold text-900 mb-3\"><i class=\"pi pi-user mr-2\"></i>Kullanıcı Bilgileri</div>");
        sb.append("<div class=\"flex flex-column gap-2\">");
        sb.append("<div><span class=\"font-bold\">Kullanıcı Kodu (uid) : </span>").append(escapeHtml(uidStr)).append("</div>");
        sb.append("<div><span class=\"font-bold\">Adı Soyadı (cn) : </span>").append(escapeHtml(cnStr)).append("</div>");
        sb.append("<div><span class=\"font-bold\">Eposta : </span>").append("__@__.com").append("</div>");
        sb.append("</div>");
        sb.append("</div>");

        // Middle Card: Roles Section (Comma-separated list)
        sb.append("<div class=\"surface-card p-3 border-round border-1 surface-border mb-3\">");
        sb.append("<div class=\"text-lg font-bold text-900 mb-2\"><i class=\"pi pi-user-edit mr-2\"></i>Roller</div>");
        sb.append("<div class=\"text-base\">");
        if (roles == null || roles.isEmpty()) {
            sb.append("<span class=\"text-500\">Kayıt bulunamadı</span>");
        } else {
            String roleNames = roles.stream()
                    .filter(r -> r != null && r.getRole() != null)
                    .map(r -> escapeHtml(r.getRole()))
                    .collect(Collectors.joining(", "));
            sb.append(roleNames);
        }
        sb.append("</div>");
        sb.append("</div>");

        // Bottom Card: Companies Section
        sb.append("<div class=\"surface-card p-3 border-round border-1 surface-border mb-3\">");
        sb.append("<div class=\"text-lg font-bold text-900 mb-2\"><i class=\"pi pi-building mr-2\"></i>Yetkilendirildiği Kuruluşlar</div>");
        sb.append(buildCompanyTable(companies));
        sb.append("</div>");

        return sb.toString();
    }

    public String buildRoleTable(List<HtmlRole> roles) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"ui-datatable ui-widget ui-datatable-resizable mb-2\">");
        sb.append("<div class=\"ui-datatable-tablewrapper\">");
        sb.append("<table role=\"grid\" class=\"w-full\">");
        sb.append("<thead><tr>");
        sb.append("<th class=\"ui-state-default ui-resizable-column\" role=\"columnheader\"><span class=\"ui-column-title\">Rol Adı</span></th>");
        sb.append("<th class=\"ui-state-default ui-resizable-column\" role=\"columnheader\"><span class=\"ui-column-title\">Rol Grubu</span></th>");
        sb.append("<th class=\"ui-state-default ui-resizable-column\" role=\"columnheader\"><span class=\"ui-column-title\">Ünvanı</span></th>");
        sb.append("<th class=\"ui-state-default ui-resizable-column\" role=\"columnheader\"><span class=\"ui-column-title\">Açıklama</span></th>");
        sb.append("<th class=\"ui-state-default ui-resizable-column\" role=\"columnheader\"><span class=\"ui-column-title\">Ek Bilgi</span></th>");
        sb.append("</tr></thead>");

        sb.append("<tbody class=\"ui-datatable-data ui-widget-content\">");
        if (roles == null || roles.isEmpty()) {
            sb.append("<tr class=\"ui-widget-content\"><td colspan=\"5\" style=\"text-align:center; padding:10px;\">Kayıt bulunamadı</td></tr>");
        } else {
            for (HtmlRole role : roles) {
                if (role == null) continue;
                sb.append("<tr class=\"ui-widget-content ui-datatable-even ui-datatable-selectable\" role=\"row\">");
                sb.append("<td role=\"gridcell\"><span style=\"white-space:nowrap;font-family: monospace;text-align:left;\">").append(escapeHtml(role.getRole())).append("</span></td>");
                sb.append("<td role=\"gridcell\"><span style=\"white-space:nowrap;font-family: monospace;text-align:left;\">").append(escapeHtml(role.getGroup())).append("</span></td>");
                sb.append("<td role=\"gridcell\"><span style=\"white-space:nowrap;font-family: monospace;text-align:left;\">").append(escapeHtml(role.getTitle())).append("</span></td>");
                sb.append("<td role=\"gridcell\"><span style=\"white-space:nowrap;font-family: monospace;text-align:left;\">").append(escapeHtml(role.getDesc())).append("</span></td>");
                sb.append("<td role=\"gridcell\"><span style=\"white-space:nowrap;font-family: monospace;text-align:left;\">").append(escapeHtml(role.getInfo())).append("</span></td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table></div></div>");
        return sb.toString();
    }

    public String buildCompanyTable(List<HtmlCompany> companies) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"ui-datatable ui-widget ui-datatable-resizable mb-2\">");
        sb.append("<div class=\"ui-datatable-tablewrapper\">");
        sb.append("<table role=\"grid\" class=\"w-full\">");
        sb.append("<thead><tr>");
        sb.append("<th class=\"ui-state-default ui-resizable-column\" role=\"columnheader\"><span class=\"ui-column-title\">Kuruluş Kodu</span></th>");
        sb.append("<th class=\"ui-state-default ui-resizable-column\" role=\"columnheader\"><span class=\"ui-column-title\">Ünvan</span></th>");
        sb.append("</tr></thead>");

        sb.append("<tbody class=\"ui-datatable-data ui-widget-content\">");
        if (companies == null || companies.isEmpty()) {
            sb.append("<tr class=\"ui-widget-content\"><td colspan=\"2\" style=\"text-align:center; padding:10px;\">Kayıt bulunamadı</td></tr>");
        } else {
            for (HtmlCompany company : companies) {
                if (company == null) continue;
                sb.append("<tr class=\"ui-widget-content ui-datatable-even ui-datatable-selectable\" role=\"row\">");
                sb.append("<td role=\"gridcell\"><span style=\"white-space:nowrap;font-family: monospace;text-align:left;\">").append(escapeHtml(company.getCode())).append("</span></td>");
                sb.append("<td role=\"gridcell\"><span style=\"white-space:nowrap;font-family: monospace;text-align:left;\">").append(escapeHtml(company.getTitle())).append("</span></td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table></div></div>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
