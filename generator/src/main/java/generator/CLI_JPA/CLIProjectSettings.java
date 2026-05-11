package generator.CLI_JPA;

public class CLIProjectSettings {
    private String basePackage = "com.example.app";
    private String applicationName = "cli-jpa-template-app";
    private DatabaseType databaseType = DatabaseType.POSTGRESQL;

    // getters and setters
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
