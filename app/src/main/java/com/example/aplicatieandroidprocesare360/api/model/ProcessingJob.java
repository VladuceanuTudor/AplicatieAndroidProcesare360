package com.example.aplicatieandroidprocesare360.api.model;

import com.google.gson.annotations.SerializedName;

public class ProcessingJob {

    @SerializedName("job_id")
    private String jobId;

    @SerializedName("status")
    private String status;

    @SerializedName("result_url")
    private String resultUrl;

    @SerializedName("depth_map_url")
    private String depthMapUrl;

    @SerializedName("processing_time_ms")
    private long processingTimeMs;

    @SerializedName("quality_score")
    private float qualityScore;

    @SerializedName("error_message")
    private String errorMessage;

    @SerializedName("progress")
    private int progress;

    public String getJobId()           { return jobId; }
    public String getStatus()          { return status; }
    public String getResultUrl()       { return resultUrl; }
    public String getDepthMapUrl()     { return depthMapUrl; }
    public long   getProcessingTimeMs(){ return processingTimeMs; }
    public float  getQualityScore()    { return qualityScore; }
    public String getErrorMessage()    { return errorMessage; }
    public int    getProgress()        { return progress; }

    public boolean isDone()   { return "done".equalsIgnoreCase(status); }
    public boolean isFailed() { return "failed".equalsIgnoreCase(status); }
}
