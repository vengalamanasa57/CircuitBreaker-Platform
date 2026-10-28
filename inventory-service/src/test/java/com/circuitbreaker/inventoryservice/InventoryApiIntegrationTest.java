package com.circuitbreaker.inventoryservice;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "management.tracing.enabled=false"
})
@AutoConfigureMockMvc
@Transactional // MockMvc runs on the test thread, so every test rolls back and stays independent
class InventoryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsSeededStock() throws Exception {
        mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));
    }

    @Test
    void reportsOutOfStockProducts() throws Exception {
        mockMvc.perform(get("/inventory/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inStock", is(false)));
    }

    @Test
    void returns404ForUnknownProduct() throws Exception {
        mockMvc.perform(get("/inventory/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reserveDecrementsStock() throws Exception {
        mockMvc.perform(post("/inventory/1/reserve").param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(20)));
    }

    @Test
    void reserveBeyondStockIsRejectedWithConflict() throws Exception {
        mockMvc.perform(post("/inventory/5/reserve").param("quantity", "100"))
                .andExpect(status().isConflict());
    }

    @Test
    void setQuantityValidatesInput() throws Exception {
        mockMvc.perform(put("/inventory/1").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-3}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/inventory/1").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(7)));
    }
}
