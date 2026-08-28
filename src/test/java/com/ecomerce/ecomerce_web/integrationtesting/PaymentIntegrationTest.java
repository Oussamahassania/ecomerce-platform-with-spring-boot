package com.ecomerce.ecomerce_web.integrationtesting;

import com.ecomerce.ecomerce_web.config.TestCacheConfig;
import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.PaymentRequestDto;
import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.repository.*;
import com.ecomerce.ecomerce_web.services.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestMailConfig.class, TestCacheConfig.class})
class PaymentIntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private PaymentGateway paymentGateway;

    private String adminToken;
    private String userToken;
    private String secondUserToken;
    private Long categoryId;
    private User user;
    private User secondUser;

    @BeforeEach
    void setUp() throws Exception {
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
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
        user = registerUserDirectly("user@example.com", "Passw0rd!", "USER");
        secondUser = registerUserDirectly("second@example.com", "Passw0rd!", "USER");

        adminToken = loginAndGetToken("admin@example.com", "Passw0rd!");
        userToken = loginAndGetToken("user@example.com", "Passw0rd!");
        secondUserToken = loginAndGetToken("second@example.com", "Passw0rd!");

        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("desc");
        categoryId = categoryRepository.save(category).getId();
    }

    // ---------- Pay ----------

    @Test
    void pay_asOwner_gatewayApproves_returnsPaidAndUpdatesOrder() throws Exception {
        when(paymentGateway.charge(any())).thenReturn(true);

        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.PENDING, PaymentStatus.PENDING,
                new BigDecimal("20.00"), productId, 2);

        mockMvc.perform(post("/api/payment/pay/" + order.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updated.getStatus()).isEqualTo(Status.CONFIRMED);
    }

    @Test
    void pay_gatewayDeclines_returnsFailedStatus() throws Exception {
        when(paymentGateway.charge(any())).thenReturn(false);

        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.PENDING, PaymentStatus.PENDING,
                new BigDecimal("20.00"), productId, 2);

        mockMvc.perform(post("/api/payment/pay/" + order.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void pay_withIdempotencyKey_secondCallReturnsCachedPayment() throws Exception {
        when(paymentGateway.charge(any())).thenReturn(true);

        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.PENDING, PaymentStatus.PENDING,
                new BigDecimal("20.00"), productId, 2);

        String idempotencyKey = "test-key-123";

        MvcResult first = mockMvc.perform(post("/api/payment/pay/" + order.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .header("idempotency-key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isOk())
                .andReturn();

        String firstRef = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("paymentReference").asText();

        MvcResult second = mockMvc.perform(post("/api/payment/pay/" + order.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .header("idempotency-key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isOk())
                .andReturn();

        String secondRef = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("paymentReference").asText();

        assertThat(secondRef).isEqualTo(firstRef);
        assertThat(paymentRepository.findAll()).hasSize(1);
    }

    @Test
    void pay_notOwner_returns403() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.PENDING, PaymentStatus.PENDING,
                new BigDecimal("20.00"), productId, 2);

        mockMvc.perform(post("/api/payment/pay/" + order.getId())
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void pay_alreadyPaidOrder_returns409() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.CONFIRMED, PaymentStatus.PAID,
                new BigDecimal("20.00"), productId, 2);

        mockMvc.perform(post("/api/payment/pay/" + order.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void pay_cancelledOrder_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.CANCELLED, PaymentStatus.PENDING,
                new BigDecimal("20.00"), productId, 2);

        mockMvc.perform(post("/api/payment/pay/" + order.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pay_refundedOrder_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.CANCELLED, PaymentStatus.REFUNDED,
                new BigDecimal("20.00"), productId, 2);

        mockMvc.perform(post("/api/payment/pay/" + order.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pay_nonExistentOrder_returns404() throws Exception {
        mockMvc.perform(post("/api/payment/pay/99999")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void pay_noToken_returns403() throws Exception {
        mockMvc.perform(post("/api/payment/pay/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePaymentRequest())))
                .andExpect(status().isForbidden());
    }

    // ---------- Get status ----------

    @Test
    void getStatus_asOwner_returns200() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.CONFIRMED, PaymentStatus.PAID,
                new BigDecimal("20.00"), productId, 2);
        createPaymentDirectly(order, PaymentStatus.PAID);

        mockMvc.perform(get("/api/payment/" + order.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void getStatus_asAdmin_returns200() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.CONFIRMED, PaymentStatus.PAID,
                new BigDecimal("20.00"), productId, 2);
        createPaymentDirectly(order, PaymentStatus.PAID);

        mockMvc.perform(get("/api/payment/" + order.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void getStatus_notOwner_returns403() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.CONFIRMED, PaymentStatus.PAID,
                new BigDecimal("20.00"), productId, 2);
        createPaymentDirectly(order, PaymentStatus.PAID);

        mockMvc.perform(get("/api/payment/" + order.getId())
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getStatus_noPaymentRecordYet_returns404() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Order order = createOrderDirectly(user, Status.PENDING, PaymentStatus.PENDING,
                new BigDecimal("20.00"), productId, 2);

        mockMvc.perform(get("/api/payment/" + order.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // ---------- Refund ----------

    @Test
    void refund_paidOrder_returns200AndRestoresStock() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 3);
        Order order = createOrderDirectly(user, Status.CONFIRMED, PaymentStatus.PAID,
                new BigDecimal("20.00"), productId, 2);
        createPaymentDirectly(order, PaymentStatus.PAID);

        mockMvc.perform(post("/api/payment/refund/" + order.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStock()).isEqualTo(5); // 3 + 2 restored

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(updated.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void refund_asAdmin_returns200() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 3);
        Order order = createOrderDirectly(user, Status.CONFIRMED, PaymentStatus.PAID,
                new BigDecimal("20.00"), productId, 2);
        createPaymentDirectly(order, PaymentStatus.PAID);

        mockMvc.perform(post("/api/payment/refund/" + order.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void refund_notPaidOrder_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 3);
        Order order = createOrderDirectly(user, Status.PENDING, PaymentStatus.PENDING,
                new BigDecimal("20.00"), productId, 2);

        mockMvc.perform(post("/api/payment/refund/" + order.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refund_notOwner_returns403() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 3);
        Order order = createOrderDirectly(user, Status.CONFIRMED, PaymentStatus.PAID,
                new BigDecimal("20.00"), productId, 2);
        createPaymentDirectly(order, PaymentStatus.PAID);

        mockMvc.perform(post("/api/payment/refund/" + order.getId())
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void refund_nonExistentOrder_returns404() throws Exception {
        mockMvc.perform(post("/api/payment/refund/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // ---------- Helpers ----------

    private PaymentRequestDto samplePaymentRequest() {
        PaymentRequestDto dto = new PaymentRequestDto();
        // Field names assumed - adjust to your real DTO. The gateway call is mocked
        // so the actual card details are never validated against a real processor.
        dto.setPaymentMethod("PAYPAL");
        return dto;
    }

    private Long createProductDirectly(String name, BigDecimal price, int stock) {
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

    private Order createOrderDirectly(User owner, Status status, PaymentStatus paymentStatus,
                                      BigDecimal totalAmount, Long productId, int quantity) {
        Order order = new Order();
        order.setUser(owner);
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order); // INSERT — @PrePersist may force status/paymentStatus to defaults here

        Product product = productRepository.findById(productId).orElseThrow();
        OrderItem item = new OrderItem();
        item.setOrder(savedOrder);
        item.setProduct(product);
        item.setQuantity(quantity);
        orderItemRepository.save(item);

        savedOrder.setOrderItems(List.of(item));
        savedOrder.setStatus(status);
        savedOrder.setPaymentStatus(paymentStatus);
        return orderRepository.save(savedOrder);
    }

    private Payment createPaymentDirectly(Order order, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentReference("PAY-TEST0001");
        payment.setIdempotencyKey(java.util.UUID.randomUUID().toString());
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(status);
        payment.setProcessedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
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