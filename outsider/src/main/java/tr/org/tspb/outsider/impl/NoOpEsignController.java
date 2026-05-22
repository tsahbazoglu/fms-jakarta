package tr.org.tspb.outsider.impl;

import jakarta.faces.event.ActionEvent;
import java.util.List;
import java.util.Map;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import tr.org.tspb.constants.exceptions.NullNotExpectedException;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.outsider.intr.EsignControllerIntr;
import tr.org.tspb.outsider.intr.FmsEimzaAttachment;
import tr.org.tspb.util.service.DlgCtrl;

public class NoOpEsignController implements EsignControllerIntr {
    @Override
    public String getXhtmlPath() {
        return "";
    }

    @Override
    public String getXhtmlPathEsignUserGuide() {
        return "";
    }

    @Override
    public boolean checkPermissionForEsign(List<Map> list) throws Exception {
        return false;
    }

    @Override
    public String cleanFullTextSearch() {
        return "";
    }

    @Override
    public String correctMetada() throws Exception {
        return "";
    }

    @Override
    public void createDownloadFile(ActionEvent ae) {

    }

    @Override
    public void esignAjaxCall() {

    }

    @Override
    public String esignCollector() {
        return "";
    }

    @Override
    public String esignShowDialog() {
        return "";
    }

    @Override
    public void esignShowDialog1() throws Exception {

    }

    @Override
    public String errorMessageTest() {
        return "";
    }

    @Override
    public int findDataCount() throws NullNotExpectedException {
        return 0;
    }

    @Override
    public List<Map> findLazyData(int first, int pageSize, Map sortMap1) throws NullNotExpectedException {
        return List.of();
    }

    @Override
    public Boolean getCanNotifyWithoutEsign() {
        return null;
    }

    @Override
    public String getCrudObjectTextViewer() {
        return "";
    }

    @Override
    public String getCrudObjectTextViewerID() {
        return "";
    }

    @Override
    public String getDialogWidgetVar() {
        return "";
    }

    @Override
    public String getEimzaFileID() {
        return "";
    }

    @Override
    public Map getEmailData() {
        return Map.of();
    }

    @Override
    public String getEsignChunk() {
        return "";
    }

    @Override
    public Integer getEsignChunkIndex() {
        return 0;
    }

    @Override
    public LazyDataModel<Map> getEsignedDocs22() {
        return null;
    }

    @Override
    public StreamedContent getFile() {
        return null;
    }

    @Override
    public List<Map<String, String>> getFileData() {
        return List.of();
    }

    @Override
    public List<Map<String, String>> getFilteredEsignedDocs() {
        return List.of();
    }

    @Override
    public String getFullTextSearch() {
        return "";
    }

    @Override
    public String getImz64() {
        return "";
    }

    @Override
    public Map<Integer, String> getImz64ChunkedTreeMap() {
        return Map.of();
    }

    @Override
    public List<Map> getMetadata() {
        return List.of();
    }

    @Override
    public String getMultiUnique() {
        return "";
    }

    @Override
    public DlgCtrl getMyMessageDialog() {
        return null;
    }

    @Override
    public FmsForm getSelectedForm() {
        return null;
    }

    @Override
    public List<Map> getSignedDocContentFields() {
        return List.of();
    }

    @Override
    public List<Map> getSignedDocInfo() {
        return List.of();
    }

    @Override
    public List<FmsEimzaAttachment> getSignedEimzaAttachments() {
        return List.of();
    }

    @Override
    public Map getTransparentProperties() {
        return Map.of();
    }

    @Override
    public UploadedFile getUploadedFile() {
        return null;
    }

    @Override
    public String getUserErrorWarning() {
        return "";
    }

    @Override
    public String getViewId() {
        return "";
    }

    @Override
    public void hidePopup(String clientSideDialogName) {

    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public String justNotifyWithoutEsign() throws Exception {
        return "";
    }

    @Override
    public String makeFullTExtSearch() {
        return "";
    }

    @Override
    public void retrieveSignedEimzaFilesMerge(String multiUnique) {

    }

    @Override
    public Map retriveRowData(String rowKey) {
        return Map.of();
    }

    @Override
    public String searchByMemberName() {
        return "";
    }

    @Override
    public void setCanNotifyWithoutEsign(Boolean canNotifyWithoutEsign) {

    }

    @Override
    public void setCrudObjectTextViewer(String crudObjectTextViewer) {

    }

    @Override
    public void setCrudObjectTextViewerID(String crudObjectTextViewerID) {

    }

    @Override
    public void setEimzaDialogWidgerVar(String eimzaDialogWidgerVar) {

    }

    @Override
    public void setEimzaFileID(String eimzaFileID) {

    }

    @Override
    public void setEmailData(Map emailData) {

    }

    @Override
    public void setEsignChunk(String esignChunk) {

    }

    @Override
    public void setEsignChunkIndex(Integer esignChunkIndex) {

    }

    @Override
    public void setFilteredEsignedDocs(List<Map<String, String>> filteredEsignedDocs) {

    }

    @Override
    public void setFullTextSearch(String fullTextSearch) {

    }

    @Override
    public void setImz64(String imz64) {

    }

    @Override
    public void setImz64ChunkedTreeMap(Map<Integer, String> imz64ChunkedTreeMap) {

    }

    @Override
    public void setListOfCruds(List<Map> listOfCruds) {

    }

    @Override
    public void setMultiUnique(String multiUnique) {

    }

    @Override
    public void setMyMessageDialog(DlgCtrl myMessageDialog) {

    }

    @Override
    public void setSelectedForm(FmsForm selectedForm) {

    }

    @Override
    public void setSignedDocContentFields(List<Map> signedDocContentFields) {

    }

    @Override
    public void setSignedDocInfo(List<Map> signedDocInfo) {

    }

    @Override
    public void setTransparentProperties(Map transparentProperties) {

    }

    @Override
    public void setUploadedFile(UploadedFile uploadedFile) {

    }

    @Override
    public String showEsignMetada() {
        return "";
    }

    @Override
    public void showPopup(String clientId, String componentWidgetVar) {

    }

    @Override
    public void showPopup(String title, String message, String clientSideDialogName, boolean renderedButton) {

    }

    @Override
    public void showPopupBackup(String clientSideDialogName) {

    }

    @Override
    public void showPopupErr(String message) {

    }

    @Override
    public String showSignedEimza() {
        return "";
    }

    @Override
    public void upload(FileUploadEvent event) {

    }

    @Override
    public void warn(String clazzName, String errMsg, Exception ex) {

    }

    @Override
    public String getPinCode() {
        return "";
    }

    @Override
    public void setPinCode(String pinCode) {

    }

    @Override
    public void esignPermissionCheckForSigner() {

    }
}
