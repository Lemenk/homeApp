package com.familyhome;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通用测试支撑：登录、取 token、JSON 工具。
 */
public abstract class ApiTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String token(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login/phone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\",\"code\":\"123456\"}"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.path("data").path("token").asText();
    }

    protected String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    protected MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected MockHttpServletRequestBuilder putJson(String url, String body) {
        return put(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected MockHttpServletRequestBuilder getAuth(String url, String token) {
        return get(url).header("Authorization", "Bearer " + token);
    }
}
