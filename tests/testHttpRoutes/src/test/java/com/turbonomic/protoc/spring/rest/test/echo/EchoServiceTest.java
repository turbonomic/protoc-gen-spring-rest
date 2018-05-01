package com.turbonomic.protoc.spring.rest.test.echo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTest.Echo;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTestREST;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTestREST.DeleteEchoResponse;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTestREST.EchoServiceController;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTestREST.EchoServiceController.EchoServiceResponse;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTestREST.GetEchoResponse;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTestREST.MultiGetEchoResponse;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTestREST.NewEchoResponse;
import com.turbonomic.protoc.spring.rest.testHttpRoutes.HttpRoutesTestREST.UpdateEchoResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(loader = AnnotationConfigWebContextLoader.class)
// Need clean context with no probes/targets registered.
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class EchoServiceTest {
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
        public NormalEchoService testService() {
            return Mockito.spy(new NormalEchoService());
        }

        @Bean
        public EchoServiceController echoServiceController() {
            return new EchoServiceController(testService());
        }
    }

    private static MockMvc mockMvc;

    private NormalEchoService echoService;

    @Autowired
    private WebApplicationContext wac;

    private Gson gson = new Gson();

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        echoService = wac.getBean(NormalEchoService.class);
    }

    @Test
    public void testEchoGet() throws Exception {
        final Echo echo = Echo.newBuilder()
                .setId(1)
                .setContent("HUH")
                .build();
        echoService.getEchoMap().put(1L, echo);

        final MvcResult result = mockMvc.perform(get("/echo/1")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();
        final EchoServiceController.EchoServiceResponse<GetEchoResponse> response =
                parseGetEchoResponse(result.getResponse().getContentAsString());

        Mockito.verify(echoService).getEcho(Mockito.any(), Mockito.any());

        Assert.assertNotNull(response.response);
        assertThat(response.response.toProto(), is(HttpRoutesTest.GetEchoResponse.newBuilder()
                .setEcho(echo)
                .build()));
    }

    @Test
    public void testEchoMultiGet() throws Exception {
        final Echo echo1 = Echo.newBuilder()
                .setId(1)
                .setContent("WAR")
                .build();
        final Echo echo2 = Echo.newBuilder()
                .setId(2)
                .setContent("HUH")
                .build();
        echoService.getEchoMap().put(echo1.getId(), echo1);
        echoService.getEchoMap().put(echo2.getId(), echo2);

        final MvcResult result = mockMvc.perform(get("/echo")
                .param("id", Long.toString(echo1.getId()))
                .param("id", Long.toString(echo2.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();
        final EchoServiceController.EchoServiceResponse<MultiGetEchoResponse> response =
                parseMultiGetEchoResponse(result.getResponse().getContentAsString());

        Mockito.verify(echoService).multiGetEcho(Mockito.any(), Mockito.any());

        Assert.assertNotNull(response.response);
        assertThat(response.response.toProto().getEchoList(), containsInAnyOrder(echo1, echo2));
    }

    @Test
    public void testEchoDelete() throws Exception {
        final Echo echo = Echo.newBuilder()
                .setId(1)
                .setContent("HUH")
                .build();
        echoService.getEchoMap().put(1L, echo);
        final MvcResult result = mockMvc.perform(delete("/echo/1")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();
        final EchoServiceResponse<DeleteEchoResponse> response =
                parseDeleteEchoResponse(result.getResponse().getContentAsString());

        Mockito.verify(echoService).deleteEcho(Mockito.any(), Mockito.any());

        Assert.assertNotNull(response.response);
        assertThat(response.response.toProto().getEcho(), is(echo));
        Assert.assertFalse(echoService.getEchoMap().containsKey(echo.getId()));
   }

   @Test
   public void testNewEcho() throws Exception {
       final Echo echo = Echo.newBuilder()
               .setId(1)
               .setContent("HUH")
               .build();
       final MvcResult result = mockMvc.perform(post("/echo")
               .content(gson.toJson(HttpRoutesTestREST.Echo.fromProto(echo)))
               .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
               .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(status().is(HttpStatus.OK.value()))
               .andReturn();
       final EchoServiceResponse<NewEchoResponse> response =
               parseNewEchoResponse(result.getResponse().getContentAsString());

       Mockito.verify(echoService).newEcho(Mockito.any(), Mockito.any());

       Assert.assertNotNull(response.response);
       assertThat(response.response.toProto().getEcho(), is(echo));
       assertThat(echoService.getEchoMap().get(echo.getId()), is(echo));
   }

    @Test
    public void testNewEchoAlternateBinding() throws Exception {
        final Echo echo = Echo.newBuilder()
                .setId(1)
                .setContent("HUH")
                .build();
        final MvcResult result = mockMvc.perform(post("/echo/" + echo.getId())
                .content("{ \"content\" : \"" + echo.getContent() + "\" }")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();
        final EchoServiceResponse<NewEchoResponse> response =
                parseNewEchoResponse(result.getResponse().getContentAsString());

        Mockito.verify(echoService).newEcho(Mockito.any(), Mockito.any());

        Assert.assertNotNull(response.response);
        assertThat(response.response.toProto().getEcho(), is(echo));
        assertThat(echoService.getEchoMap().get(echo.getId()), is(echo));
    }

    @Test
    public void testUpdateEchoPut() throws Exception {
        final Echo echo = Echo.newBuilder()
                .setId(1)
                .setContent("HUH")
                .build();
        final Echo newEcho = Echo.newBuilder(echo)
                .setContent("YEAH")
                .build();
        echoService.getEchoMap().put(1L, echo);
        final MvcResult result = mockMvc.perform(put("/echo/" + echo.getId())
                .content(gson.toJson(HttpRoutesTestREST.Echo.fromProto(newEcho)))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();
        final EchoServiceResponse<UpdateEchoResponse> response =
                parseUpdateEchoResponse(result.getResponse().getContentAsString());

        Mockito.verify(echoService).updateEcho(Mockito.any(), Mockito.any());

        Assert.assertNotNull(response.response);
        assertThat(response.response.toProto().getEcho(), is(newEcho));
        assertThat(echoService.getEchoMap().get(echo.getId()), is(newEcho));
    }

    @Test
    public void testUpdateEchoPatch() throws Exception {
        final Echo echo = Echo.newBuilder()
                .setId(1)
                .setContent("HUH")
                .build();
        final Echo newEcho = Echo.newBuilder(echo)
                .setContent("YEAH")
                .build();
        echoService.getEchoMap().put(1L, echo);
        final MvcResult result = mockMvc.perform(patch("/echo/" + echo.getId())
                .content(gson.toJson(HttpRoutesTestREST.Echo.fromProto(newEcho)))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn();
        final EchoServiceResponse<UpdateEchoResponse> response =
                parseUpdateEchoResponse(result.getResponse().getContentAsString());

        Mockito.verify(echoService).updateEcho(Mockito.any(), Mockito.any());

        Assert.assertNotNull(response.response);
        assertThat(response.response.toProto().getEcho(), is(newEcho));
        assertThat(echoService.getEchoMap().get(echo.getId()), is(newEcho));
    }

    protected EchoServiceResponse<GetEchoResponse> parseGetEchoResponse(String serializedJson) {
        return gson.fromJson(serializedJson, new TypeToken<EchoServiceResponse<GetEchoResponse>>(){}.getType());
    }

    protected EchoServiceResponse<MultiGetEchoResponse> parseMultiGetEchoResponse(String serializedJson) {
        return gson.fromJson(serializedJson, new TypeToken<EchoServiceResponse<MultiGetEchoResponse>>(){}.getType());
    }

    private EchoServiceResponse<DeleteEchoResponse> parseDeleteEchoResponse(String serializedJson) {
        return gson.fromJson(serializedJson, new TypeToken<EchoServiceResponse<DeleteEchoResponse>>(){}.getType());
    }

    private EchoServiceResponse<NewEchoResponse> parseNewEchoResponse(String serializedJson) {
        return gson.fromJson(serializedJson, new TypeToken<EchoServiceResponse<NewEchoResponse>>(){}.getType());
    }

    protected EchoServiceResponse<UpdateEchoResponse> parseUpdateEchoResponse(String serializedJson) {
        return gson.fromJson(serializedJson, new TypeToken<EchoServiceResponse<UpdateEchoResponse>>(){}.getType());
    }
}
