package com.monteastur.envios.controller;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TestExceptionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @ParameterizedTest
    @CsvSource({
        "/test/exception/resource-not-found, 404, Not Found",
        "/test/exception/bad-request,         400, Bad Request",
        "/test/exception/conflict,            409, Conflict",
        "/test/exception/illegal-argument,     400, Bad Request",
        "/test/exception/illegal-state,        409, Conflict",
        "/test/exception/date-time-parse,      400, Bad Request",
        "/test/exception/generic,              500, Internal Server Error"
    })
    void mvcPath_returnsCorrectViewAndModel(String url, int status, String error) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is(status))
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", status))
                .andExpect(model().attribute("error", error))
                .andExpect(model().attributeExists("message", "timestamp"));
    }

    @ParameterizedTest
    @CsvSource({
        "/en/test/exception/resource-not-found",
        "/en/test/exception/bad-request",
        "/en/test/exception/conflict",
        "/en/test/exception/illegal-argument",
        "/en/test/exception/illegal-state",
        "/en/test/exception/date-time-parse",
        "/en/test/exception/generic"
    })
    void mvcPath_enLocale_returnsEnErrorView(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(view().name("en/error"))
                .andExpect(model().attributeExists("status", "error", "message", "timestamp"));
    }

    @ParameterizedTest
    @CsvSource({
        "/api/test/exception/generic,          500, Error interno del servidor"
    })
    void restPath_genericHandler_works(String url, int status, String error) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error));
    }
}
