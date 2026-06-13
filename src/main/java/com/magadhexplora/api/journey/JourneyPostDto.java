package com.magadhexplora.api.journey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class JourneyPostDto {

    private Long id;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 160)
    private String location;

    @Size(max = 500)
    private String caption;

    private String mediaType;

    @NotBlank
    @Size(max = 500)
    private String mediaUrl;

    @Size(max = 500)
    private String videoUrl;

    private int likes;
    private boolean approved;
    private Instant createdAt;

    public static JourneyPostDto from(JourneyPostEntity e) {
        JourneyPostDto d = new JourneyPostDto();
        d.id = e.getId();
        d.name = e.getName();
        d.location = e.getLocation();
        d.caption = e.getCaption();
        d.mediaType = e.getMediaType();
        d.mediaUrl = e.getMediaUrl();
        d.videoUrl = e.getVideoUrl();
        d.likes = e.getLikes();
        d.approved = e.isApproved();
        d.createdAt = e.getCreatedAt();
        return d;
    }

    public JourneyPostEntity toEntity() {
        JourneyPostEntity e = new JourneyPostEntity();
        e.setName(name.trim());
        e.setLocation(location == null ? null : location.trim());
        e.setCaption(caption == null ? null : caption.trim());
        e.setMediaType(normalizeType(mediaType));
        e.setMediaUrl(mediaUrl.trim());
        e.setVideoUrl(videoUrl == null || videoUrl.isBlank() ? null : videoUrl.trim());
        return e;
    }

    /** Only photo/video/reel allowed; anything else falls back to photo. */
    public static String normalizeType(String t) {
        if (t == null) return "photo";
        return switch (t.trim().toLowerCase()) {
            case "video" -> "video";
            case "reel"  -> "reel";
            default      -> "photo";
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
