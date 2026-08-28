package com.ecomerce.ecomerce_web.integrationtesting;
import com.ecomerce.ecomerce_web.config.TestCacheConfig;
import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.OrderItemRequestDto;
import com.ecomerce.ecomerce_web.dtos.OrderRequestDto;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.repository.*;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestMailConfig.class, TestCacheConfig.class})
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private String otherUserToken;
    private Long userId;
    private Long otherUserId;
    private Long adminUserId;
    private Long productId1;
    private Long productId2;
    private Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();
        roleRepository.deleteAll();

        // Setup roles
        Role userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        // Setup category
        Category category = new Category();
        category.setName("Electronics");
        categoryId = categoryRepository.save(category).getId();

        // Register users
        User admin = registerUserDirectly("admin@example.com", "Passw0rd!", "ADMIN");
        adminUserId = admin.getId();

        User user = registerUserDirectly("user@example.com", "Passw0rd!", "USER");
        userId = user.getId();

        User otherUser = registerUserDirectly("otheruser@example.com", "Passw0rd!", "USER");
        otherUserId = otherUser.getId();

        // Get tokens
        adminToken = loginAndGetToken("admin@example.com", "Passw0rd!");
        userToken = loginAndGetToken("user@example.com", "Passw0rd!");
        otherUserToken = loginAndGetToken("otheruser@example.com", "Passw0rd!");

        // Create products
        productId1 = createProductDirectly("Laptop", "High-performance laptop", new BigDecimal("999.99"), 10);
        productId2 = createProductDirectly("Mouse", "Wireless mouse", new BigDecimal("29.99"), 50);
    }

    // ========== CREATE ORDER ==========

    @Test
    void createOrder_byUser_withValidItems_returns200AndPersists() throws Exception {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId(null); // User creates for themselves
        dto.setItems(List.of(
                createOrderItemDto(productId1, 1),
                createOrderItemDto(productId2, 2)
        ));

        MvcResult result = mockMvc.perform(post("/api/orders/createOrder")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value("1059.97"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn();

        // Verify order persisted
        assertThat(orderRepository.findByUserId(userId)).hasSize(1);

        // Verify stock decreased
        Product laptop = productRepository.findById(productId1).orElseThrow();
        assertThat(laptop.getStock()).isEqualTo(9);

        Product mouse = productRepository.findById(productId2).orElseThrow();
        assertThat(mouse.getStock()).isEqualTo(48);
    }

    @Test
    void createOrder_byAdmin_forSpecificUser_returns200() throws Exception {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId(otherUserId); // Admin creates for another user
        dto.setItems(List.of(createOrderItemDto(productId1, 1)));

        mockMvc.perform(post("/api/orders/createOrder")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(otherUserId));

        assertThat(orderRepository.findByUserId(otherUserId)).hasSize(1);
    }

    @Test
    void createOrder_withoutAuthentication_returns403() throws Exception {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId(null);
        dto.setItems(List.of(createOrderItemDto(productId1, 1)));

        mockMvc.perform(post("/api/orders/createOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOrder_withEmptyItems_returns400() throws Exception {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId(null);
        dto.setItems(new ArrayList<>());

        mockMvc.perform(post("/api/orders/createOrder")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_withNonExistentProduct_returns404() throws Exception {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId(null);
        dto.setItems(List.of(new OrderItemRequestDto(99999L, 1)));

        mockMvc.perform(post("/api/orders/createOrder")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_withInsufficientStock_returns400() throws Exception {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId(null);
        dto.setItems(List.of(createOrderItemDto(productId1, 100))); // Only 10 in stock

        mockMvc.perform(post("/api/orders/createOrder")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Stock Is Not Enough")));
    }

    @Test
    void createOrder_byAdmin_forNonExistentUser_returns404() throws Exception {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setUserId(99999L);
        dto.setItems(List.of(createOrderItemDto(productId1, 1)));

        mockMvc.perform(post("/api/orders/createOrder")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ========== GET ALL ORDERS ==========

    @Test
    void getAllOrders_byAdmin_returns200WithAllOrders() throws Exception {
        createOrderDirectly(userId, productId1, 1);
        createOrderDirectly(otherUserId, productId2, 2);

        mockMvc.perform(get("/api/orders/AllOrders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllOrders_byRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/AllOrders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllOrders_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/AllOrders"))
                .andExpect(status().isForbidden());
    }

    // ========== GET ORDERS BY USER ==========

    @Test
    void getOrdersByUser_byAdmin_returns200() throws Exception {
        createOrderDirectly(userId, productId1, 1);
        createOrderDirectly(userId, productId2, 2);
        createOrderDirectly(otherUserId, productId1, 1);

        mockMvc.perform(get("/api/orders/user/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void getOrdersByUser_byRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/user/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrdersByUser_forNonExistentUser_returns200WithEmptyList() throws Exception {
        mockMvc.perform(get("/api/orders/user/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== GET ORDER BY ID ==========

    @Test
    void getOrderById_byOrderOwner_returns200() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(get("/api/orders/order/" + orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void getOrderById_byAdmin_returns200() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(get("/api/orders/order/" + orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void getOrderById_byNonOwnerUser_returns403() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(get("/api/orders/order/" + orderId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("Access Denied")));
    }

    @Test
    void getOrderById_nonExistentOrder_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/order/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderById_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/order/1"))
                .andExpect(status().isForbidden());
    }

    // ========== UPDATE ORDER STATUS ==========

    @Test
    void updateOrderStatus_byAdmin_returns200AndUpdates() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        Order updated = orderRepository.findById(orderId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Status.SHIPPED);
    }

    @Test
    void updateOrderStatus_byRegularUser_returns403() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "SHIPPED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOrderStatus_nonExistentOrder_returns404() throws Exception {
        mockMvc.perform(put("/api/orders/99999/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "SHIPPED"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOrderStatus_withInvalidStatus_returns400() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    // ========== CANCEL ORDER ==========

    @Test
    void cancelOrder_byOrderOwner_returns200AndRestoresStock() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 5);
        Product product = productRepository.findById(productId1).orElseThrow();
        int stockAfterOrder = product.getStock();

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify order is cancelled
        Order cancelled = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(Status.CANCELLED);

        // Verify stock restored
        Product updated = productRepository.findById(productId1).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(stockAfterOrder + 5);
    }

    @Test
    void cancelOrder_byAdmin_returns200() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_byNonOwnerUser_returns403() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelOrder_alreadyCancelled_returns400() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(Status.CANCELLED);
        orderRepository.saveAndFlush(order);

        mockMvc.perform(put("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Already canceled")));
    }

    @Test
    void cancelOrder_nonExistentOrder_returns404() throws Exception {
        mockMvc.perform(put("/api/orders/99999/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE ORDER ==========

    @Test
    void deleteOrder_byAdmin_returns200AndRemoves() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(delete("/api/orders/delete/" + orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("order deleted successfully"));

        assertThat(orderRepository.findById(orderId)).isEmpty();
    }

    @Test
    void deleteOrder_byRegularUser_returns403() throws Exception {
        Long orderId = createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(delete("/api/orders/delete/" + orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOrder_nonExistentOrder_returns404() throws Exception {
        mockMvc.perform(delete("/api/orders/delete/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ========== COUNT ORDERS ==========

    @Test
    void countOrders_byAdmin_returns200WithCorrectCount() throws Exception {
        createOrderDirectly(userId, productId1, 1);
        createOrderDirectly(userId, productId2, 1);
        createOrderDirectly(otherUserId, productId1, 1);

        mockMvc.perform(get("/api/orders/countOrders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }

    @Test
    void countOrders_byRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/countOrders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void countOrders_emptyDatabase_returns0() throws Exception {
        mockMvc.perform(get("/api/orders/countOrders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    // ========== TOTAL REVENUE ==========

    @Test
    void getTotalRevenue_byAdmin_returns200WithSum() throws Exception {
        createOrderDirectly(userId, productId1, 1);        // 999.99
        createOrderDirectly(userId, productId2, 2);        // 59.98
        // Total should be 1059.97

        mockMvc.perform(get("/api/orders/totalRevenue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1059.97));
    }

    @Test
    void getTotalRevenue_byRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/totalRevenue")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTotalRevenue_emptyDatabase_returns0() throws Exception {
        mockMvc.perform(get("/api/orders/totalRevenue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    // ========== GET ORDERS BY STATUS ==========

    @Test
    void getOrdersByStatus_byAdmin_returns200WithFilteredOrders() throws Exception {
        Long orderId1 = createOrderDirectly(userId, productId1, 1);
        Long orderId2 = createOrderDirectly(userId, productId2, 1);
        Long orderId3 = createOrderDirectly(otherUserId, productId1, 1);

        // Set one to SHIPPED
        Order order = orderRepository.findById(orderId2).orElseThrow();
        order.setStatus(Status.SHIPPED);
        orderRepository.save(order);

        mockMvc.perform(get("/api/orders/ordersByStatus")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getOrdersByStatus_byRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/ordersByStatus")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "PENDING"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrdersByStatus_noOrdersWithStatus_returns200WithEmptyList() throws Exception {
        createOrderDirectly(userId, productId1, 1);

        mockMvc.perform(get("/api/orders/ordersByStatus")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== HELPER METHODS ==========

    private OrderItemRequestDto createOrderItemDto(Long productId, int quantity) {
        return new OrderItemRequestDto(productId, quantity);
    }

    private Long createOrderDirectly(Long userId, Long productId, int quantity) {
        User user = userRepository.findById(userId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Status.PENDING);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setPrice(product.getPrice());

        order.getOrderItems().add(item);
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));

        // Update stock
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        return orderRepository.save(order).getId();
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