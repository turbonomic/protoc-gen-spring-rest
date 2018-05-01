package com.turbonomic.protoc.spring.rest.test.echo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.turbonomic.protoc.spring.rest.testServices.Echo;
import com.turbonomic.protoc.spring.rest.testServices.EchoREST;
import com.turbonomic.protoc.spring.rest.testServices.EchoREST.EchoResponse;
import com.turbonomic.protoc.spring.rest.testServices.EchoREST.EchoServiceController.EchoServiceResponse;
import com.turbonomic.protoc.spring.rest.testServices.EchoServiceGrpc.EchoServiceImplBase;

public class AbstractEchoServiceTest {
    protected static MockMvc mockMvc;

    protected EchoServiceImplBase testService;

    @Autowired
    protected WebApplicationContext wac;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        testService = wac.getBean(EchoServiceImplBase.class);
    }


    protected Gson gson = new Gson();

    protected final String ECHO_STR = "echoThis1";

    protected final EchoREST.EchoRequest inputRequest1 = EchoREST.EchoRequest
            .fromProto(Echo.EchoRequest.newBuilder()
                    .setEchoThis("echoThis1")
                    .setExtraOptional("extraOptional1")
                    .build());

    protected final String inputJson1 = "{ \"echoThis\" : \"" + ECHO_STR + "\" }";

    protected final EchoREST.EchoRequest inputRequest2 = EchoREST.EchoRequest
            .fromProto(Echo.EchoRequest.newBuilder()
                    .setEchoThis("echoThis2")
                    .setExtraOptional("extraOptional2")
                    .build());

    protected String postAndExpect(String route,
                                 String serializedRequest,
                                 HttpStatus expectedStatus) throws Exception {
        final MvcResult result = mockMvc.perform(post(route)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(serializedRequest)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    protected EchoREST.EchoServiceController.EchoServiceResponse<EchoREST.EchoResponse> parseEchoResponse(String serializedJson) {
        return gson.fromJson(serializedJson,
                new TypeToken<EchoServiceResponse<EchoResponse>>(){}.getType());
    }

    protected EchoREST.EchoServiceController.EchoServiceResponse<List<EchoResponse>> parseListEchoResponse(String serializedJson) {
        return gson.fromJson(serializedJson,
                new TypeToken<EchoREST.EchoServiceController.EchoServiceResponse<List<EchoREST.EchoResponse>>>(){}.getType());
    }
}
