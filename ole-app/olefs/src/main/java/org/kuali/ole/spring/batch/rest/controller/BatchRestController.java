package org.kuali.ole.spring.batch.rest.controller;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.drools.core.time.impl.CronExpression;
import org.kuali.ole.constants.OleNGConstants;
import org.kuali.ole.oleng.batch.process.model.BatchJobDetails;
import org.kuali.ole.oleng.batch.process.model.BatchProcessJob;
import org.kuali.ole.oleng.batch.process.model.BatchScheduleJob;
import org.kuali.ole.oleng.rest.controller.OleNgControllerBase;
import org.kuali.ole.spring.batch.processor.BatchBibFileProcessor;
import org.kuali.ole.spring.batch.processor.BatchFileProcessor;
import org.kuali.ole.spring.batch.processor.BatchInvoiceImportProcessor;
import org.kuali.ole.spring.batch.processor.BatchOrderImportProcessor;
import org.kuali.ole.utility.OleStopWatch;
import org.kuali.rice.core.api.config.property.ConfigContext;
import org.kuali.rice.krad.util.GlobalVariables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Created by sheiksalahudeenm on 25/6/15.
 */
@Controller
@RequestMapping("/batch")
public class BatchRestController extends OleNgControllerBase {

    @Autowired
    private BatchBibFileProcessor batchBibFileProcessor;

    @Autowired
    private BatchOrderImportProcessor batchOrderImportProcessor;

    @Autowired
    private BatchInvoiceImportProcessor batchInvoiceImportProcessor;

    @RequestMapping(value = "/upload", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON})
    @ResponseBody
    public String UploadFile(@RequestParam("file") MultipartFile file, @RequestParam("profileName") String profileName,
                             @RequestParam("batchType") String batchType, HttpServletRequest request) throws IOException, JSONException {
        if (null != file && StringUtils.isNotBlank(profileName) && StringUtils.isNotBlank(batchType)) {
            String rawContent = IOUtils.toString(file.getBytes());
            return processBatch(profileName, batchType, rawContent);
        }
        return null;
    }

    private String processBatch(String profileName, String batchType, String rawContent) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        OleStopWatch oleStopWatch = new OleStopWatch();
        oleStopWatch.start();
        BatchFileProcessor batchProcessor = getBatchProcessor(batchType);
        batchProcessor.processBatch(rawContent, profileName);
        oleStopWatch.end();
        long totalTime = oleStopWatch.getTotalTime();
        jsonObject.put("processTime",totalTime + "ms");
        return jsonObject.toString();
    }

    @RequestMapping(value = "/submit/api", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON})
    @ResponseBody
    public String submitApi(@RequestBody String requestBody) throws IOException, JSONException {
        JSONObject requestJson = new JSONObject(requestBody);
        String rawContent = requestJson.getString("marcContent");
        String batchType = requestJson.getString("batchType");
        String profileName = requestJson.getString("profileName");
        String batchResponse = processBatch(profileName, batchType, rawContent);
        return batchResponse;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/job/create", produces = {MediaType.APPLICATION_JSON})
    @ResponseBody
    public String createBatchJobDetailsEntry(@RequestBody String requestBody) {
        String response = "";
        BatchProcessJob batchProcessJob;
        try {
            batchProcessJob = convertJsonToProcessJob(requestBody);
            String loginUser = GlobalVariables.getUserSession().getPrincipalName();
            batchProcessJob.setCreatedBy(loginUser);
            batchProcessJob.setCreatedOn(new Timestamp(new Date().getTime()));
            getBusinessObjectService().save(batchProcessJob);
            response = getObjectMapper().writeValueAsString(batchProcessJob);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/job/quickLaunch", produces = {MediaType.APPLICATION_JSON})
    @ResponseBody
    public String quickLaunchJob(@RequestParam("jobId") String jobId, @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            BatchProcessJob matchedBatchJob = getBatchProcessJobById(Long.valueOf(jobId));
            BatchJobDetails batchJobDetails = createBatchJobDetailsEntry(matchedBatchJob);
            if (null != file) {
                String fileContent = IOUtils.toString(file.getBytes());
            }
            getBusinessObjectService().save(batchJobDetails);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @RequestMapping(method = RequestMethod.POST, value = "/job/schedule", produces = {MediaType.APPLICATION_JSON})
    @ResponseBody
    public String scheduleJob(@RequestParam("jobId") String jobId, @RequestParam("file") MultipartFile file, @RequestParam("scheduleJob") String scheduleJobString, HttpServletRequest request) {
        try {
            BatchScheduleJob batchScheduleJob = convertJsonToScheduleJob(scheduleJobString);
            BatchProcessJob matchedBatchJob = getBatchProcessJobById(Long.valueOf(jobId));
            if (null != file) {
                saveUploadedFile(matchedBatchJob, file);
            }
            String cronExpression = batchScheduleJob.getCronExpression();
            if (StringUtils.isNotBlank(cronExpression)) {
                if (org.quartz.CronExpression.isValidExpression(cronExpression)) {
                    matchedBatchJob.setCronExpression(cronExpression);
                    CronExpression cron = new CronExpression(cronExpression);
                    Date date = cron.getNextValidTimeAfter(new Date());
                    matchedBatchJob.setNextRunTime(new Timestamp(date.getTime()));
                }
            }
            getBusinessObjectService().save(matchedBatchJob);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(jobId);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/job/destroy", produces = {MediaType.APPLICATION_JSON})
    @ResponseBody
    public String destroyJob(@RequestParam("jobId") long jobId) {
        try {
            BatchProcessJob matchedBatchJob = getBatchProcessJobById(jobId);
            if (null != matchedBatchJob) {
                // Set Batch Process Job status to Destroyed
                //getBusinessObjectService().save(matchedBatchJob);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(jobId);
    }

    private BatchJobDetails createBatchJobDetailsEntry(BatchProcessJob batchProcessJob) {
        BatchJobDetails batchJobDetails = new BatchJobDetails();
        batchJobDetails.setJobId(batchProcessJob.getJobId());
        batchJobDetails.setJobName(batchProcessJob.getJobName());
        batchJobDetails.setProfileType(batchProcessJob.getProfileType());
        batchJobDetails.setProfileName(batchProcessJob.getBatchProfileName());
        batchJobDetails.setCreatedBy(batchProcessJob.getCreatedBy());
        batchJobDetails.setStartTime(new Timestamp(new Date().getTime()));
        batchJobDetails.setStatus("RUNNING");
        return batchJobDetails;
    }

    public BatchProcessJob convertJsonToProcessJob(String processJsonString) throws JSONException, IOException {
        getObjectMapper().setVisibilityChecker(getObjectMapper().getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        BatchProcessJob batchProcessJob = getObjectMapper().readValue(processJsonString, BatchProcessJob.class);
        return batchProcessJob;
    }

    public BatchScheduleJob convertJsonToScheduleJob(String scheduleJobJsonString) throws JSONException, IOException {
        getObjectMapper().setVisibilityChecker(getObjectMapper().getVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        BatchScheduleJob batchScheduleJob = getObjectMapper().readValue(scheduleJobJsonString, BatchScheduleJob.class);
        return batchScheduleJob;
    }

    public BatchProcessJob getBatchProcessJobById(Long jobId) {
        Map map = new HashedMap();
        map.put(OleNGConstants.JOB_ID, jobId);
        return getBusinessObjectService().findByPrimaryKey(BatchProcessJob.class, map);
    }

    public BatchFileProcessor getBatchProcessor(String batchType) {
        if(batchType.equalsIgnoreCase("Order Record Import")){
            return batchOrderImportProcessor;
        } else if(batchType.equalsIgnoreCase("Bib Import")) {
            return batchBibFileProcessor;
        } else if(batchType.equalsIgnoreCase("Invoice Import")) {
            return batchInvoiceImportProcessor;
        }
        return null;
    }

    private void saveUploadedFile(BatchProcessJob matchedBatchJob, MultipartFile multipartFile) throws IOException {
        String directoryPath = getDirectoryPath(matchedBatchJob.getProfileType(), matchedBatchJob.getJobId());
        File file = new File(directoryPath + multipartFile.getOriginalFilename());
        FileUtils.writeStringToFile(file, IOUtils.toString(multipartFile.getBytes()));
    }

    private String getDirectoryPath(String profileType, long jobId) {
        String batchFileLocation = null;
        String fileSeparator = FileSystems.getDefault().getSeparator();
        String stagingDirectory = ConfigContext.getCurrentContextConfig().getProperty("staging.directory");
        batchFileLocation = stagingDirectory + fileSeparator + getJobDirectoryName(profileType);

        File profileTypeDirectory = new File(batchFileLocation);
        if (profileTypeDirectory.isDirectory()) {
            batchFileLocation = batchFileLocation + fileSeparator + jobId;
            File jobDirectory = new File(batchFileLocation);
            if (jobDirectory.isDirectory()) {
                batchFileLocation = batchFileLocation + fileSeparator;
            } else {
                jobDirectory.mkdir();
                batchFileLocation = batchFileLocation + fileSeparator;
            }
        } else {
            batchFileLocation = batchFileLocation + fileSeparator + jobId;
            File jobDirectory = new File(batchFileLocation);
            jobDirectory.mkdirs();
            batchFileLocation = batchFileLocation + fileSeparator;
        }
        return batchFileLocation;
    }

    private String getJobDirectoryName(String profileType) {
        String jobDirectoryName = null;
        if (StringUtils.equalsIgnoreCase("Bib Import", profileType)) {
            jobDirectoryName = "batchBibImport";
        } else if (StringUtils.equalsIgnoreCase("Invoice Import", profileType)) {
            jobDirectoryName = "batchInvoice";
        } else if (StringUtils.equalsIgnoreCase("Order Record Import", profileType)) {
            jobDirectoryName = "batchOrderRecord";
        }
        return jobDirectoryName;
    }
}