package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// configuração do endpoint REST / job de uma operação
@Getter
@Setter
@NoArgsConstructor
public class EndpointConfigDTO {
    private boolean job = false;          // operação é @Scheduled (sem endpoint REST)
    private String endpointName;          // path custom; null/vazio = nome da operação
    private String httpMethod = "POST";   // GET | POST | PUT | DELETE | PATCH
    private String cron;                  // expressão cron
}
