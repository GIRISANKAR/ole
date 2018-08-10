package org.kuali.ole.oleng.dao.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.kuali.ole.OLEConstants;
import org.kuali.ole.batch.bo.OLEBatchProcessFilterCriteriaBo;
import org.kuali.ole.constants.OleNGConstants;
import org.kuali.ole.deliver.notice.bo.OleNoticeContentConfigurationBo;
import org.kuali.ole.describe.bo.*;
import org.kuali.ole.docstore.common.util.BusinessObjectServiceHelperUtil;
import org.kuali.ole.docstore.engine.service.storage.rdbms.pojo.AccessLocation;
import org.kuali.ole.docstore.engine.service.storage.rdbms.pojo.AuthenticationTypeRecord;
import org.kuali.ole.docstore.engine.service.storage.rdbms.pojo.ReceiptStatusRecord;
import org.kuali.ole.docstore.engine.service.storage.rdbms.pojo.StatisticalSearchRecord;
import org.kuali.ole.oleng.batch.process.model.BatchJobDetails;
import org.kuali.ole.oleng.batch.process.model.BatchProcessJob;
import org.kuali.ole.oleng.batch.profile.model.BatchProcessProfile;
import org.kuali.ole.oleng.dao.DescribeDAO;
import org.kuali.ole.select.bo.OLEDonor;
import org.kuali.ole.select.bo.OleGloballyProtectedField;
import org.kuali.rice.krad.bo.PersistableBusinessObject;
import org.kuali.rice.krad.service.KRADServiceLocator;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by SheikS on 12/17/2015.
 */
@Repository("DescribeDAO")
@Scope("prototype")
public class DescribeDAOImpl extends BusinessObjectServiceHelperUtil implements DescribeDAO {

    public List<OleShelvingScheme> fetchAllCallNumerTypes(){
        return (List<OleShelvingScheme>) KRADServiceLocator.getBusinessObjectService().findAll(OleShelvingScheme.class);
    }

    public List<OleLocation> fetchAllLocations() {
        return (List<OleLocation>) getBusinessObjectService().findAll(OleLocation.class);
    }

    public List<OleBibliographicRecordStatus> fetchAllBibStatus() {
        return (List<OleBibliographicRecordStatus>) KRADServiceLocator.getBusinessObjectService().findAll(OleBibliographicRecordStatus.class);
    }

    @Override
    public List<OleInstanceItemType> fetchAllItemType() {
        return (List<OleInstanceItemType>) getBusinessObjectService().findAll(OleInstanceItemType.class);
    }

    @Override
    public List<OLEDonor> fetchAllDonorCode() {
        return (List<OLEDonor>) getBusinessObjectService().findAll(OLEDonor.class);
    }

    @Override
    public List<OleItemAvailableStatus> fetchAllItemStatus() {
        return (List<OleItemAvailableStatus>) getBusinessObjectService().findAll(OleItemAvailableStatus.class);
    }

    @Override
    public List<BatchProcessProfile> fetchAllProfiles() {
        return (List<BatchProcessProfile>) getBusinessObjectService().findAll(BatchProcessProfile.class);
    }

    @Override
    public List<OleGloballyProtectedField> fetchAllGloballyProtectedFields() {
        return (List<OleGloballyProtectedField>) getBusinessObjectService().findAll(OleGloballyProtectedField.class);
    }

    @Override
    public List<AuthenticationTypeRecord> fetchAuthenticationTypeRecords() {
        return (List<AuthenticationTypeRecord>) getBusinessObjectService().findAll(AuthenticationTypeRecord.class);
    }

    @Override
    public List<ReceiptStatusRecord> fetchReceiptStatusRecords() {
        return (List<ReceiptStatusRecord>) getBusinessObjectService().findAll(ReceiptStatusRecord.class);
    }

    @Override
    public List<AccessLocation> fetchAccessLocations() {
        return (List<AccessLocation>) getBusinessObjectService().findAll(AccessLocation.class);
    }

    @Override
    public List<StatisticalSearchRecord> fetchStatisticalSearchCodes() {
        return (List<StatisticalSearchRecord>) getBusinessObjectService().findAll(StatisticalSearchRecord.class);
    }

    @Override
    public List<BatchProcessJob> fetchAllBatchProcessJobs() {
        return (List<BatchProcessJob>) getBusinessObjectService().findAll(BatchProcessJob.class);
    }

    @Override
    public List<BatchJobDetails> fetchAllBatchJobDetails() {
        return (List<BatchJobDetails>) getBusinessObjectService().findAll(BatchJobDetails.class);
    }

    @Override
    public void deleteProfileById(Long profileId) {
        Map parameterMap = new HashedMap();
        parameterMap.put(OleNGConstants.BATCH_PROCESS_PROFILE_ID,profileId);
        getBusinessObjectService().deleteMatching(BatchProcessProfile.class, parameterMap);
    }

    @Override
    public List<BatchProcessProfile> fetchProfileByNameAndType(String profileName, String type) {
        Map parameterMap = new HashedMap();
        parameterMap.put(OleNGConstants.BATCH_PROCESS_PROFILE_NAME,profileName);
        parameterMap.put("batchProcessType",type);
        return (List<BatchProcessProfile>)getBusinessObjectService().findMatching(BatchProcessProfile.class, parameterMap);
    }

    @Override
    public <T extends PersistableBusinessObject> T save(T bo) {
        return getBusinessObjectService().save(bo);
    }

    @Override
    public List<OLEBatchProcessFilterCriteriaBo> fetchAllBatchProcessFilterCriterias() {
        return (List< OLEBatchProcessFilterCriteriaBo>) getBusinessObjectService().findAll(OLEBatchProcessFilterCriteriaBo.class);
    }

    @Override
    public Map<String,String> getDeliverNoticeNames(){
        Map<String,String> deliverNoticeMap = new HashMap<>();
        Map<String,Object> parameterMap = new HashMap<String,Object>();
        List<String> noticeNames = new ArrayList<>();
        String[] deliverNoticeNames = new String[]{OLEConstants.COURTESY_NOTICE,OLEConstants.OVERDUE_NOTICE,OLEConstants.NOTICE_LOST};
        noticeNames.addAll(Arrays.asList(deliverNoticeNames));
        parameterMap.put("noticeType",noticeNames);
        List<OleNoticeContentConfigurationBo> noticeContentConfigurationBos = (List<OleNoticeContentConfigurationBo>)getBusinessObjectService().findMatching(OleNoticeContentConfigurationBo.class,parameterMap);
        if(CollectionUtils.isNotEmpty(noticeContentConfigurationBos)){
            for(OleNoticeContentConfigurationBo noticeContentConfigurationBo : noticeContentConfigurationBos){
                deliverNoticeMap.put(noticeContentConfigurationBo.getNoticeType(),noticeContentConfigurationBo.getNoticeType());
            }
        }
        return deliverNoticeMap;
    }
}
