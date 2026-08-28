package com.ecomerce.ecomerce_web.integrationtesting;

import com.ecomerce.ecomerce_web.config.TestCacheConfig;
import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.ProductRequestDto;
import com.ecomerce.ecomerce_web.dtos.StockUpdateRequestDto;
import com.ecomerce.ecomerce_web.entity.Category;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.repository.CategoryRepository;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.repository.RoleRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.autoconfigure.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestMailConfig.class, TestCacheConfig.class})
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        productRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        Category category = new Category();
        category.setName("Electronics");
        categoryId = categoryRepository.save(category).getId();

        registerUserDirectly("admin@example.com", "Passw0rd!", "ADMIN");
        registerUserDirectly("user@example.com", "Passw0rd!", "USER");

        adminToken = loginAndGetToken("admin@example.com", "Passw0rd!");
        userToken = loginAndGetToken("user@example.com", "Passw0rd!");
    }

    // ---------- Create ----------

    @Test
    void createProduct_asAdmin_returns200AndPersists() throws Exception {
        ProductRequestDto dto = validProductDto();

        mockMvc.perform(post("/api/products/createProduct")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.categoryId").value(categoryId));

        assertThat(productRepository.findByActiveTrue()).hasSize(1);
    }

    @Test
    void createProduct_asRegularUser_returns403() throws Exception {
        mockMvc.perform(post("/api/products/createProduct")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_withNoToken_returns403() throws Exception {
        mockMvc.perform(post("/api/products/createProduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_withBlankName_returns400() throws Exception {
        ProductRequestDto dto = validProductDto();
        dto.setName("");

        mockMvc.perform(post("/api/products/createProduct")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createProduct_withShortDescription_returns400() throws Exception {
        ProductRequestDto dto = validProductDto();
        dto.setDescription("too short");

        mockMvc.perform(post("/api/products/createProduct")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    void createProduct_withInvalidCategoryId_returns404() throws Exception {
        ProductRequestDto dto = validProductDto();
        dto.setCategoryId(99999L);

        mockMvc.perform(post("/api/products/createProduct")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ---------- Update ----------

    @Test
    void updateProduct_asAdmin_returns200AndUpdatesFields() throws Exception {
        Long productId = createProductDirectly("Old Name", "This is a description over 20 chars", new BigDecimal("10.00"), 5);

        ProductRequestDto dto = validProductDto();
        dto.setName("New Name");

        mockMvc.perform(put("/api/products/updateProduct/" + productId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void updateProduct_nonExistentId_returns404() throws Exception {
        mockMvc.perform(put("/api/products/updateProduct/99999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductDto())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_asRegularUser_returns403() throws Exception {
        Long productId = createProductDirectly("Name", "This is a description over 20 chars", new BigDecimal("10.00"), 5);

        mockMvc.perform(put("/api/products/updateProduct/" + productId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProductDto())))
                .andExpect(status().isForbidden());
    }

    // ---------- Read (public) ----------

    @Test
    void getAllProducts_noToken_returns200() throws Exception {
        createProductDirectly("A", "This is a description over 20 chars", new BigDecimal("5.00"), 3);
        createProductDirectly("B", "This is a description over 20 chars", new BigDecimal("7.00"), 3);

        mockMvc.perform(get("/api/products/AllProducts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getById_existingProduct_returns200() throws Exception {
        Long productId = createProductDirectly("Widget", "This is a description over 20 chars", new BigDecimal("15.00"), 8);

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Widget"));
    }

    @Test
    void getById_nonExistentProduct_returns404() throws Exception {
        mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound());
    }

    // ---------- Delete (soft delete) ----------

    @Test
    void deleteProduct_asAdmin_softDeletesAndExcludesFromGetAll() throws Exception {
        Long productId = createProductDirectly("ToDelete", "This is a description over 20 chars", new BigDecimal("9.00"), 4);

        mockMvc.perform(delete("/api/products/delete/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Product deleted = productRepository.findById(productId).orElseThrow();
        assertThat(deleted.isActive()).isFalse();

        mockMvc.perform(get("/api/products/AllProducts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteProduct_asRegularUser_returns403() throws Exception {
        Long productId = createProductDirectly("Name", "This is a description over 20 chars", new BigDecimal("9.00"), 4);

        mockMvc.perform(delete("/api/products/delete/" + productId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }


    @Test
    void searchProducts_byName_returnsMatch() throws Exception {
        createProductDirectly("Bluetooth Speaker", "This is a description over 20 chars", new BigDecimal("30.00"), 5);
        createProductDirectly("Wired Headphones", "This is a description over 20 chars", new BigDecimal("15.00"), 5);

        mockMvc.perform(get("/api/products/search").param("name", "Bluetooth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Bluetooth Speaker"));
    }

    @Test
    void searchProducts_withTooLongName_returns400() throws Exception {
        String longName = "a".repeat(101);

        mockMvc.perform(get("/api/products/search").param("name", longName))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchProducts_withInvalidSortField_returns400() throws Exception {
        mockMvc.perform(get("/api/products/search").param("sort", "notARealField"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filterProducts_byPriceRange_returnsMatches() throws Exception {
        createProductDirectly("Cheap", "This is a description over 20 chars", new BigDecimal("5.00"), 5);
        createProductDirectly("Mid", "This is a description over 20 chars", new BigDecimal("50.00"), 5);
        createProductDirectly("Expensive", "This is a description over 20 chars", new BigDecimal("500.00"), 5);

        mockMvc.perform(get("/api/products/filter")
                        .param("minPrice", "10")
                        .param("maxPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Mid"));
    }

    @Test
    void filterProducts_negativeMinPrice_returns400() throws Exception {
        mockMvc.perform(get("/api/products/filter").param("minPrice", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filterProducts_minGreaterThanMax_returns400() throws Exception {
        mockMvc.perform(get("/api/products/filter")
                        .param("minPrice", "100")
                        .param("maxPrice", "10"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Stock ----------

    @Test
    void updateStock_asAdmin_updatesValue() throws Exception {
        Long productId = createProductDirectly("Stocked", "This is a description over 20 chars", new BigDecimal("20.00"), 2);

        StockUpdateRequestDto stockDto = new StockUpdateRequestDto();
        stockDto.setStock(50);

        mockMvc.perform(put("/api/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(50));
    }

    @Test
    void updateStock_asRegularUser_returns403() throws Exception {
        Long productId = createProductDirectly("Stocked", "This is a description over 20 chars", new BigDecimal("20.00"), 2);

        StockUpdateRequestDto stockDto = new StockUpdateRequestDto();
        stockDto.setStock(50);

        mockMvc.perform(put("/api/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLowStockProducts_asAdmin_returnsOnlyBelowThreshold() throws Exception {
        createProductDirectly("LowStock", "This is a description over 20 chars", new BigDecimal("5.00"), 3);
        createProductDirectly("HighStock", "This is a description over 20 chars", new BigDecimal("5.00"), 100);

        mockMvc.perform(get("/api/products/low-stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("LowStock"));
    }

    @Test
    void getLowStockProducts_asRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/products/low-stock")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }


    private ProductRequestDto validProductDto() {
        ProductRequestDto dto = new ProductRequestDto();
        dto.setName("Wireless Mouse");
        dto.setDescription("A comfortable wireless mouse with long battery life");
        dto.setPrice(new BigDecimal("25.99"));
        dto.setStock(20);
        dto.setImg_url("https://example.com/mouse.jpg");
        dto.setCategoryId(categoryId);
        return dto;
    }

    private Long createProductDirectly(String name, String description, BigDecimal price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setImg_url("https://example.com/img.jpg");
        product.setCreatedAt(LocalDateTime.now());
        product.setActive(true);
        product.setCategory(categoryRepository.findById(categoryId).orElseThrow());
        return productRepository.save(product).getId();
    }

    private User registerUserDirectly(String email, String rawPassword, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = User.builder()
                .fullName("Test User")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .emailVerified(true)
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}