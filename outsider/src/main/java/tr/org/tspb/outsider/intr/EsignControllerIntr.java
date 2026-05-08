package tr.org.tspb.outsider.intr;

import jakarta.faces.event.ActionEvent;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.datamodel.gui.FmsOnFlyData;
import tr.org.tspb.util.service.DlgCtrl;

/**
 *
 * @author Telman Şahbazoğlu
 */
public interface EsignControllerIntr extends Serializable, FmsOnFlyData {

    final String TO_BE_SIGNED_DLG_JSF_WIDGET_VAR = "widgetVarToBeSignedDialog";
    final String TO_BE_SIGNED_DLG_JSF_CLIENT_ID = "id-tab-view:idToBeSignedDialog";

    public String getXhtmlPath();

    boolean checkPermissionForEsign(List<Map> list) throws Exception;

    String cleanFullTextSearch();

    String correctMetada() throws Exception;

    void createDownloadFile(ActionEvent ae);

    void esignAjaxCall();

    /*
    public final String esignConfirmEmailHtmlContent = "Sayın Üyemiz,<br/>"
    + "Kurumunuz tarafından elektronik imza ile '%s' tarihinde imzalanan '%s' adlı bildiriminiz Birliğimize ulaşmıştır. Teşekkür ederiz.<br/><br/>"
    + "Bu e-mail bilgilendirme amacıyla sistem tarafından otomatik olarak gönderilmekte olup, lütfen bu e-maili yanıtlamayınız.<br/><br/>"
    // + "Bildirim formunuzun bir örneği ekte sunulmaktadır.<br/><br/>"
    + "Saygılarımızla,<br/><br/>"
    + "Türkiye Sermaye Piyasaları Birliği";
     */
    String esignCollector();

    String esignShowDialog();

    void esignShowDialog1() throws Exception;

    int findDataCount() throws NullNotExpectedException;

    List<Map> findLazyData(int first, int pageSize, Map sortMap1) throws
            NullNotExpectedException;

    Boolean getCanNotifyWithoutEsign();

    String getCrudObjectTextViewer();

    String getCrudObjectTextViewerID();

    String getDialogWidgetVar();

    String getEimzaFileID();

    Map getEmailData();

    String getEsignChunk();

    Integer getEsignChunkIndex();

    LazyDataModel<Map> getEsignedDocs22();

    StreamedContent getFile();

    List<Map<String, String>> getFileData();

    List<Map<String, String>> getFilteredEsignedDocs();

    String getFullTextSearch();

    String getImz64();

    Map<Integer, String> getImz64ChunkedTreeMap();

    List<Map> getMetadata();

    String getMultiUnique();

    DlgCtrl getMyMessageDialog();

    FmsForm getSelectedForm();

    List<Map> getSignedDocContentFields();

    List<Map> getSignedDocInfo();

    List<FmsEimzaAttachment> getSignedEimzaAttachments();

    Map getTransparentProperties();

    UploadedFile getUploadedFile();

    String getUserErrorWarning();

    String getViewId();

    void hidePopup(String clientSideDialogName);

    boolean isAdmin();

    String justNotifyWithoutEsign() throws Exception;

    String makeFullTExtSearch();

    void retrieveSignedEimzaFilesMerge(String multiUnique);

    Map retriveRowData(String rowKey);

    String searchByMemberName();

    void setCanNotifyWithoutEsign(Boolean canNotifyWithoutEsign);

    void setCrudObjectTextViewer(String crudObjectTextViewer);

    void setCrudObjectTextViewerID(String crudObjectTextViewerID);

    void setEimzaDialogWidgerVar(String eimzaDialogWidgerVar);

    void setEimzaFileID(String eimzaFileID);

    void setEmailData(Map emailData);

    void setEsignChunk(String esignChunk);

    void setEsignChunkIndex(Integer esignChunkIndex);

    void setFilteredEsignedDocs(
            List<Map<String, String>> filteredEsignedDocs);

    void setFullTextSearch(String fullTextSearch);

    void setImz64(String imz64);

    void setImz64ChunkedTreeMap(Map<Integer, String> imz64ChunkedTreeMap);

    void setListOfCruds(List<Map> listOfCruds);

    void setMultiUnique(String multiUnique);

    void setMyMessageDialog(DlgCtrl myMessageDialog);

    void setSelectedForm(FmsForm selectedForm);

    void setSignedDocContentFields(List<Map> signedDocContentFields);

    void setSignedDocInfo(List<Map> signedDocInfo);

    void setTransparentProperties(Map transparentProperties);

    void setUploadedFile(UploadedFile uploadedFile);

    String showEsignMetada();

    void showPopup(String clientId, String componentWidgetVar);

    void showPopup(String title, String message, String clientSideDialogName,
            boolean renderedButton);

    void showPopupBackup(String clientSideDialogName);

    void showPopupErr(String message);

    String showSignedEimza();

    void upload(FileUploadEvent event);

    void warn(String clazzName, String errMsg, Exception ex);

}
