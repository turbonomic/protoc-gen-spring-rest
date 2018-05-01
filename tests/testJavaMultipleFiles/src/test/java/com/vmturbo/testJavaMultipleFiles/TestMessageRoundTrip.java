package com.vmturbo.testJavaMultipleFiles;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.vmturbo.protoc.spring.rest.testJavaMultipleFiles.JavaMultipleFilesTestREST;
import com.vmturbo.protoc.spring.rest.testJavaMultipleFiles.Msg1;
import com.vmturbo.protoc.spring.rest.testJavaMultipleFiles.Msg2;
import com.vmturbo.protoc.spring.rest.testJavaMultipleFiles.Msg2.NestedMsg;

public class TestMessageRoundTrip {

    @Test
    public void testMsg1() {
        final Msg1 startMsg = Msg1.newBuilder()
            .setStringContent("boo!")
            .build();

        final Msg1 endMsg = JavaMultipleFilesTestREST.Msg1.fromProto(startMsg).toProto();
        assertThat(endMsg, is(startMsg));
    }

    @Test
    public void testNestedMessage() {
        final Msg2 startMsg = Msg2.newBuilder()
                .setNumericContent(NestedMsg.newBuilder()
                        .setNumericContent(7L))
                .build();

        final Msg2 endMsg = JavaMultipleFilesTestREST.Msg2.fromProto(startMsg).toProto();
        assertThat(endMsg, is(startMsg));
    }
}
