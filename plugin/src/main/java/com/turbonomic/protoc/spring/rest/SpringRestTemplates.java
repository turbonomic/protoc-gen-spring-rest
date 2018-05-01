/*
 *
 * Copyright (C) 2009 - 2018 Turbonomic, Inc.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.turbonomic.protoc.spring.rest;

import org.stringtemplate.v4.ST;

/**
 * This class contains the templates required for code generation.
 * If this becomes unwieldy we can move the actual templates to files
 * and have this class be responsible for reading from the files.
 *
 * TODO (roman, November 11 2016):
 * These string templates can be unwieldy. Consider migrating the code
 * generation to a library such as CodeModel or JavaPoet.
 */
public class SpringRestTemplates {
    private SpringRestTemplates() {}

    public static ST service() {
        return new ST(SERVICE_TEMPLATE);
    }

    public static ST serviceMethod() {
        return new ST(SERVICE_METHOD_TEMPLATE);
    }

    public static ST message() {
        return new ST(MESSAGE_TEMPLATE);
    }

    public static ST enumerator() {
        return new ST(ENUM_TEMPLATE);
    }

    public static ST fieldDeclaration() {
        return new ST(FIELD_DECL_TEMPLATE);
    }

    public static ST setFieldFromProto() {
        return new ST(SET_FIELD_FROM_PROTO_TEMPLATE);
    }

    public static ST addFieldToProtoBuilder() {
        return new ST(ADD_FIELD_TO_PROTO_BUILDER_TEMPLATE);
    }

    public static String imports() {
        return IMPORTS;
    }

    private static final String IMPORTS =
            "import java.util.Map;" +
            "import java.util.List;" +
            "import com.google.gson.annotations.SerializedName;" +
            "import com.fasterxml.jackson.annotation.JsonProperty;" +
            "import com.google.protobuf.ByteString;" +
            "import java.util.stream.Collectors;" +
            "import io.swagger.annotations.ApiModel;" +
            "import io.swagger.annotations.ApiModelProperty;" +
            "import org.springframework.http.MediaType;" +
            "import org.springframework.http.ResponseEntity;" +
            "import org.springframework.web.bind.annotation.PathVariable;" +
            "import org.springframework.web.bind.annotation.RequestParam;" +
            "import org.springframework.web.bind.annotation.RequestBody;" +
            "import org.springframework.web.bind.annotation.RequestMapping;" +
            "import org.springframework.web.bind.annotation.RequestMethod;" +
            "import io.grpc.ManagedChannel;" +
            "import io.swagger.annotations.Api;" +
            "import io.swagger.annotations.ApiModel;" +
            "import io.swagger.annotations.ApiModelProperty;" +
            "import io.swagger.annotations.ApiOperation;" +
            "import java.util.stream.Stream;" +
            "import java.util.stream.StreamSupport;" +
            "import io.grpc.StatusRuntimeException;" +
            "import org.springframework.http.HttpStatus;" +
            "import io.grpc.Status;" +
            "import io.grpc.Status.Code;" +
            "import io.grpc.stub.StreamObserver;" +
            "import java.util.concurrent.LinkedBlockingQueue;" +
            "import java.util.ArrayList;" +
            "import org.springframework.beans.factory.annotation.Autowired;" +
            "import org.springframework.web.bind.annotation.RestController;";

    private static final String SERVICE_TEMPLATE =
        "@Api(value=\"/<serviceName>\") " +
        "@RestController " +
        "public static class <serviceName>Controller {" +
            "@ApiModel(description=\"Wrapper around the responses from <serviceName> to provide optional error information.\")" +
            "public static class <responseWrapper>\\<T> {" +
                "@ApiModelProperty(\"If present, the response from the method call. Null IFF error is present.\")"+
                "public final T response;" +
                "@ApiModelProperty(\"If present, the error encountered during the method call. Null IFF response is present.\")" +
                "public final String error;" +
                "private <responseWrapper>() { response = null; error = null; }" +
                "private <responseWrapper>(T response, String error) { this.response = response; this.error = error; }" +
                "static \\<T> <responseWrapper> success(T response) { return new <responseWrapper>(response, null); }" +
                "static \\<T> <responseWrapper> error(String error) { return new <responseWrapper>(null, error); }" +
            "}" +
            "private <package>.<serviceName>Grpc.<serviceName>ImplBase service;" +
            "public <serviceName>Controller(<package>.<serviceName>Grpc.<serviceName>ImplBase service) {" +
                "this.service = service;" +
            "}" +
            // Individual method definitions
            "<methodDefinitions>" +
        "}";

    private static final String SERVICE_METHOD_TEMPLATE =
        "\n@ApiOperation(value=\"<path>\",notes=<comments>)" +
        "@RequestMapping(path=\"<path>\",method=<methodType>,"+
        "consumes = {MediaType.APPLICATION_JSON_UTF8_VALUE}," +
        "produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})" +
        "public ResponseEntity\\<<responseBodyType>> <restMethodName> (<requestArgs>) {" +
            "if (service == null) {" +
                "return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)" +
                    ".body(<responseWrapper>.error(\"The service <serviceName> was not injected " +
                    "into the RestController. Check that a bean implementing <serviceName>ImplBase" +
                    " exists in the Spring configuration.\"));" +
            "}" +
            "// The plugin code should assign a value to this variable inside \\<prepareInput\\>\n" +
            "final <protoInputType> input;" +
            "try {" +
                // Prepare the "input" field out of the request args.
                // If the "input" field does not get set here, there will be a compilation error.
                "<prepareInput>" +
            "} catch (RuntimeException e) {" +
                "return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)" +
                    ".body(<responseWrapper>.error(\"Failed to prepare input for method <methodName>: \" + e.getMessage()));" +
            "}" +
            "List\\<<resultType>> responseList = new ArrayList\\<>();" +
            "LinkedBlockingQueue\\<Status> statusQueue = new LinkedBlockingQueue\\<>(1);" +
            "StreamObserver\\<<resultProto>> responseObserver = new StreamObserver\\<<resultProto>>() {" +
                "@Override " +
                "public void onNext(<resultProto> value) {" +
                    "responseList.add(<resultType>.fromProto(value));" +
                "}" +
                "@Override " +
                "public void onError(Throwable t) {" +
                    "statusQueue.offer(Status.fromThrowable(t));" +
                "}" +
                "@Override " +
                "public void onCompleted() {" +
                    "statusQueue.offer(Status.OK);" +
                "}" +
            "};" +

            "try {" +
                "<if(isClientStream)>" +
                    "StreamObserver\\<<requestProto>> requestObserver = service.<methodName>(responseObserver);" +
                    "try {" +
                        "input.stream().forEach(requestObserver::onNext);" +
                        "requestObserver.onCompleted();" +
                    "} catch (Exception e) {" +
                        "requestObserver.onError(e);" +
                        "return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)" +
                            ".body(<responseWrapper>.error(\"Error during input processing: \" + e.getMessage()));" +
                    "}" +
                "<else>" +
                    "service.<methodName>(input, responseObserver);" +
                "<endif>" +
            "} catch (RuntimeException e) {" +
                "return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)" +
                ".body(<responseWrapper>.error(\"The method <methodName> returned an exception: \" + e.getMessage()));" +
            "}" +

            "\ntry {" +
                "Status status = statusQueue.take();" +
                "if (status.isOk()) {" +
                    "return ResponseEntity.ok(<responseWrapper>.success(responseList<if(isSingleResponse)>.get(0)<endif>));" +
                "} else if (status.getDescription() != null) {" +
                    "return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)" +
                        ".body(<responseWrapper>.error(status.getDescription()));" +
                "} else if (status.getCause() != null) {" +
                    "return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)" +
                        ".body(<responseWrapper>.error(status.getCause().getMessage()));" +
                "} else {" +
                    "return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)" +
                        ".body(<responseWrapper>.error(\"Unknown error... Sorry.\"));" +
                "}" +
            "} catch (InterruptedException e) {" +
                "return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)" +
                    ".body(<responseWrapper>.error(\"Interrupted while waiting for gRPC call to complete.\"));" +
            "}" +
        "}";

    private static final String MESSAGE_TEMPLATE =
       "@ApiModel(value=\"<className>\", <if(comment)>description=<comment><endif>)" +
       "public static class <className> {" +
           "private <className>() {}" +
           "<nestedDefinitions>" +
           "<fieldDeclarations>" +
           "public <originalProtoType> toProto() {" +
               "<originalProtoType>.Builder builder = <originalProtoType>.newBuilder();" +
               "<setBuilderFields>" +
               "return builder.build();" +
           "}" +
           "public static <className> fromProto(<originalProtoType> input) {" +
               "<className> newMsg = new <className>();" +
               "<setMsgFields>" +
               "return newMsg;" +
           "}" +
       "}";

    private static final String ENUM_TEMPLATE =
        "@ApiModel(description=<comment>)" +
        "public enum <enumName> {" +
            "<values;separator=\",\">;" +
            "private final int value;" +
            "private <enumName>(int value) {" +
                "this.value = value;" +
            "}" +
            "public int getValue() {" +
                "return value;" +
            "}" +
            "public <originalProtoType> toProto() {" +
                "return <originalProtoType>.forNumber(this.value);" +
            "}" +
            "public static <enumName> fromProto(<originalProtoType> input) {" +
                "for (<enumName> val : <enumName>.values()) {" +
                    "if (val.value == input.getNumber()) { return val; }" +
                "}" +
                "throw new IllegalStateException(\"No matching enum for \" + input + \" in \" + <enumName>.values());" +
            "}" +
        "}";

    private static final String FIELD_DECL_TEMPLATE =
        "@ApiModelProperty(required=<isRequired>," +
                "name=\"<displayName>\", " +
                "value=" +
                "<if(hasOneOf)>\"This field belongs to the oneof group: <oneof>." +
                "Only one field in the group should be set, or else weird things will happen.\\n\" +" +
                "<endif>" +
                "<comment>)" +
        "@SerializedName(value=\"<displayName>\")" +
        "@JsonProperty(value=\"<displayName>\")" +
        "public <type> <name> = null;";

    private static final String SET_FIELD_FROM_PROTO_TEMPLATE =
            "<msgName>.<fieldName> = " +
                "<if(isList)>" +
                    "<if(isMap)>" +
                        "input.get<capProtoName>Map()" +
                            "<if(isMapMsg)>" +
                                ".entrySet().stream().collect(" +
                                    "Collectors.toMap(" +
                                        "entry -> entry.getKey()," +
                                        "entry -> <mapValType>.fromProto(entry.getValue())))" +
                            "<endif>" +
                        ";" +
                    "<else>" +
                        "input.get<capProtoName>List()" +
                            "<if(isMsg)>" +
                                ".stream()" +
                                    ".map(item -> <msgType>.fromProto(item))" +
                                    ".collect(Collectors.toList())"+
                            "<endif>" +
                        ";" +
                    "<endif>" +
                "<elseif(isOneOf)>" +
                    "input.get<oneOfName>Case().getNumber() != <fieldNumber> ? " +
                        "null : " +
                        "<if(isMsg)>" +
                            "<msgType>.fromProto(input.get<capProtoName>());" +
                        "<else>" +
                            "input.get<capProtoName>();" +
                        "<endif>" +
                "<elseif(isProto3)>" +
                    "<if(isMsg)>" +
                        "<msgType>.fromProto(input.get<capProtoName>());" +
                    "<else>" +
                        "input.get<capProtoName>();" +
                    "<endif>" +
                "<else>" +
                    "!input.has<capProtoName>() ? null :" +
                        "<if(isMsg)>" +
                            "<msgType>.fromProto(input.get<capProtoName>());" +
                        "<else>" +
                            "input.get<capProtoName>();" +
                        "<endif>" +
                "<endif>";

    private static final String ADD_FIELD_TO_PROTO_BUILDER_TEMPLATE =
        // Need to defend against setting things to null, because that throws exceptions.
        "if (<name> != null) {" +
            "<if(isList)>" +
                "<if(isMap)>" +
                    "builder.putAll<capProtoName>(<name>" +
                        "<if(isMapMsg)>" +
                        ".entrySet().stream()" +
                            ".collect(Collectors.toMap(" +
                                "entry -> entry.getKey()," +
                                "entry -> entry.getValue().toProto()))" +
                        "<endif>" +
                    ");" +
                "<else>" +
                    "builder.addAll<capProtoName>(<name>" +
                        "<if(isMsg)>" +
                            ".stream()" +
                            ".map(item -> item.toProto())" +
                            ".collect(Collectors.toList())" +
                        "<endif>" +
                    ");" +
                "<endif>" +
            "<else>" +
                "builder.set<capProtoName>(<name><if(isMsg)>.toProto()<endif>);" +
            "<endif>" +
        "}";
}
