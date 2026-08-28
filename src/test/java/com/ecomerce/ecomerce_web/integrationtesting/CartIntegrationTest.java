package com.ecomerce.ecomerce_web.integrationtesting;

import com.ecomerce.ecomerce_web.config.TestCacheConfig;
import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.CartItemRequestDto;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
class CartIntegrationTest {

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
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private String secondUserToken;
    private Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
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
        registerUserDirectly("second@example.com", "Passw0rd!", "USER");

        adminToken = loginAndGetToken("admin@example.com", "Passw0rd!");
        userToken = loginAndGetToken("user@example.com", "Passw0rd!");
        secondUserToken = loginAndGetToken("second@example.com", "Passw0rd!");

        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("desc");
        categoryId = categoryRepository.save(category).getId();
    }

    // ---------- Add to cart ----------

    @Test
    void addToCart_asUser_returns200AndAddsItem() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);

        CartItemRequestDto dto = new CartItemRequestDto();
        dto.setProductId(productId);
        dto.setQuantity(2);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void addToCart_existingItem_incrementsQuantity() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 10);

        CartItemRequestDto dto = new CartItemRequestDto();
        dto.setProductId(productId);
        dto.setQuantity(2);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(4));
    }

    @Test
    void addToCart_inactiveProduct_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5, false);

        CartItemRequestDto dto = new CartItemRequestDto();
        dto.setProductId(productId);
        dto.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addToCart_insufficientStock_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 1);

        CartItemRequestDto dto = new CartItemRequestDto();
        dto.setProductId(productId);
        dto.setQuantity(5);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addToCart_nonExistentProduct_returns404() throws Exception {
        CartItemRequestDto dto = new CartItemRequestDto();
        dto.setProductId(99999L);
        dto.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addToCart_noToken_returns403() throws Exception {
        CartItemRequestDto dto = new CartItemRequestDto();
        dto.setProductId(1L);
        dto.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ---------- Get cart ----------

    @Test
    void getCart_returnsUserOwnCart() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        addItemDirectlyToCart("user@example.com", productId, 2);

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getCart_newUser_returnsEmptyCart() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    // ---------- Update item ----------

    @Test
    void updateCartItem_asOwner_updatesQuantity() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 10);
        Long itemId = addItemDirectlyToCart("user@example.com", productId, 2);

        mockMvc.perform(put("/api/cart/update/" + itemId)
                        .header("Authorization", "Bearer " + userToken)
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void updateCartItem_insufficientStock_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 3);
        Long itemId = addItemDirectlyToCart("user@example.com", productId, 2);

        mockMvc.perform(put("/api/cart/update/" + itemId)
                        .header("Authorization", "Bearer " + userToken)
                        .param("quantity", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCartItem_belongsToAnotherUser_returns403() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 10);
        Long itemId = addItemDirectlyToCart("user@example.com", productId, 2);

        mockMvc.perform(put("/api/cart/update/" + itemId)
                        .header("Authorization", "Bearer " + secondUserToken)
                        .param("quantity", "3"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCartItem_belowMinimum_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 10);
        Long itemId = addItemDirectlyToCart("user@example.com", productId, 2);

        mockMvc.perform(put("/api/cart/update/" + itemId)
                        .header("Authorization", "Bearer " + userToken)
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Remove item ----------

    @Test
    void removeFromCart_asOwner_removesItem() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 10);
        Long itemId = addItemDirectlyToCart("user@example.com", productId, 2);

        mockMvc.perform(delete("/api/cart/remove/" + itemId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void removeFromCart_belongsToAnotherUser_returns403() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 10);
        Long itemId = addItemDirectlyToCart("user@example.com", productId, 2);

        mockMvc.perform(delete("/api/cart/remove/" + itemId)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeFromCart_nonExistentItem_returns404() throws Exception {
        mockMvc.perform(delete("/api/cart/remove/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // ---------- Clear cart ----------

    @Test
    void clearCart_removesAllItems() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 10);
        addItemDirectlyToCart("user@example.com", productId, 2);

        mockMvc.perform(delete("/api/cart/clear")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    // ---------- Checkout ----------

    @Test
    void checkout_withItems_createsOrderAndClearsCart() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 10);
        addItemDirectlyToCart("user@example.com", productId, 2);

        mockMvc.perform(post("/api/cart/checkout")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        assertThat(orderRepository.findAll()).hasSize(1);
    }

    @Test
    void checkout_emptyCart_returns400() throws Exception {
        mockMvc.perform(post("/api/cart/checkout")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_noToken_returns403() throws Exception {
        mockMvc.perform(post("/api/cart/checkout"))
                .andExpect(status().isForbidden());
    }

    // ---------- Helpers ----------

    private Long createProductDirectly(String name, BigDecimal price, int stock) {
        return createProductDirectly(name, price, stock, true);
    }

    private Long createProductDirectly(String name, BigDecimal price, int stock, boolean active) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("A perfectly ordinary description over 20 characters");
        product.setPrice(price);
        product.setStock(stock);
        product.setImg_url("https://example.com/img.jpg");
        product.setCreatedAt(LocalDateTime.now());
        product.setActive(active);
        product.setCategory(categoryRepository.findById(categoryId).orElseThrow());
        return productRepository.save(product).getId();
    }

    private Long addItemDirectlyToCart(String userEmail, Long productId, int quantity) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(user);
            return cartRepository.save(c);
        });
        Product product = productRepository.findById(productId).orElseThrow();

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setPriceAtAdding(product.getPrice());
        return cartItemRepository.save(item).getId();
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