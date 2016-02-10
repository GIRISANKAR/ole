var batchProcessJobsApp = angular.module('batchProcessJobs', ['datatables', 'customDirectives']);

var jobTypes = [
    {id: 'adhoc', name: 'Adhoc'},
    {id: 'scheduled', name: 'Scheduled'}
];

var scheduleOptions = [
    {id: 'cron', name: 'Provide Cron Expression'},
    {id: 'schedule', name: 'Schedule'}
];

var scheduleTypes = [
    {id: 'once', name: 'Once'},
    {id: 'daily', name: 'Daily'},
    {id: 'everyWeek', name: 'Every Week'},
    {id: 'everyMonth', name: 'Every Month'}
];

var weekDays = [
    {id: 'monday', name: 'Monday'},
    {id: 'tuesday', name: 'Tuesday'},
    {id: 'wednesday', name: 'Wednesday'},
    {id: 'thursday', name: 'Thursday'},
    {id: 'friday', name: 'Friday'},
    {id: 'saturday', name: 'Saturday'},
    {id: 'sunday', name: 'Sunday'}
];

var monthDays = [];

batchProcessJobsApp.controller('batchProcessJobsController', ['$scope', '$http', function ($scope, $http) {

    for (var i = 1; i <= 31; i++) {
        monthDays.push({id: i,name: i});
    }

    $scope.quickLaunch = {};
    $scope.batchSchedule = {
        scheduleOption: 'Provide Cron Expression',
        scheduleOptions: scheduleOptions,
        scheduleTypes: scheduleTypes,
        weekDays: weekDays,
        monthDays: monthDays
    };

    $scope.init = function() {
        $scope.initializeJobs();
    };

    $scope.initializeCreateJob = function() {
        $scope.batchProcessCreateTab = {};
        $scope.selected = 1;
        $scope.page = 'batchScheduling/views/batchCreateJob.html';
        $scope.profileTypeValues = batchProcessTypeValues;
        $scope.jobTypes = jobTypes;
        $scope.scheduleOptions = scheduleOptions;
        $scope.scheduleTypes = scheduleTypes;
        $scope.weekDays = weekDays;
        $scope.monthDays = [];
        for (var i = 1; i <= 31; i++) {
            $scope.monthDays.push({id: i,name: i});
        }
    };

    $scope.initializeJobs = function() {
        $scope.selected = 2;
        $scope.page = 'batchScheduling/views/batchJobs.html';
        $scope.batchProcessJobs = getBatchProcessJobs();
    };

    $scope.initializeExecutions = function() {
        $scope.selected = 3;
        $scope.page = 'batchScheduling/views/batchJobExecutions.html';
        $scope.batchJobs = getBatchJobs();
    };

    $scope.populateProfileNames = function() {
        $http.get(OLENG_CONSTANTS.PROFILE_GET_NAMES, {params: {"batchType": $scope.batchProcessCreateTab.profileType}}).success(function(data) {
            $scope.profileNames = data;
        });
    };

    $scope.onChangeJobType = function() {
        clearScheduleValues();
    };

    $scope.onChangeScheduleOption = function() {
        $scope.batchSchedule.cronExpression = null;
        $scope.batchSchedule.scheduleType = null;
        $scope.batchSchedule.scheduleDate = null;
        $scope.batchSchedule.weekDay = null;
        $scope.batchSchedule.monthDay = null;
        $scope.batchSchedule.monthFrequency = null;
        $scope.batchSchedule.scheduleTime = null;
    };

    function clearScheduleValues() {
        $scope.batchSchedule.scheduleOption = 'Provide Cron Expression';
        $scope.batchSchedule.cronExpression = null;
        $scope.batchSchedule.scheduleType = null;
        $scope.batchSchedule.scheduleDate = null;
        $scope.batchSchedule.weekDay = null;
        $scope.batchSchedule.monthDay = null;
        $scope.batchSchedule.monthFrequency = null;
        $scope.batchSchedule.scheduleTime = null;
    }

    $scope.submit = function () {
        getProfileNameById();
        var batchProcessJob = {
            "jobId": $scope.batchProcessCreateTab.jobId,
            "jobName": $scope.batchProcessCreateTab.jobName,
            "profileType": $scope.batchProcessCreateTab.profileType,
            "profileId": $scope.batchProcessCreateTab.profileId,
            "profileName": $scope.batchProcessCreateTab.profileName,
            "jobType": $scope.batchProcessCreateTab.jobType,
            "schedule": getBatchSchedule()
        };

        $http.post(OLENG_CONSTANTS.PROCESS_SUBMIT, batchProcessJob).success(function (data) {
            $scope.batchProcessJob = data;
            $scope.initializeJobs();
        });
    };

    function getProfileNameById() {
        angular.forEach($scope.profileNames, function(value, key) {
            if ($scope.batchProcessCreateTab.profileId == value.profileId) {
                $scope.batchProcessCreateTab.profileName = value.profileName;
            }
        })
    }

    function getBatchSchedule() {
        var batchSchedule = {
            scheduleOption: $scope.batchSchedule.scheduleOption,
            scheduleType: $scope.batchSchedule.scheduleType,
            scheduleDate: $scope.batchSchedule.scheduleDate,
            scheduleTime: $scope.batchSchedule.scheduleTime,
            weekDay: $scope.batchSchedule.weekDay,
            monthDay: $scope.batchSchedule.monthDay,
            monthFrequency: $scope.batchSchedule.monthFrequency,
            cronExpression: $scope.batchSchedule.cronExpression
        };
        return batchSchedule;
    }

    function getBatchProcessJobs() {
        $http.get(OLENG_CONSTANTS.PROCESS_JOBS).success(function(data) {
            $scope.batchProcessJobs = data;
        });
    }

    function getBatchJobs() {
        $http.get(OLENG_CONSTANTS.BATCH_JOBS).success(function(data) {
            $scope.batchJobs = data;
        });
    }

    $scope.quickLaunchPopUp = function (jobId) {
        $scope.jobId = jobId;
        $scope.quickLaunch.showModal = !$scope.quickLaunch.showModal;
    };

    $scope.schedulePopUp = function (jobId) {
        clearScheduleValues();
        $scope.jobId = jobId;
        $scope.batchSchedule.showModal = !$scope.batchSchedule.showModal;
    };

    $scope.closeModal = function() {
        $scope.jobId = null;
        if ($scope.quickLaunch.showModal) {
            $scope.quickLaunch.selectedFile = null;
            $scope.quickLaunch.showModal = false;
        } else if ($scope.batchSchedule.showModal) {
            $scope.batchSchedule.showModal = false;
        }
    };

    $scope.scheduleJob = function() {
        var fd = new FormData();
        fd.append('jobId', $scope.jobId);
        fd.append('file', $scope.batchSchedule.scheduleJobFile);
        fd.append('scheduleJob', JSON.stringify(getBatchSchedule()));
        $http.post(OLENG_CONSTANTS.PROCESS_SCHEDULE, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).success(function (data) {
            $scope.message = "Job Scheduled";
            $scope.batchSchedule.showModal = false;
            $scope.closeModal();
        });
        $scope.batchSchedule.showModal = false;
        $scope.closeModal();
    };

    $scope.submitQuickLaunchJob = function() {
        var fd = new FormData();
        fd.append('jobId', $scope.jobId);
        fd.append('file', $scope.quickLaunch.selectedFile);
        $http.post(OLENG_CONSTANTS.PROCESS_QUICK_LAUNCH, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).success(function (data) {
            $scope.message = "Job Launched";
            $scope.quickLaunch.showModal = false;
            $scope.closeModal();
            //$scope.initializeExecutions();
        });
        $scope.quickLaunch.showModal = false;
        $scope.closeModal();
    };

    $scope.destroyJob = function(jobId) {
        $http.get(OLENG_CONSTANTS.DESTROY_PROCESS, {params: {"jobId": jobId}}).success(function(data) {
            $scope.message = "Job Destroyed";
        });
    };

}]);