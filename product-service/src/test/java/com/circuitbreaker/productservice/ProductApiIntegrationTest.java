package com.circuitbreaker.productservice;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class ProductApiIntegrationTest {

    private static final String VALID_BODY =
            "{\"name\":\"Webcam\",\"description\":\"1080p\",\"price\":59.90,\"category\":\"Accessories\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsSeededCatalogue() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].name", is("Laptop")));
    }

    @Test
    void filtersByCategoryCaseInsensitively() throws Exception {
        mockMvc.perform(get("/products").param("category", "accessories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void returns404ProblemDetailForUnknownProduct() throws Exception {
        mockMvc.perform(get("/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("Product 9999 not found")));
    }

    @Test
    void createsAndFetchesProduct() throws Exception {
        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name", is("Webcam")));
    }

    @Test
    void rejectsInvalidPayloadWithFieldErrors() throws Exception {
        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1,\"category\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.category").exists());
    }

    @Test
    void updatesAndDeletesProduct() throws Exception {
        mockMvc.perform(put("/products/2").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Webcam")));

        mockMvc.perform(delete("/products/8")).andExpect(status().isNoContent());
        mockMvc.perform(get("/products/8")).andExpect(status().isNotFound());
    }
}
