package com.turbonomic.protoc.spring.rest.testProto3;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.turbonomic.protoc.spring.rest.testProto3.Proto3Test.EchoRequest;
import com.turbonomic.protoc.spring.rest.testProto3.Proto3Test.EchoRequest.SubMessage;

public class TestProto3Test {

    @Test
    public void testConversionEcho() {
        final EchoRequest echoRequest = EchoRequest.newBuilder()
                .setEcho("foo")
                .build();
        assertThat(Proto3TestREST.EchoRequest.fromProto(echoRequest).toProto(), is(echoRequest));
    }

    @Test
    public void testConversionSubmessage() {
        final EchoRequest echoRequest = EchoRequest.newBuilder()
                .setEchoSubmessage(SubMessage.newBuilder()
                        .setEcho("foo"))
                .build();
        assertThat(Proto3TestREST.EchoRequest.fromProto(echoRequest).toProto(), is(echoRequest));
    }

    @Test
    public void testConversionUnset() {
        final EchoRequest echoRequest = EchoRequest.newBuilder()
                .build();
        assertThat(Proto3TestREST.EchoRequest.fromProto(echoRequest).toProto(), is(echoRequest));
    }
}
