package com.turbonomic.protoc.spring.rest.test.echo;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.turbonomic.protoc.spring.rest.testServices.EchoREST;
import com.turbonomic.protoc.spring.rest.testServices.EchoREST.EchoServiceController;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(loader = AnnotationConfigWebContextLoader.class)
// Need clean context with no probes/targets registered.
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class EchoServiceErrorsTest extends AbstractEchoServiceTest {
    /**
     * Nested configuration for Spring context.
     */
    @Configuration
    @EnableWebMvc
    static class ContextConfiguration extends WebMvcConfigurerAdapter {

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            GsonHttpMessageConverter msgConverter = new GsonHttpMessageConverter();
            msgConverter.setGson(new Gson());
            converters.add(msgConverter);
        }

        @Bean
        public ErrorEchoService testService() {
            return Mockito.spy(new ErrorEchoService());
        }

        @Bean
        public EchoServiceController restController() {
            return new EchoServiceController(testService());
        }

    }

    /**
     * Test that the server behaves properly when the method throws an exception.
     * @throws Exception
     */
    @Test
    public void testEchoException() throws Exception {
        Mockito.doThrow(new IllegalStateException("RuntimeException.")).when(testService).echo(Mockito.any(), Mockito.any());

        EchoREST.EchoServiceController.EchoServiceResponse<EchoREST.EchoResponse> response =
                parseEchoResponse(postAndExpect("/EchoService/echo", gson.toJson(inputRequest1), HttpStatus.INTERNAL_SERVER_ERROR));
        Assert.assertNotNull(response.error);
        Assert.assertNull(response.response);
    }

    @Test
    public void testEchoServerStreamException() throws Exception {
        Mockito.doThrow(new IllegalStateException("RuntimeException.")).when(testService).serverStreamEcho(Mockito.any(), Mockito.any());

        EchoREST.EchoServiceController.EchoServiceResponse<EchoREST.EchoResponse> response =
                parseEchoResponse(postAndExpect("/EchoService/serverStreamEcho", gson.toJson(inputRequest1), HttpStatus.INTERNAL_SERVER_ERROR));
        Assert.assertNotNull(response.error);
        Assert.assertNull(response.response);
    }

    @Test
    public void testEchoClientStreamException() throws Exception {
        Mockito.doThrow(new IllegalStateException("RuntimeException.")).when(testService).clientStreamEcho(Mockito.any());

        List<EchoREST.EchoRequest> requests = ImmutableList.of(inputRequest1, inputRequest2);
        EchoREST.EchoServiceController.EchoServiceResponse<EchoREST.EchoResponse> response =
                parseEchoResponse(postAndExpect("/EchoService/clientStreamEcho", gson.toJson(requests), HttpStatus.INTERNAL_SERVER_ERROR));

        Mockito.verify(testService).clientStreamEcho(Mockito.any());
        Assert.assertNotNull(response.error);
        Assert.assertNull(response.response);
    }

    @Test
    public void testEchoBiStreamException() throws Exception {
        Mockito.doThrow(new IllegalStateException("RuntimeException.")).when(testService).biStreamEcho(Mockito.any());
        List<EchoREST.EchoRequest> requests = ImmutableList.of(inputRequest1, inputRequest2);
        EchoREST.EchoServiceController.EchoServiceResponse<List<EchoREST.EchoResponse>> responses =
                parseListEchoResponse(postAndExpect("/EchoService/biStreamEcho", gson.toJson(requests), HttpStatus.INTERNAL_SERVER_ERROR));

        Mockito.verify(testService).biStreamEcho(Mockito.any());

        Assert.assertNotNull(responses.error);
        Assert.assertNull(responses.response);
    }

    @Test
    public void testEchoError() throws Exception {
        EchoREST.EchoServiceController.EchoServiceResponse<EchoREST.EchoResponse> response =
                parseEchoResponse(postAndExpect("/EchoService/echo", gson.toJson(inputRequest1), HttpStatus.INTERNAL_SERVER_ERROR));
        Assert.assertNotNull(response.error);
        Assert.assertNull(response.response);
    }

    @Test
    public void testEchoServerStreamError() throws Exception {
        EchoREST.EchoServiceController.EchoServiceResponse<List<EchoREST.EchoResponse>> responses =
                parseListEchoResponse(postAndExpect("/EchoService/serverStreamEcho", gson.toJson(inputRequest1), HttpStatus.INTERNAL_SERVER_ERROR));

        Mockito.verify(testService).serverStreamEcho(Mockito.any(), Mockito.any());

        Assert.assertNotNull(responses.error);
        Assert.assertNull(responses.response);
    }

    @Test
    public void testEchoClientStreamError() throws Exception {
        List<EchoREST.EchoRequest> requests = ImmutableList.of(inputRequest1, inputRequest2);
        EchoREST.EchoServiceController.EchoServiceResponse<EchoREST.EchoResponse> response =
                parseEchoResponse(postAndExpect("/EchoService/clientStreamEcho", gson.toJson(requests), HttpStatus.INTERNAL_SERVER_ERROR));

        Mockito.verify(testService).clientStreamEcho(Mockito.any());
        Assert.assertNotNull(response.error);
        Assert.assertNull(response.response);
    }

    @Test
    public void testEchoBiStreamError() throws Exception {
        List<EchoREST.EchoRequest> requests = ImmutableList.of(inputRequest1, inputRequest2);
        EchoREST.EchoServiceController.EchoServiceResponse<List<EchoREST.EchoResponse>> responses =
                parseListEchoResponse(postAndExpect("/EchoService/biStreamEcho", gson.toJson(requests), HttpStatus.INTERNAL_SERVER_ERROR));

        Mockito.verify(testService).biStreamEcho(Mockito.any());

        Assert.assertNotNull(responses.error);
        Assert.assertNull(responses.response);
    }
}

