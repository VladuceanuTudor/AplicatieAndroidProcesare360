package com.example.aplicatieandroidprocesare360.model;

public class Panorama {

    public static final String STATUS_PENDING    = "PENDING";
    public static final String STATUS_UPLOADING  = "UPLOADING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_DONE       = "DONE";
    public static final String STATUS_FAILED     = "FAILED";

    public static final String SOURCE_LOCAL      = "LOCAL";
    public static final String SOURCE_MAPILLARY  = "MAPILLARY";
    public static final String SOURCE_STREETVIEW = "STREETVIEW";

    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_IMAGE = "IMAGE";

    private int    id;
    private String title;
    private String description;
    private String filePath;
    private String thumbnailUrl;
    private long   uploadDate;
    private double latitude;
    private double longitude;
    private String status;
    private String jobId;
    private String resultUrl;
    private String depthMapUrl;
    private float  qualityScore;
    private long   processingTimeMs;
    private float  rating;
    private String sourceType;
    private String mapillaryId;
    private String notes;
    private String jobType;

    public Panorama() {
        this.status     = STATUS_PENDING;
        this.sourceType = SOURCE_LOCAL;
        this.uploadDate = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public long getUploadDate() { return uploadDate; }
    public void setUploadDate(long uploadDate) { this.uploadDate = uploadDate; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean hasLocation() { return latitude != 0 || longitude != 0; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getResultUrl() { return resultUrl; }
    public void setResultUrl(String resultUrl) { this.resultUrl = resultUrl; }

    public String getDepthMapUrl() { return depthMapUrl; }
    public void setDepthMapUrl(String depthMapUrl) { this.depthMapUrl = depthMapUrl; }

    public float getQualityScore() { return qualityScore; }
    public void setQualityScore(float qualityScore) { this.qualityScore = qualityScore; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getMapillaryId() { return mapillaryId; }
    public void setMapillaryId(String mapillaryId) { this.mapillaryId = mapillaryId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getJobType() { return jobType != null ? jobType : TYPE_VIDEO; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public boolean isImageJob() { return TYPE_IMAGE.equals(getJobType()); }
}
