package com.ecomerce.ecomerce_web.integrationtesting;

import com.ecomerce.ecomerce_web.config.TestCacheConfig;
import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class OrderItemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String ownerToken;
    private String otherToken;
    private String adminToken;

    private Product product;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() throws Exception {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        User owner = registerUserDirectly("owner@example.com", "Passw0rd!", "USER");
        registerUserDirectly("other@example.com", "Passw0rd!", "USER");
        registerUserDirectly("admin@example.com", "Passw0rd!", "ADMIN");

        ownerToken = loginAndGetToken("owner@example.com", "Passw0rd!");
        otherToken = loginAndGetToken("other@example.com", "Passw0rd!");
        adminToken = loginAndGetToken("admin@example.com", "Passw0rd!");

        product = new Product();
        product.setName("Test Laptop");
        product.setDescription("A perfectly ordinary description over 20 characters");
        product.setStock(10);
        product.setPrice(new BigDecimal("999.99"));
        product.setImg_url("https://example.com/img.jpg");
        product.setCreatedAt(LocalDateTime.now());
        product.setActive(true);
        product = productRepository.save(product);

        order = new Order();
        order.setUser(owner);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Status.PENDING);
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(2)));
        order = orderRepository.save(order);

        orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setPrice(product.getPrice());
        orderItem = orderItemRepository.save(orderItem);
    }

    // ==========================================
    // GET /api/orderItem/itemByOrderId/{orderId}
    // ==========================================
    @Nested
    @DisplayName("GET /api/orderItem/itemByOrderId/{orderId}")
    class GetItemsByOrderId {

        @Test
        void ownerCanFetchOrderItems() throws Exception {
            mockMvc.perform(get("/api/orderItem/itemByOrderId/{orderId}", order.getId())
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].productId").value(product.getId()))
                    .andExpect(jsonPath("$[0].quantity").value(2));
        }

        @Test
        void adminCanFetchOrderItems() throws Exception {
            mockMvc.perform(get("/api/orderItem/itemByOrderId/{orderId}", order.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void nonOwnerCannotFetchOrderItems() throws Exception {
            mockMvc.perform(get("/api/orderItem/itemByOrderId/{orderId}", order.getId())
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void returns404WhenOrderHasNoItems() throws Exception {
            mockMvc.perform(get("/api/orderItem/itemByOrderId/{orderId}", 999L)
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isNotFound());
        }
    }

    // ==============================================
    // GET /api/orderItem/orderItemById/{orderItemId}
    // ==============================================
    @Nested
    @DisplayName("GET /api/orderItem/orderItemById/{orderItemId}")
    class GetOrderItemById {

        @Test
        void ownerCanFetchOrderItemById() throws Exception {
            mockMvc.perform(get("/api/orderItem/orderItemById/{orderItemId}", orderItem.getId())
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(product.getId()))
                    .andExpect(jsonPath("$.quantity").value(2));
        }

        @Test
        void nonOwnerAccessDenied() throws Exception {
            mockMvc.perform(get("/api/orderItem/orderItemById/{orderItemId}", orderItem.getId())
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void returns404ForNonExistentId() throws Exception {
            mockMvc.perform(get("/api/orderItem/orderItemById/{orderItemId}", 999L)
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isNotFound());
        }
    }

    // ==============================================
    // PUT /api/orderItem/updateQuantity/{id}
    // ==============================================
    @Nested
    @DisplayName("PUT /api/orderItem/updateQuantity/{id}")
    class UpdateQuantity {

        @Test
        void ownerCanUpdateQuantitySuccess() throws Exception {
            mockMvc.perform(put("/api/orderItem/updateQuantity/{id}", orderItem.getId())
                            .header("Authorization", "Bearer " + ownerToken)
                            .param("quantity", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantity").value(5));

            Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updatedProduct.getStock()).isEqualTo(7);
        }

        @Test
        void failWhenStockIsInsufficient() throws Exception {
            mockMvc.perform(put("/api/orderItem/updateQuantity/{id}", orderItem.getId())
                            .header("Authorization", "Bearer " + ownerToken)
                            .param("quantity", "20"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void nonOwnerCannotUpdateQuantity() throws Exception {
            mockMvc.perform(put("/api/orderItem/updateQuantity/{id}", orderItem.getId())
                            .header("Authorization", "Bearer " + otherToken)
                            .param("quantity", "4"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==============================================
    // DELETE /api/orderItem/removeOrderItem/{id}
    // ==============================================
    @Nested
    @DisplayName("DELETE /api/orderItem/removeOrderItem/{id}")
    class RemoveOrderItem {

        @Test
        void ownerCanRemoveOrderItemAndRestoreStock() throws Exception {
            mockMvc.perform(delete("/api/orderItem/removeOrderItem/{id}", orderItem.getId())
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(content().string("OrderItem Removed Successfully"));

            assertThat(orderItemRepository.findById(orderItem.getId())).isEmpty();

            Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updatedProduct.getStock()).isEqualTo(12);
        }

        @Test
        void nonOwnerCannotRemoveOrderItem() throws Exception {
            mockMvc.perform(delete("/api/orderItem/removeOrderItem/{id}", orderItem.getId())
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());

            assertThat(orderItemRepository.findById(orderItem.getId())).isPresent();
        }

        @Test
        void returns404WhenDeletingNonExistentItem() throws Exception {
            mockMvc.perform(delete("/api/orderItem/removeOrderItem/{id}", 999L)
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------- Helpers ----------

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