package com.ecomerce.ecomerce_web.integrationtesting;
import com.ecomerce.ecomerce_web.config.TestCacheConfig;
import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.CategoryRequest;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
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
class CategoryIntegrationTest {

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

    @BeforeEach
    void setUp() throws Exception {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        registerUserDirectly("admin@example.com", "Passw0rd!", "ADMIN");
        registerUserDirectly("user@example.com", "Passw0rd!", "USER");

        adminToken = loginAndGetToken("admin@example.com", "Passw0rd!");
        userToken = loginAndGetToken("user@example.com", "Passw0rd!");
    }

    // ---------- Create ----------

    @Test
    void createCategory_asAdmin_returns201AndPersists() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");
        request.setDescription("Gadgets and devices");

        mockMvc.perform(post("/api/categories/create")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"));

        assertThat(categoryRepository.findByNameIgnoreCase("Electronics")).isPresent();
    }

    @Test
    void createCategory_duplicateNameCaseInsensitive_returns409() throws Exception {
        createCategoryDirectly("Electronics", "desc");

        CategoryRequest request = new CategoryRequest();
        request.setName("electronics"); // different casing, still a duplicate
        request.setDescription("Another description");

        mockMvc.perform(post("/api/categories/create")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createCategory_blankName_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("");
        request.setDescription("desc");

        mockMvc.perform(post("/api/categories/create")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createCategory_asRegularUser_returns403() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Books");
        request.setDescription("desc");

        mockMvc.perform(post("/api/categories/create")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategory_withNoToken_returns403() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Books");
        request.setDescription("desc");

        mockMvc.perform(post("/api/categories/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---------- Read (public) ----------

    @Test
    void getAll_noToken_returns200() throws Exception {
        createCategoryDirectly("Electronics", "desc");
        createCategoryDirectly("Books", "desc");

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAll_whenEmpty_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }


    @Test
    void getProductsByCategory_noToken_returnsPagedProducts() throws Exception {
        Long categoryId = createCategoryDirectly("Electronics", "desc");
        createProductDirectly("Widget A", categoryId, new BigDecimal("10.00"), 5);
        createProductDirectly("Widget B", categoryId, new BigDecimal("20.00"), 5);

        mockMvc.perform(get("/api/categories/" + categoryId + "/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getProductsByCategory_nonExistentCategory_returns404() throws Exception {
        mockMvc.perform(get("/api/categories/99999/products"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductsByCategory_respectsPageSize() throws Exception {
        Long categoryId = createCategoryDirectly("Electronics", "desc");
        for (int i = 0; i < 5; i++) {
            createProductDirectly("Widget " + i, categoryId, new BigDecimal("10.00"), 5);
        }

        mockMvc.perform(get("/api/categories/" + categoryId + "/products")
                        .param("size", "2")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }


    @Test
    void updateCategory_asAdmin_returns200AndUpdatesFields() throws Exception {
        Long categoryId = createCategoryDirectly("Old Name", "old desc");

        CategoryRequest request = new CategoryRequest();
        request.setName("New Name");
        request.setDescription("new desc");

        mockMvc.perform(put("/api/categories/update/" + categoryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("new desc"));
    }

    @Test
    void updateCategory_sameNameDifferentCase_doesNotConflictWithItself() throws Exception {
        Long categoryId = createCategoryDirectly("Electronics", "desc");

        CategoryRequest request = new CategoryRequest();
        request.setName("ELECTRONICS"); // same category, just different case
        request.setDescription("updated desc");

        mockMvc.perform(put("/api/categories/update/" + categoryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ELECTRONICS"));
    }

    @Test
    void updateCategory_toNameUsedByAnotherCategory_returns409() throws Exception {
        createCategoryDirectly("Books", "desc");
        Long categoryId = createCategoryDirectly("Electronics", "desc");

        CategoryRequest request = new CategoryRequest();
        request.setName("Books"); // collides with the other category
        request.setDescription("desc");

        mockMvc.perform(put("/api/categories/update/" + categoryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCategory_nonExistentId_returns404() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Whatever");
        request.setDescription("desc");

        mockMvc.perform(put("/api/categories/update/99999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCategory_asRegularUser_returns403() throws Exception {
        Long categoryId = createCategoryDirectly("Electronics", "desc");

        CategoryRequest request = new CategoryRequest();
        request.setName("New Name");
        request.setDescription("desc");

        mockMvc.perform(put("/api/categories/update/" + categoryId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    private Long createCategoryDirectly(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category).getId();
    }

    private Long createProductDirectly(String name, Long categoryId, BigDecimal price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("A perfectly ordinary description over 20 characters");
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