package tr.org.tspb.datamodel.pojo;

import jakarta.json.bind.annotation.JsonbProperty;
import java.util.List;

public record FmsConstraintApiControlParams(
        @JsonbProperty("ctype-codes") List<String> ctypeCodes,
        @JsonbProperty("collection") String collection,
        @JsonbProperty("ctype-name") String ctypeName,
        @JsonbProperty("function-code") int functionCode
) {}