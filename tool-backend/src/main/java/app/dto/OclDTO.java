package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OclDTO {
    private String name; // opt
    private String expression; // req
    private String message; // opt
}
