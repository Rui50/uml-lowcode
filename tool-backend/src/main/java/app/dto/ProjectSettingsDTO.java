package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProjectSettingsDTO {
    private String basePackage;       // optional, "com.example.app"
    private String applicationName;   // optional, "my-app"
    private String databaseType;      // "H2" | "POSTGRESQL" | "MYSQL"
    private Integer serverPort;       // Spring only
    private Boolean openApi;          // Spring only — incluir OpenAPI/Swagger
    private Boolean dockerfile;       // Spring / CLI-JPA — gerar Dockerfile (+ compose se Postgres/MySQL)
}


