package com.example.aplicatieandroidprocesare360.api.model;

import com.google.gson.annotations.SerializedName;

// Matches GET /jobs/{job_id}/status response from the backend.
public class ProcessingJob {

    @SerializedName("id")           private String id;
    @SerializedName("status")       private String status;
    @SerializedName("progress_pct") private float  progressPct;
    @SerializedName("error")        private String errorMessage;

    public String getId()           { return id; }
    public String getStatus()       { return status; }
    public float  getProgressPct()  { return progressPct; }
    public String getErrorMessage() { return errorMessage; }

    public boolean isDone()   { return "done".equalsIgnoreCase(status); }
    public boolean isFailed() { return "failed".equalsIgnoreCase(status); }
}
