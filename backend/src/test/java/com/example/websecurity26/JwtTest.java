package com.example.websecurity26;

import com.example.websecurity26.service.ItemService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class JwtTest {

    @Autowired WebApplicationContext context;
    @MockitoBean ItemService itemService;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String token(String email, String password) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    @Test
    @DisplayName("JWT 1. Login with valid credentials returns token")
    void loginReturnsToken() throws Exception {
        mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@mail.com\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("JWT 2. Login with wrong password -> 401")
    void loginWrongPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@mail.com\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT 3. USER token: GET items 200, POST items 403")
    void userTokenAccess() throws Exception {
        String t = token("user@mail.com", "user");
        mvc.perform(get("/api/v1/items").header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/items").header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("JWT 4. ADMIN token: admin-zone 200")
    void adminTokenAccess() throws Exception {
        String t = token("admin@mail.com", "admin");
        mvc.perform(get("/api/v1/annotated/admin-zone").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("JWT 5. Invalid token -> 401")
    void invalidToken() throws Exception {
        mvc.perform(get("/api/v1/items").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }
}
