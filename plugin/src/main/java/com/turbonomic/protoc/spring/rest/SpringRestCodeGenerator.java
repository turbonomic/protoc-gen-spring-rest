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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.stringtemplate.v4.ST;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;

import com.turbonomic.protoc.plugin.common.generator.EnumDescriptor;
import com.turbonomic.protoc.plugin.common.generator.FieldDescriptor;
import com.turbonomic.protoc.plugin.common.generator.MessageDescriptor;
import com.turbonomic.protoc.plugin.common.generator.ProtocPluginCodeGenerator;
import com.turbonomic.protoc.plugin.common.generator.ServiceDescriptor;
import com.turbonomic.protoc.spring.rest.method.MethodGenerator;

/**
 * An implementation of {@link ProtocPluginCodeGenerator} that generates Spring Framework-compatible
 * REST controllers and swagger-annotated POJOs for gRPC services.
 */
class SpringRestCodeGenerator extends ProtocPluginCodeGenerator {

    private static final Logger logger = LogManager.getLogger();

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String getPluginName() {
        return "protoc-spring-rest";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    protected String generatePluginJavaClass(@Nonnull final String protoJavaClass) {
        return protoJavaClass + "REST";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    protected String generateImports() {
        return SpringRestTemplates.imports();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    protected Optional<String> generateEnumCode(@Nonnull final EnumDescriptor enumDescriptor) {
        return Optional.of(SpringRestTemplates.enumerator()
                .add("enumName", enumDescriptor.getName())
                .add("comment", enumDescriptor.getComment())
                .add("originalProtoType", enumDescriptor.getQualifiedOriginalName())
                .add("values", enumDescriptor.getValues().entrySet().stream()
                        .map(valueEntry -> {
                            String valueTemplate =
                                    "@ApiModelProperty(value=<comment>)" +
                                            "<name>(<value>)";
                            ST valueMsg = new ST(valueTemplate);
                            valueMsg.add("comment", enumDescriptor.getValueComment(valueEntry.getKey()));
                            valueMsg.add("name", valueEntry.getKey());
                            valueMsg.add("value", valueEntry.getValue());
                            return valueMsg.render();
                        })
                        .collect(Collectors.toList()))
                .render());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    protected Optional<String> generateServiceCode(@Nonnull final ServiceDescriptor serviceDescriptor) {
        final String responseWrapper = serviceDescriptor.getName() + "Response";

        return Optional.of(SpringRestTemplates.service()
                .add("serviceName", serviceDescriptor.getName())
                .add("responseWrapper", responseWrapper)
                .add("package", serviceDescriptor.getJavaPkgName())
                .add("methodDefinitions", serviceDescriptor.getMethodDescriptors().stream()
                    .map(serviceMethodDescriptor -> new MethodGenerator(serviceDescriptor,
                            serviceMethodDescriptor, responseWrapper).generateCode())
                    .collect(Collectors.toList()))
                .render());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    protected Optional<String> generateMessageCode(@Nonnull final MessageDescriptor messageDescriptor) {
        final Map<Integer, String> oneofNameMap = new HashMap<>();
        final DescriptorProto descriptorProto = messageDescriptor.getDescriptorProto();
        for (int i = 0; i < descriptorProto.getOneofDeclCount(); ++i) {
            oneofNameMap.put(i, descriptorProto.getOneofDecl(i).getName());
        }

        return Optional.of(SpringRestTemplates.message()
                .add("comment", messageDescriptor.getComment())
                .add("className", messageDescriptor.getName())
                .add("originalProtoType", messageDescriptor.getQualifiedOriginalName())
                .add("nestedDefinitions", messageDescriptor.getNestedMessages().stream()
                        .filter(nestedDescriptor -> !(nestedDescriptor instanceof MessageDescriptor &&
                                ((MessageDescriptor) nestedDescriptor).isMapEntry()))
                        .map(this::generateCode)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()))
                .add("fieldDeclarations", messageDescriptor.getFieldDescriptors().stream()
                        .map(descriptor -> generateFieldDeclaration(descriptor, oneofNameMap))
                        .collect(Collectors.toList()))
                .add("setBuilderFields", messageDescriptor.getFieldDescriptors().stream()
                        .map(this::addFieldToProtoBuilder)
                        .collect(Collectors.toList()))
                .add("setMsgFields", messageDescriptor.getFieldDescriptors().stream()
                        .map(descriptor -> addFieldSetFromProto(descriptor, "newMsg"))
                        .collect(Collectors.toList()))
                .render());
    }

    @Nonnull
    private String generateFieldDeclaration(@Nonnull final FieldDescriptor fieldDescriptor,
                                            @Nonnull final Map<Integer, String> oneofNameMap) {
        final ST template = SpringRestTemplates.fieldDeclaration()
                .add("type", fieldDescriptor.getType())
                .add("displayName", fieldDescriptor.getName())
                .add("name", fieldDescriptor.getSuffixedName())
                .add("isRequired", fieldDescriptor.getProto().getLabel() == Label.LABEL_REQUIRED)
                .add("comment", fieldDescriptor.getComment());
        if (fieldDescriptor.getProto().hasOneofIndex()) {
            template.add("hasOneOf", true);
            template.add("oneof", FieldDescriptor.formatFieldName(
                    oneofNameMap.get(fieldDescriptor.getProto().getOneofIndex())));
        } else {
            template.add("hasOneOf", false);
        }
        return template.render();
    }

    /**
     * Generate code to add this field to the builder that creates
     * a protobuf object from the generated Java object.
     *
     * @return The generated code string.
     */
    @Nonnull
    private String addFieldToProtoBuilder(@Nonnull final FieldDescriptor fieldDescriptor) {
        final ST template = SpringRestTemplates.addFieldToProtoBuilder()
                .add("name", fieldDescriptor.getSuffixedName())
                .add("capProtoName", StringUtils.capitalize(fieldDescriptor.getName()))
                .add("isList", fieldDescriptor.isList())
                .add("isMsg", fieldDescriptor.getContentMessage().isPresent())
                .add("isMap", fieldDescriptor.isMapField());
        if (fieldDescriptor.isMapField()) {
            FieldDescriptor value = fieldDescriptor.getContentMessage()
                    .map(descriptor -> ((MessageDescriptor) descriptor).getMapValue())
                    .orElseThrow(() -> new IllegalStateException("Content message not present in map field."));
            template.add("isMapMsg", value.getContentMessage().isPresent());
        }
        return template.render();
    }

    /**
     * Generate code to set this field in the generated Java
     * object from a protobuf object.
     *
     * @param msgName The variable name of the protobuf object.
     * @return The generated code string.
     */
    @Nonnull
    private String addFieldSetFromProto(@Nonnull final FieldDescriptor fieldDescriptor,
                                        @Nonnull final String msgName) {
        final ST template = SpringRestTemplates.setFieldFromProto()
                .add("isMsg", fieldDescriptor.getContentMessage().isPresent())
                .add("isProto3", fieldDescriptor.isProto3Syntax())
                .add("msgName", msgName)
                .add("fieldName", fieldDescriptor.getSuffixedName())
                .add("fieldNumber", fieldDescriptor.getProto().getNumber())
                .add("capProtoName", StringUtils.capitalize(fieldDescriptor.getName()))
                .add("msgType", fieldDescriptor.getTypeName())
                .add("isList", fieldDescriptor.isList())
                .add("isMap", fieldDescriptor.isMapField())
                .add("isOneOf", fieldDescriptor.getOneofName().isPresent());

        fieldDescriptor.getOneofName().ifPresent(oneOfName ->
                template.add("oneOfName", StringUtils.capitalize(oneOfName)));

        if (fieldDescriptor.isMapField()) {
            FieldDescriptor value = fieldDescriptor.getContentMessage()
                    .map(descriptor -> ((MessageDescriptor) descriptor).getMapValue())
                    .orElseThrow(() -> new IllegalStateException("Content message not present in map field."));
            template.add("isMapMsg", value.getContentMessage().isPresent());
            template.add("mapValType", value.getTypeName());
        }

        return template.render();
    }
}
