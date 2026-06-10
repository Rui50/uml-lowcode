package model;

public class ProjectSettings {
    private String basePackage;
    private String applicationName;
    private String databaseType;   // "H2" | "POSTGRESQL" | "MYSQL"
    private Integer serverPort;
    private Boolean openApi;        // incluir OpenAPI/Swagger (null = default = true)
    private Boolean dockerfile;     // gerar Dockerfile (null = default = false)

    public Boolean getOpenApi() { return openApi; }
    public void setOpenApi(Boolean openApi) { this.openApi = openApi; }

    public Boolean getDockerfile() { return dockerfile; }
    public void setDockerfile(Boolean dockerfile) { this.dockerfile = dockerfile; }

    public String getBasePackage() { return basePackage; }
    public void setBasePackage(String basePackage) { this.basePackage = basePackage; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }

    public Integer getServerPort() { return serverPort; }
    public void setServerPort(Integer serverPort) { this.serverPort = serverPort; }
}
