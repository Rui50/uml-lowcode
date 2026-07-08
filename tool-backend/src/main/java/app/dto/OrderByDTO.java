package app.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// ordenação de uma coleção (persistence tab): @OrderBy("<attribute> <direction>")
@Getter
@Setter
@NoArgsConstructor
public class OrderByDTO {
    private String attribute;
    private String direction;   // "ASC" | "DESC"
}
