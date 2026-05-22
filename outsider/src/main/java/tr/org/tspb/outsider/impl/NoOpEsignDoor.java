package tr.org.tspb.outsider.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import tr.org.tspb.datamodel.dao.FmsForm;
import tr.org.tspb.outsider.intr.EsignDoor;

public class NoOpEsignDoor implements EsignDoor, Serializable {

    @Override
    public void eimzaContextInstance(Properties properties) {

    }

    @Override
    public Boolean disabled() {
        return Boolean.TRUE;
    }

    @Override
    public boolean isTest() {
        return false;
    }

    @Override
    public String getSignType() {
        return "";
    }

    @Override
    public void initAndShowEsignDlg(List<Map> list, FmsForm selectedForm, String widgetVarName, String multiUnique) {

    }

    @Override
    public void initEsignCtrl(FmsForm selectedForm, List<Map> listOfCruds, String fullTextSearch, String multiUnique) {

    }

    @Override
    public void initEsignCtrlV3(FmsForm myFormLarge) {

    }

    @Override
    public void iniAndShowEsignDlgV1(TreeMap<Integer, String> treeMap, List<Map> listOfCruds, FmsForm selectedForm, String widgetVarToBeSignedDialog, String UNIQUE) {

    }

    @Override
    public void initEsignCtrlV2(FmsForm selectedForm, String fullTextSearch, String multiUnique) {

    }

    @Override
    public void initAndFindEsigns(FmsForm selectedForm, String fullTextSearch) {

    }

    @Override
    public void initAndFindEsignsV1(FmsForm selectedForm, String fullTextSearch, List<Map> listOfCruds, String multiUnique) {

    }
}