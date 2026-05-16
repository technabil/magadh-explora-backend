package com.magadhexplora.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    private String dir = "./uploads";
    private String urlPrefix = "/uploads";

    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }

    public String getUrlPrefix() { return urlPrefix; }
    public void setUrlPrefix(String urlPrefix) { this.urlPrefix = urlPrefix; }
}
