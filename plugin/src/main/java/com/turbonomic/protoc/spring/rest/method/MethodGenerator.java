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
package com.turbonomic.protoc.spring.rest.method;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.stringtemplate.v4.ST;

import com.google.api.HttpRule;
import com.google.common.base.CaseFormat;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.TextFormat;
import com.turbonomic.protoc.plugin.common.generator.FieldDescriptor;
import com.turbonomic.protoc.plugin.common.generator.MessageDescriptor;
import com.turbonomic.protoc.plugin.common.generator.ServiceDescriptor;
import com.turbonomic.protoc.plugin.common.generator.ServiceMethodDescriptor;
import com.turbonomic.protoc.plugin.common.generator.ServiceMethodDescriptor.MethodType;
import com.turbonomic.protoc.spring.rest.PathTemplate;
import com.turbonomic.protoc.spring.rest.SpringRestTemplates;

/**
 * A utility class to encapsulate the generation of HTTP methods for gRPC services.
 */
public class MethodGenerator {

    private static final Logger logger = LogManager.getLogger();

    private final ServiceDescriptor serviceDescriptor;

    private final ServiceMethodDescriptor serviceMethodDescriptor;

    private final String responseWrapper;

    public MethodGenerator(@Nonnull final ServiceDescriptor serviceDescriptor,
                           @Nonnull final ServiceMethodDescriptor serviceMethodDescriptor,
                           @Nonnull final String responseWrapper) {
        this.serviceDescriptor = Objects.requireNonNull(serviceDescriptor);
        this.serviceMethodDescriptor = Objects.requireNonNull(serviceMethodDescriptor);
        this.responseWrapper = Objects.requireNonNull(responseWrapper);
    }

    @Nonnull
    public String generateCode() {
        if (!serviceMethodDescriptor.getHttpRule().isPresent()) {
            return new MethodTemplate(SpringMethodType.POST).render();
        } else {
            final HttpRule topLevelRule = serviceMethodDescriptor.getHttpRule().get();
            final StringBuilder allMethodsCode = new StringBuilder();
            allMethodsCode.append(generateMethodFromHttpRule(topLevelRule, Optional.empty()));

            // No recursion allowed - additional bindings will not contain rules that contain
            // additional bindings!
            for (int i = 0; i < topLevelRule.getAdditionalBindingsCount(); ++i) {
                allMethodsCode.append(generateMethodFromHttpRule(
                        topLevelRule.getAdditionalBindings(i), Optional.of(i)));
            }
            return allMethodsCode.toString();
        }
    }

    @Nonnull
    private String generateMethodFromHttpRule(@Nonnull final HttpRule httpRule,
                                              @Nonnull final Optional<Integer> bindingIndex) {
        if (clientStream()) {
            logger.warn("HTTP Rule Patterns not supported for client-stream method: " +
                    serviceMethodDescriptor.getName() + "Generating default POST method.");
            return new MethodTemplate(SpringMethodType.POST).render();
        }
        switch (httpRule.getPatternCase()) {
            case GET:
                return generateMethodCode(httpRule.getGet(), Optional.empty(),
                        SpringMethodType.GET, bindingIndex);
            case PUT:
                return generateMethodCode(httpRule.getPut(), Optional.of(httpRule.getBody()),
                        SpringMethodType.PUT, bindingIndex);
            case POST:
                return generateMethodCode(httpRule.getPost(), Optional.of(httpRule.getBody()),
                        SpringMethodType.POST, bindingIndex);
            case DELETE:
                return generateMethodCode(httpRule.getDelete(), Optional.empty(),
                        SpringMethodType.DELETE, bindingIndex);
            case PATCH:
                return generateMethodCode(httpRule.getPatch(), Optional.of(httpRule.getBody()),
                        SpringMethodType.PATCH, bindingIndex);
            case CUSTOM:
                logger.error("Custom HTTP Rule Pattern Not Supported!\n {}",
                        TextFormat.printToString(httpRule.getCustom()));
                return new MethodTemplate(SpringMethodType.POST).render();
        }
        return new MethodTemplate(SpringMethodType.POST).render();
    }

    @Nonnull
    private String variableForPath(@Nonnull final String path) {
        return CaseFormat.LOWER_UNDERSCORE.to(
                CaseFormat.LOWER_CAMEL, path.replace(".", "_"));
    }

    @Nonnull
    private String generateVariableSetter(@Nonnull final String path,
                                          @Nonnull final FieldDescriptor field) {
        final StringBuilder setFieldBuilder = new StringBuilder();
        setFieldBuilder.append("if (").append(variableForPath(path)).append(" != null) {")
            .append(" inputBuilder");

        final Deque<String> pathStack = new ArrayDeque<>(Arrays.asList(path.split("\\.")));
        while (pathStack.size() > 1) {
            final String nextElement = pathStack.removeFirst();
            setFieldBuilder.append(".get")
                    .append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, nextElement))
                    .append("Builder()");
        }

        if (field.getProto().getLabel().equals(Label.LABEL_REPEATED)) {
            setFieldBuilder.append(".addAll")
                    .append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, pathStack.removeFirst()))
                    .append("(")
                    .append(variableForPath(path))
                    .append(");");
        } else {
            setFieldBuilder.append(".set")
                    .append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, pathStack.removeFirst()))
                    .append("(")
                    .append(variableForPath(path));
            if (field.getProto().getType().equals(Type.TYPE_MESSAGE) ||
                    field.getProto().getType().equals(Type.TYPE_ENUM)) {
                setFieldBuilder.append(".toProto()");
            }
            setFieldBuilder.append(");");
        }

        setFieldBuilder.append("}");


        return setFieldBuilder.toString();
    }

    @Nonnull
    private String generateMethodCode(@Nonnull final String pattern,
                                      @Nonnull final Optional<String> bodyPattern,
                                      @Nonnull final SpringMethodType methodType,
                                      @Nonnull final Optional<Integer> bindingIndex) {
        final PathTemplate template = new PathTemplate(pattern);
        final MessageDescriptor inputDescriptor = serviceMethodDescriptor.getInputMessage();
        final Set<String> boundVariables = template.getBoundVariables();

        final MethodGenerationFieldVisitor fieldVisitor = new MethodGenerationFieldVisitor(boundVariables);

        // We visit all fields. There are two possibilities:
        // 1) Field is "bound" to the path.
        // 2) Field is not bound. It may become a query parameter, or be in the request body.
        inputDescriptor.visitFields(fieldVisitor);

        final List<String> requestArgs = new ArrayList<>();
        final List<String> requestToInputSteps = new ArrayList<>();

        final String inputBuilderPrototype;
        if (bodyPattern.isPresent()) {
            final String body = StringUtils.strip(bodyPattern.get());
            if (body.equals("*")) {
                requestArgs.add("@RequestBody " +
                        getBodyType(true) +
                        " inputDto");
                inputBuilderPrototype = "inputDto.toProto()";
            } else {
                inputBuilderPrototype = "";
                if (body.contains(".")) {
                    throw new IllegalArgumentException("Invalid body: " + body +
                            ". Body must refer to a top-level field.");
                }

                // Must be a field of the top-level request object.
                final FieldDescriptor bodyField = inputDescriptor.getFieldDescriptors().stream()
                        .filter(fieldDescriptor -> fieldDescriptor.getProto().getName().equals(body))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Body field: "
                            + body + " does not exist. Valid fields are: " +
                            inputDescriptor.getFieldDescriptors().stream()
                                .map(FieldDescriptor::getName)
                                .collect(Collectors.joining(", "))));
                if (bodyField.isList() || bodyField.isMapField()) {
                    throw new IllegalArgumentException("Invalid body: " + body +
                            ". Body must refer to a non-repeated/map field.");
                }
                requestArgs.add("@RequestBody(required = " + bodyField.isRequired() + ") " +
                        bodyField.getType() + " " + variableForPath(body));

                requestToInputSteps.add(generateVariableSetter(body, bodyField));
            }
        } else {
            inputBuilderPrototype = "";
            // @RequestParam(name = <path>) <Type> <camelcasePath>
            fieldVisitor.getQueryParamFields().forEach((path, type) -> {
                requestArgs.add("@RequestParam(name = \"" + path + "\", required = " +
                        type.getProto().getLabel().equals(Label.LABEL_REQUIRED) + ") "
                        + type.getType() + " " + variableForPath(path));
                requestToInputSteps.add(generateVariableSetter(path, type));
            });
        }

        // Set the path fields after the body, so the path fields override anything set
        // in the body (if there are collisions).
        //
        // @PathVariable(name = <path>) <Type> <camelcasePath>
        fieldVisitor.getPathFields().forEach((path, type) -> {
            requestArgs.add("@PathVariable(name = \"" + path + "\") " + type.getType() + " " + variableForPath(path));
            requestToInputSteps.add(generateVariableSetter(path, type));
        });

        final StringBuilder requestToInput = new StringBuilder(inputDescriptor.getQualifiedOriginalName())
                .append(".Builder inputBuilder = ")
                .append(inputDescriptor.getQualifiedOriginalName())
                .append(".newBuilder(").append(inputBuilderPrototype).append(");");

        requestToInputSteps.forEach(requestToInput::append);
        requestToInput.append("input = inputBuilder.build();");

        return new MethodTemplate(methodType)
                .setPath(template.getQueryPath())
                .setRequestArgs(requestArgs.stream().collect(Collectors.joining(",")))
                .setRequestToInput(requestToInput.toString())
                .setRestMethodName(serviceMethodDescriptor.getName() +
                        bindingIndex.map(index -> Integer.toString(index)).orElse(""))
                .render();
    }

    private class MethodTemplate {

        private static final String REQUEST_ARGS_NAME = "requestArgs";
        private static final String PREPARE_INPUT_NAME = "prepareInput";
        private static final String PATH_NAME = "path";
        private static final String REST_METHOD_NAME = "restMethodName";

        private final ST template;

        MethodTemplate(@Nonnull final SpringMethodType springMethodType) {
            final MessageDescriptor inputDescriptor = serviceMethodDescriptor.getInputMessage();
            final MessageDescriptor outputDescriptor = serviceMethodDescriptor.getOutputMessage();
            final MethodType type = serviceMethodDescriptor.getType();
            final String requestBodyType = getBodyType(true);
            final String protoInputType = getProtoInputType();
            final String responseBodyType = responseWrapper + "<" + getBodyType(false) + ">";
            this.template = SpringRestTemplates.serviceMethod()
                .add("resultProto", outputDescriptor.getQualifiedOriginalName())
                .add("resultType", outputDescriptor.getQualifiedName())
                .add("responseWrapper", responseWrapper)
                .add("requestProto", inputDescriptor.getQualifiedOriginalName())
                .add("protoInputType", protoInputType)
                .add("requestType", inputDescriptor.getQualifiedName())
                .add("responseBodyType", responseBodyType)
                .add("isClientStream", type == MethodType.BI_STREAM || type == MethodType.CLIENT_STREAM)
                .add("isSingleResponse", type == MethodType.SIMPLE || type == MethodType.CLIENT_STREAM)
                .add("comments", serviceMethodDescriptor.getComment())
                .add("methodName", StringUtils.uncapitalize(serviceMethodDescriptor.getName()))
                .add("methodType", springMethodType.getType())
                .add("serviceName", serviceDescriptor.getName())
                // Defaults.
                .add(REQUEST_ARGS_NAME, "@RequestBody " + requestBodyType + " inputDto")
                .add(PATH_NAME, "/" + serviceDescriptor.getName() + "/" + StringUtils.uncapitalize(serviceMethodDescriptor.getName()))
                .add(REST_METHOD_NAME, StringUtils.uncapitalize(serviceMethodDescriptor.getName()));
            final String defaultPrepareInput;
            if (clientStream()) {
                defaultPrepareInput = "input = inputDto.stream()" +
                            ".map(dto -> dto.toProto())" +
                            ".collect(Collectors.toList());";
            } else {
                defaultPrepareInput = "input = inputDto.toProto();";
            }

            this.template.add(PREPARE_INPUT_NAME, defaultPrepareInput);
        }

        @Nonnull
        public MethodTemplate setRequestArgs(@Nonnull final String requestArgsCode) {
            this.template.remove(REQUEST_ARGS_NAME);
            this.template.add(REQUEST_ARGS_NAME, requestArgsCode);
            return this;
        }

        @Nonnull
        public MethodTemplate setRequestToInput(@Nonnull final String requestToInputCode) {
            this.template.remove(PREPARE_INPUT_NAME);
            this.template.add(PREPARE_INPUT_NAME, requestToInputCode);
            return this;
        }

        @Nonnull
        public MethodTemplate setPath(@Nonnull final String path) {
            this.template.remove(PATH_NAME);
            this.template.add(PATH_NAME, path);
            return this;
        }

        @Nonnull
        public MethodTemplate setRestMethodName(@Nonnull final String restMethodName) {
            this.template.remove(REST_METHOD_NAME);
            this.template.add(REST_METHOD_NAME, restMethodName);
            return this;
        }

        @Nonnull
        public String render() {
            return this.template.render();
        }
    }

    @Nonnull
    private String getBodyType(final boolean request) {
        final MessageDescriptor descriptor = request ?
                serviceMethodDescriptor.getInputMessage() : serviceMethodDescriptor.getOutputMessage();
        final boolean stream = request ? clientStream() : serverStream();

        String requestBodyType = descriptor.getQualifiedName();
        if (stream) {
            requestBodyType = "List<" + requestBodyType + ">";
        }
        return requestBodyType;
    }

    private String getProtoInputType() {
        final MessageDescriptor descriptor = serviceMethodDescriptor.getInputMessage();
        String requestBodyType = descriptor.getQualifiedOriginalName();
        if (clientStream()) {
            requestBodyType = "List<" + requestBodyType + ">";
        }
        return requestBodyType;
    }

    private boolean clientStream() {
        return serviceMethodDescriptor.getType() == MethodType.CLIENT_STREAM ||
            serviceMethodDescriptor.getType() == MethodType.BI_STREAM;
    }

    private boolean serverStream() {
        return serviceMethodDescriptor.getType() == MethodType.SERVER_STREAM ||
                serviceMethodDescriptor.getType() == MethodType.BI_STREAM;
    }

}
