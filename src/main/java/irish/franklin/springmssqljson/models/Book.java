package irish.franklin.springmssqljson.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Book {
    private Integer id;

    @NonNull
    private String name;

    @NonNull
    private String description;

    private String author;

    private String language;

    private JsonNode additionalProperties;
}
