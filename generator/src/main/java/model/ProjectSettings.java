package model;

public class ProjectSettings {
    private String basePackage;
    private String applicationName;
    private String databaseType;   // "H2" | "POSTGRESQL" | "MYSQL"
    private Integer serverPort;
    private Boolean openApi;        // incluir OpenAPI/Swagger
    private Boolean dockerfile;     // gerar Dockerfile
    private Boolean timestamps;     // adicionar createdAt/updatedAt nas entidades
    private Boolean demoFrontend;   // gerar uma ui demo HTML+JS em resources/static

    public Boolean getDemoFrontend() { return demoFrontend; }
    public void setDemoFrontend(Boolean demoFrontend) { this.demoFrontend = demoFrontend; }

    public Boolean getOpenApi() { return openApi; }
    public void setOpenApi(Boolean openApi) { this.openApi = openApi; }

    public Boolean getDockerfile() { return dockerfile; }
    public void setDockerfile(Boolean dockerfile) { this.dockerfile = dockerfile; }

    public Boolean getTimestamps() { return timestamps; }
    public void setTimestamps(Boolean timestamps) { this.timestamps = timestamps; }

    public String getBasePackage() { return basePackage; }
    public void setBasePackage(String basePackage) { this.basePackage = basePackage; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }

    public Integer getServerPort() { return serverPort; }
    public void setServerPort(Integer serverPort) { this.serverPort = serverPort; }
}
