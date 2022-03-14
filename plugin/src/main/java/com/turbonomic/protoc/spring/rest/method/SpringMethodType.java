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

import javax.annotation.Nonnull;

/**
 * Represents the various HTTP method types, and the associated
 * Spring enum.
 *
 * This only represents the method types supported by the protobuf compiler framework.
 */
enum SpringMethodType {
    POST("RequestMethod.POST"),
    PUT("RequestMethod.PUT"),
    PATCH("RequestMethod.PATCH"),
    GET("RequestMethod.GET"),
    DELETE("RequestMethod.DELETE");

    private final String springMethodType;

    SpringMethodType(final String springMethodType) {
        this.springMethodType = springMethodType;
    }

    @Nonnull
    public String getType() {
        return springMethodType;
    }
}
