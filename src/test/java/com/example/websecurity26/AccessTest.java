package com.example.websecurity26;

import com.example.websecurity26.model.Item;
import com.example.websecurity26.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AccessTest {

    private static final String ITEMS = "/api/v1/items";
    private static final String BODY = "{\"name\":\"Test\",\"description\":\"junit\"}";

    @Autowired WebApplicationContext context;
    @MockitoBean ItemService itemService;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Item item = new Item();
        item.setId("1");
        item.setName("Test");
        when(itemService.findAll()).thenReturn(List.of(item));
        when(itemService.findById(anyString())).thenReturn(item);
        when(itemService.create(any())).thenReturn(item);
        when(itemService.update(anyString(), any())).thenReturn(item);
    }

    @Test
    @DisplayName("1. Anonymous GET /items -> 401")
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get(ITEMS)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Wrong password -> 401")
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(get(ITEMS).with(httpBasic("admin", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. USER GET /items -> 200")
    void userCanReadAll() throws Exception {
        mvc.perform(get(ITEMS).with(httpBasic("user", "user"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. USER GET /items/1 -> 200")
    void userCanReadOne() throws Exception {
        mvc.perform(get(ITEMS + "/1").with(httpBasic("user", "user"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("5. USER POST /items -> 403")
    void userCannotCreate() throws Exception {
        mvc.perform(post(ITEMS).with(httpBasic("user", "user")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. MANAGER POST /items -> 201")
    void managerCanCreate() throws Exception {
        mvc.perform(post(ITEMS).with(httpBasic("manager", "manager")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("7. MANAGER PUT /items/1 -> 200")
    void managerCanUpdate() throws Exception {
        mvc.perform(put(ITEMS + "/1").with(httpBasic("manager", "manager")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("8. MANAGER DELETE /items/1 -> 403")
    void managerCannotDelete() throws Exception {
        mvc.perform(delete(ITEMS + "/1").with(httpBasic("manager", "manager")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. ADMIN DELETE /items/1 -> 204")
    void adminCanDelete() throws Exception {
        mvc.perform(delete(ITEMS + "/1").with(httpBasic("admin", "admin")).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("10. ADMIN POST without CSRF token -> 403")
    void postWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post(ITEMS).with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("11. @Secured admin-zone as USER -> 403")
    void securedAdminZoneDeniedForUser() throws Exception {
        mvc.perform(get("/api/v1/annotated/admin-zone")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("12. @Secured admin-zone as ADMIN -> 200")
    void securedAdminZoneAllowedForAdmin() throws Exception {
        mvc.perform(get("/api/v1/annotated/admin-zone")).andExpect(status().isOk());
    }
}
