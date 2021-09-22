package org.opentripplanner.ext.legacygraphqlapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opentripplanner.api.json.GraphQLResponseSerializer;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO move to org.opentripplanner.api.resource, this is a Jersey resource class

@Path("/routers/{ignoreRouterId}/index/graphql")
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class LegacyGraphQLAPI {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(LegacyGraphQLAPI.class);

    private final Router router;
    private final ObjectMapper deserializer = new ObjectMapper();

    /**
     * @deprecated The support for multiple routers are removed from OTP2. See
     * https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated
    @PathParam("ignoreRouterId")
    private String ignoreRouterId;

    public LegacyGraphQLAPI(@Context OTPServer otpServer) {
        this.router = otpServer.getRouter();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public void getGraphQL(
            @Suspended final AsyncResponse asyncResponse,
            HashMap<String, Object> queryParameters,
            @HeaderParam("OTPTimeout") @DefaultValue("30000") int timeout,
            @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
            @Context HttpHeaders headers
    ) {
        if (queryParameters == null || !queryParameters.containsKey("query")) {
            LOG.debug("No query found in body");
            var response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .entity("No query found in body")
                    .build();
            asyncResponse.resume(response);
        }
        else {

            Locale locale = headers.getAcceptableLanguages().size() > 0
                    ? headers.getAcceptableLanguages().get(0)
                    : router.defaultRoutingRequest.locale;

            String query = (String) queryParameters.get("query");
            Object queryVariables = queryParameters.getOrDefault("variables", null);
            String operationName = (String) queryParameters.getOrDefault("operationName", null);
            Map<String, Object> variables;
            if (queryVariables instanceof Map) {
                variables = (Map) queryVariables;
            }
            else if (queryVariables instanceof String && !((String) queryVariables).isEmpty()) {
                try {
                    variables = deserializer.readValue((String) queryVariables, Map.class);
                }
                catch (IOException e) {
                    var response = Response
                            .status(Response.Status.BAD_REQUEST)
                            .type(MediaType.TEXT_PLAIN_TYPE)
                            .entity("Variables must be a valid json object")
                            .build();

                    asyncResponse.resume(response);
                }
            }
            else {
                variables = new HashMap<>();
                var asyncResult = LegacyGraphQLIndex.getGraphQLResponse(
                        query,
                        router,
                        variables,
                        operationName,
                        maxResolves,
                        timeout,
                        locale
                );
                asyncResult.thenAccept(asyncResponse::resume);
            }
        }
    }

    @POST
    @Path("/")
    @Consumes("application/graphql")
    public void getGraphQL(
            @Suspended final AsyncResponse asyncResponse,
            String query,
            @HeaderParam("OTPTimeout") @DefaultValue("30000") int timeout,
            @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
            @Context HttpHeaders headers
    ) {
        Locale locale = headers.getAcceptableLanguages().size() > 0
                ? headers.getAcceptableLanguages().get(0)
                : router.defaultRoutingRequest.locale;
        var x = LegacyGraphQLIndex.getGraphQLResponse(
                query,
                router,
                null,
                null,
                maxResolves,
                timeout,
                locale
        );
        x.thenAccept(asyncResponse::resume);
    }


    @POST
    @Path("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    public void getGraphQLBatch(
            @Suspended final AsyncResponse asyncResponse,
            List<HashMap<String, Object>> queries,
            @HeaderParam("OTPTimeout") @DefaultValue("30000") int timeout,
            @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") int maxResolves,
            @Context HttpHeaders headers
    ) {
        Locale locale = headers.getAcceptableLanguages().size() > 0
                ? headers.getAcceptableLanguages().get(0)
                : router.defaultRoutingRequest.locale;

        Stream<CompletableFuture<ExecutionResult>> futures = queries.stream().map( query ->{
            Map<String, Object> variables = null;
            try {
                variables = getVariables(query);
            }
            String operationName = (String) query.getOrDefault("operationName", null);

            return LegacyGraphQLIndex.getGraphQLExecutionResult(
                    (String) query.get("query"),
                    router,
                    variables,
                    operationName,
                    maxResolves,
                    timeout,
                    locale
            );
        });

        var results = futures.map(CompletableFuture::join).collect(Collectors.toList());
        var httpResponse = Response.status(Response.Status.OK)
                .entity(GraphQLResponseSerializer.serializeBatchList(queries, results))
                .build();
        asyncResponse.resume(httpResponse);

    }

    private Map<String, Object> getVariables(
            HashMap<String, Object> query
    ) throws IOException {

        if (query.get("variables") instanceof Map) {
            return (Map) query.get("variables");
        }
        else if (query.get("variables") instanceof String
                && ((String) query.get("variables")).length() > 0) {
                return deserializer.readValue((String) query.get("variables"), Map.class);
        }
        else {
            return null;
        }
    }
}
