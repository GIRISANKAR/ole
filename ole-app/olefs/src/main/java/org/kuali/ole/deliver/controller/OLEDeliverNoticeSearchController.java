package org.kuali.ole.deliver.controller;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.kuali.incubator.SolrRequestReponseHandler;
import org.kuali.ole.OLEParameterConstants;
import org.kuali.ole.batch.bo.*;
import org.kuali.ole.batch.service.OLEDeliverNoticeService;
import org.kuali.ole.deliver.bo.OLEDeliverNoticeHistory;
import org.kuali.ole.deliver.bo.OLEDeliverNoticeSearchResult;
import org.kuali.ole.deliver.form.OLEDeliverNoticeSearchForm;
import org.kuali.ole.deliver.processor.LoanProcessor;
import org.kuali.rice.core.api.config.property.ConfigContext;
import org.kuali.rice.krad.service.BusinessObjectService;
import org.kuali.rice.krad.service.KRADServiceLocator;
import org.kuali.rice.krad.web.controller.UifControllerBase;
import org.kuali.rice.krad.web.form.UifFormBase;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

/**
 * Created by chenchulakshmig on 10/16/15.
 */
@Controller
@RequestMapping(value = "/deliverNoticeSearchController")
public class OLEDeliverNoticeSearchController extends UifControllerBase {

    private BusinessObjectService businessObjectService;

    @Override
    protected UifFormBase createInitialForm(HttpServletRequest request) {
        return new OLEDeliverNoticeSearchForm();
    }

    @RequestMapping(params = "methodToCall=search")
    public ModelAndView search(@ModelAttribute("KualiForm") UifFormBase form, BindingResult result,
                               HttpServletRequest request, HttpServletResponse response) {
        OLEDeliverNoticeSearchForm oleDeliverNoticeSearchForm = (OLEDeliverNoticeSearchForm) form;
        OLEDeliverNoticeService oleNoticeService = new OLEDeliverNoticeService();


       Map<String, Object> filterFields =  buildFilterFields(oleDeliverNoticeSearchForm);

        String solrQuery = buildSolrQuery(filterFields);
        List results = new SolrRequestReponseHandler().retriveResults(solrQuery);

        for (Iterator iterator = results.iterator(); iterator.hasNext(); ) {
            Map resultsMap = (Map) iterator.next();
            if(resultsMap.containsKey("patronId")){

                String patronId = (String) resultsMap.get("patronId");
                if (StringUtils.isNotBlank(patronId)) {
                    Map<String, Object> criteriaMap = new HashMap<>();
                    if (null != oleDeliverNoticeSearchForm.getPatronBarcode()) {
                        criteriaMap.put("patronId", patronId);
                    }
                    if (null != oleDeliverNoticeSearchForm.getNoticeType()) {
                        criteriaMap.put("noticeType", resultsMap.get("noticeName"));
                    }
                    if (null != oleDeliverNoticeSearchForm.getDateSentTo()) {
                        criteriaMap.put("noticeSentDate", resultsMap.get("dateSent"));
                    }
                    if (criteriaMap.size() > 0) {
                        List<OLEDeliverNoticeHistory> oleDeliverNoticeHistories = (List<OLEDeliverNoticeHistory>) getBusinessObjectService().findMatching(OLEDeliverNoticeHistory.class, criteriaMap);
                        if(CollectionUtils.isNotEmpty(oleDeliverNoticeHistories)){
                            oleDeliverNoticeSearchForm.getOleDeliverNoticeSearchResult().addAll(buildSearchResults(oleDeliverNoticeHistories));
                        }
                    }
                }
                //TODO: Reterive notice content from the notice table based on patron id.
                //TODO: Set it on the resuls section.
                //TODO: Need to provide export options (download/export to word etc..)
            }
        }

        return getUIFModelAndView(oleDeliverNoticeSearchForm);
    }

    private List<OLEDeliverNoticeSearchResult> buildSearchResults(List<OLEDeliverNoticeHistory> oleDeliverNoticeHistories) {
        List<OLEDeliverNoticeSearchResult> oleDeliverNoticeSearchResults = new ArrayList<>();
        for (Iterator<OLEDeliverNoticeHistory> iterator = oleDeliverNoticeHistories.iterator(); iterator.hasNext(); ) {
            OLEDeliverNoticeHistory oleDeliverNoticeHistory = iterator.next();
            OLEDeliverNoticeSearchResult oleDeliverNoticeSearchResult = new OLEDeliverNoticeSearchResult();
            oleDeliverNoticeSearchResult.setNoticeContent(new String(oleDeliverNoticeHistory.getNoticeContent()));
            oleDeliverNoticeSearchResults.add(oleDeliverNoticeSearchResult);
        }
        return oleDeliverNoticeSearchResults;
    }

    private String buildSolrQuery(Map<String, Object> filterFields) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Iterator<String> iterator = filterFields.keySet().iterator(); iterator.hasNext(); ) {
            String field = iterator.next();
            Object value = filterFields.get(field);
            stringBuilder.append(field).append(":").append(value);
            if (iterator.hasNext()) {
                stringBuilder.append(" AND ");
            }
        }
        return stringBuilder.toString();
    }

    private Map<String, Object> buildFilterFields(OLEDeliverNoticeSearchForm oleDeliverNoticeSearchForm) {

        Map<String, Object> filterFields = new HashMap<>();

        if(StringUtils.isNotBlank(oleDeliverNoticeSearchForm.getPatronBarcode())){
            filterFields.put("patronBarcode", oleDeliverNoticeSearchForm.getPatronBarcode());
        }else if(StringUtils.isNotBlank(oleDeliverNoticeSearchForm.getItemBarcode())){
            filterFields.put("itemBarcode", oleDeliverNoticeSearchForm.getItemBarcode());
        }else if(StringUtils.isNotBlank(oleDeliverNoticeSearchForm.getNoticeType())){
            filterFields.put("noticeType", oleDeliverNoticeSearchForm.getNoticeType());
        }else if(StringUtils.isNotBlank(oleDeliverNoticeSearchForm.getDeskLocation())){
            filterFields.put("deskLocation", oleDeliverNoticeSearchForm.getDeskLocation());
        }else if(oleDeliverNoticeSearchForm.getDateSentTo() != null){
            filterFields.put("dateSentTo", oleDeliverNoticeSearchForm.getDateSentTo());
        }

        return filterFields;
    }


    @RequestMapping(params = "methodToCall=clearSearch")
    public ModelAndView clearSearch(@ModelAttribute("KualiForm") UifFormBase form, BindingResult result,
                                    HttpServletRequest request, HttpServletResponse response) {
        OLEDeliverNoticeSearchForm oleDeliverNoticeSearchForm = (OLEDeliverNoticeSearchForm) form;

        oleDeliverNoticeSearchForm.setPatronBarcode(null);
        oleDeliverNoticeSearchForm.setItemBarcode(null);
        oleDeliverNoticeSearchForm.setDateSentFrom(null);
        oleDeliverNoticeSearchForm.setDateSentTo(null);
        oleDeliverNoticeSearchForm.setDeskLocation(null);
        oleDeliverNoticeSearchForm.setNoticeType(null);
        return getUIFModelAndView(oleDeliverNoticeSearchForm);
    }

    public BusinessObjectService getBusinessObjectService() {
        if(null == businessObjectService){
            businessObjectService = KRADServiceLocator.getBusinessObjectService();
        }
        return businessObjectService;
    }
}