package io.adminshell.apispec.util.service;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class OpenApiProcessor {

    @Value("${apiFolder:.}")
    private String apiFolder;

    @Value("${writeOutput:false}")
    private boolean writeOutput;

    @Value("${outputFileSuffix:.processed}")
    private String outputFileSuffix;

    public void processFolder() throws IOException {
        processFolder(apiFolder);
    }

    public void processFolder(String folder) throws IOException {
            //Set<String> fileList = new HashSet<>();
        log.info("Processing folder {}", folder);

        FileSystem fileSystem = FileSystems.getDefault();
        PathMatcher pathMatcher = fileSystem.getPathMatcher("glob:**.{yaml,yml}");

        Files.walkFileTree(Paths.get(folder), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                //log.info(file.toString());
                if (!Files.isDirectory(file) ) {
                    if (pathMatcher.matches(file)) {
                        //log.info(file.toString());
                        processFile(file.toFile().getAbsolutePath());
                    }
                }
                return super.visitFile(file, attrs);
            }
        });


    }

    public void processFile(String location) throws IOException {

        log.info("Processing file {}", location);

        AuthorizationValue auth = new AuthorizationValue()
            .keyName("api_key")
            .type("header")
            .value("some_api_key");

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(false);

        SwaggerParseResult result = new OpenAPIParser().readLocation(location, Arrays.asList(auth), parseOptions);

        OpenAPI openAPI = result.getOpenAPI();

        if (result.getMessages() != null) result.getMessages().forEach(log::warn); // validation errors and warnings

        //replaceContact(openAPI);

        if (openAPI != null && openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((key, pathItem) -> {
                processOperation("GET", key, pathItem.getGet());
                processOperation("PUT", key, pathItem.getPut());
                processOperation("POST", key, pathItem.getPost());
                processOperation("DELETE", key, pathItem.getDelete());
                processOperation("PATCH", key, pathItem.getPatch());
            });


            if (writeOutput) {
                Yaml y = new Yaml();
                y.mapper().writeValue(new File(location + outputFileSuffix), openAPI);
            }

        }

    }

    private void replaceContact(OpenAPI api) {
        api.getInfo().contact(new Contact()
                .name("name")
                .email("email")
                .url("url"));
    }

    private void processOperation(String method, String path, Operation operation) {
        if (operation == null)
            return;

        log.info("  Processing operation {} ({} {})", operation.getOperationId(), method, path);

        if (operation.getOperationId() == null) {
            log.warn("    operation id is null");
            return;
        }



        updateOperationId(operation);
        updateSemanticIds(operation);
        replaceParameters(operation);
        replaceResultTypeForPagination(operation);

    }

    private void updateOperationId(Operation operation) {
        var id = operation.getOperationId();
        if (id.endsWith("Metadata") && !id.endsWith("-Metadata")) {
            operation.operationId(replace(id,"Metadata","-Metadata"));
            log.debug("    update operation id to {}", operation.getOperationId());
        }
        else if (id.endsWith("ValueOnly") && !id.endsWith("-ValueOnly")) {
            operation.operationId(replace(id,"ValueOnly","-ValueOnly"));
            log.debug("    update operation id to {}", operation.getOperationId());
        }
        else if (id.endsWith("Reference") && !id.endsWith("-Reference")) {
            operation.operationId(replace(id,"Reference","-Reference"));
            log.debug("    update operation id to {}", operation.getOperationId());
        }
        else if (id.endsWith("Path") && !id.endsWith("-Path") && !id.endsWith("ByPath")) {
            operation.operationId(replace(id,"Path","-Path"));
            log.debug("    update operation id to {}", operation.getOperationId());
        }
    }

    private void updateSemanticIds(Operation operation) {
        if (operation.getExtensions() != null && operation.getExtensions().containsKey("x-semanticIds")) {
            log.debug("    update semantic ids");
            ArrayList<String> ids = (ArrayList<String>) operation.getExtensions().get("x-semanticIds");
            ids.forEach(id -> {
                if (!id.endsWith("/3/0")) {
                    log.warn("    found outdated id {}", id);
                }
            });
        } ;
    }

    private void replaceParameters(Operation operation) {
        if (operation.getParameters() != null) {
            var toDelete = new ArrayList<Parameter>();
            var toAdd = new ArrayList<Parameter>();
            operation.getParameters().forEach(parameter -> {
                if ("path".equalsIgnoreCase(parameter.getIn())) {
                    if ("aasIdentifier".equalsIgnoreCase(parameter.getName())) {
                        log.debug("    replace parameter {}", parameter.getName());
                        toDelete.add(parameter);
                        toAdd.add(new Parameter().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/parameters/AssetAdministrationShellIdentifier"));
                    }
                    else if ("submodelIdentifier".equalsIgnoreCase(parameter.getName())) {
                        log.debug("    replace parameter {}", parameter.getName());
                        toDelete.add(parameter);
                        toAdd.add(new Parameter().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/parameters/SubmodelIdentifier"));
                    }
                    else if ("idShortPath".equalsIgnoreCase(parameter.getName())) {
                        log.debug("    replace parameter {}", parameter.getName());
                        toDelete.add(parameter);
                        toAdd.add(new Parameter().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/parameters/IdShortPath"));
                    }
                    else if ("handleId".equalsIgnoreCase(parameter.getName())) {
                        log.debug("    replace parameter {}", parameter.getName());
                        toDelete.add(parameter);
                        toAdd.add(new Parameter().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/parameters/HandleId"));
                    }
                    else if ("packageId".equalsIgnoreCase(parameter.getName())) {
                        log.debug("    replace parameter {}", parameter.getName());
                        toDelete.add(parameter);
                        toAdd.add(new Parameter().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/parameters/PackageId"));
                    }
                }
                else if ("query".equalsIgnoreCase(parameter.getIn())) {
                }
                else {
                    if ("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/parameters/From".equalsIgnoreCase(parameter.get$ref())) {
                        log.debug("    replace parameter From");
                        toDelete.add(parameter);
                        toAdd.add(new Parameter().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/parameters/Cursor"));
                    }
                    else if ("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/parameters/Size".equalsIgnoreCase(parameter.get$ref())) {
                        log.debug("    replace parameter Size");
                        toDelete.add(parameter);
                        toAdd.add(new Parameter().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/parameters/Limit"));
                    }
                }
            });

            operation.getParameters().removeAll(toDelete);
            toAdd.addAll(operation.getParameters());
            operation.setParameters(toAdd);
        }
    }

    private void replaceResultTypeForPagination(Operation operation) {
        var response = operation.getResponses().get("200");
        if (response != null && response.getContent() != null) {
            var mediaType = response.getContent().get("application/json");
            if (mediaType != null && mediaType.getSchema() instanceof ArraySchema) {
                ArraySchema schema = (ArraySchema) mediaType.getSchema();
                var items = schema.getItems();
                String ref = items.get$ref();
                if (ref == null) {
                    log.warn("    Found concrete array item type without $ref {}", items.getType());
                    return;
                }
                switch (ref) {
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part1-MetaModel-Schemas/V3.0#/components/schemas/Reference":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetReferencesResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/PathItem":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetPathsResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part1-MetaModel-Schemas/V3.0#/components/schemas/ConceptDescription":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetConceptDescriptionsResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/PackageDescription":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetPackageDescriptionsResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/AssetAdministrationShellDescriptor":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetAssetAdministrationShellDescriptorsResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/SubmodelDescriptor":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetSubmodelDescriptorsResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part1-MetaModel-Schemas/V3.0#/components/schemas/AssetAdministrationShell":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetAssetAdministrationShellsResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/AssetAdministrationShellMetadata":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetAssetAdministrationShellsMetadataResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part1-MetaModel-Schemas/V3.0#/components/schemas/Submodel":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetSubmodelsResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/SubmodelMetadata":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetSubmodelsMetadataResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/SubmodelValue":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetSubmodelsValueResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part1-MetaModel-Schemas/V3.0#/components/schemas/SubmodelElement":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetSubmodelElementsResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/SubmodelElementMetadata":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetSubmodelElementsMetadataResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/SubmodelElementValue":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/GetSubmodelElementsValueResult"));
                        break;
                    case "https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/Description":
                        log.debug("    replace result type " + ref);
                        mediaType.schema(new Schema().$ref("https://api.swaggerhub.com/domains/Plattform_i40/Part2-API-Schemas/V3.0#/components/schemas/Description"));
                        break;
                    default:
                        log.warn("    Unhandled array type " + ref);
                }
            }
        }
    }



    public static String replace(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
    }

}
