package tr.org.tspb.architect.form.validator;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 *
 * @author Telman Şahbazoğlu
 */
@Path("jsonschema")
@RequestScoped
@Produces(TEXT_PLAIN)
@RegisterRestClient(baseUri = "http://localhost:9898/api/")
public interface JsonSchemaRestClient {

    @GET
    @Produces(MediaType.TEXT_HTML + ";charset=utf-8")
    @Consumes(MediaType.TEXT_HTML + ";charset=utf-8")
    @Path("form")
    public Response form(String json);

}
