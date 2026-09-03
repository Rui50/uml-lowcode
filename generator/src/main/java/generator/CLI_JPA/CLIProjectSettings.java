package generator.CLI_JPA;

public class CLIProjectSettings {
    private String basePackage = "app";
    private String applicationName = "cli-jpa-template-app";
    // H2 por defeito
    private DatabaseType databaseType = DatabaseType.H2;
    private boolean dockerfile = false;   // gerar Dockerfile

    // getters and setters
    public boolean isDockerfile() {
        return dockerfile;
    }

    public void setDockerfile(boolean dockerfile) {
        this.dockerfile = dockerfile;
    }

    public String getPackagePath() {
        return basePackage.replace('.', '/');
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
}
