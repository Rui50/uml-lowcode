package generator.spring;

public class SpringProjectSettings {
    private String basePackage = "com.example.app";
    private String applicationName = "spring-template-app";
    private DatabaseType databaseType = DatabaseType.H2;
    private int serverPort = 8080;
    private boolean openApi = true;   // incluir OpenAPI/Swagger UI (springdoc)

    // getters and setters
    public boolean isOpenApi() {
        return openApi;
    }
    public void setOpenApi(boolean openApi) {
        this.openApi = openApi;
    }

    public String getBasePackage() {
        return basePackage;
    }
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }
    public int getServerPort() {
        return serverPort;
    }
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    // "com.example.app" -> "com/example/app"
    public String getPackagePath() {
        return basePackage.replace('.', '/');
    }

    // "spring-template-app" -> "SpringTemplateAppApplication"
    public String getMainClassName() {
        StringBuilder sb = new StringBuilder();
        for (String part : applicationName.split("[^a-zA-Z0-9]")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        sb.append("Application");
        return sb.toString();
    }

}
