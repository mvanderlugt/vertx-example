package us.vanderlugt.sample.vertx.model.country;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Country {
    private String id;
    private String name;
    private String capital;

    public static Country map(JsonObject entry) {
        return Country.builder()
                .id(entry.getString("ID"))
                .name(entry.getString("NAME"))
                .capital(entry.getString("CAPITAL", null))
                .build();
    }
}
