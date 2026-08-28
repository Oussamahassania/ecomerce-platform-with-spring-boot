package com.ecomerce.ecomerce_web.integrationtesting;

import com.ecomerce.ecomerce_web.config.TestCacheConfig;
import com.ecomerce.ecomerce_web.config.TestMailConfig;
import com.ecomerce.ecomerce_web.dtos.LoginRequest;
import com.ecomerce.ecomerce_web.dtos.ReviewRequestDto;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestMailConfig.class, TestCacheConfig.class})
class ReviewIntegrationTest {

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
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private String secondUserToken;
    private Long categoryId;
    private User user;
    private User secondUser;

    @BeforeEach
    void setUp() throws Exception {
        reviewRepository.deleteAll();
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

    // ---------- Add review ----------

    @Test
    void addReview_userOrderedProduct_returns200() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        createDeliveredOrderWithProduct(user, productId, 1);

        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRating(5);
        dto.setComment("Great product!");

        mockMvc.perform(post("/api/review/" + productId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void addReview_userNeverOrderedProduct_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);

        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRating(4);
        dto.setComment("Looks good");

        mockMvc.perform(post("/api/review/" + productId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReview_orderWasCancelled_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        createOrderWithProduct(user, productId, 1, Status.CANCELLED);

        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRating(3);
        dto.setComment("N/A");

        mockMvc.perform(post("/api/review/" + productId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReview_duplicateReview_returns400() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        createDeliveredOrderWithProduct(user, productId, 1);
        createReviewDirectly(user, productId, 4, "First review");

        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRating(5);
        dto.setComment("Trying again");

        mockMvc.perform(post("/api/review/" + productId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReview_nonExistentProduct_returns404() throws Exception {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRating(5);
        dto.setComment("N/A");

        mockMvc.perform(post("/api/review/99999")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addReview_noToken_returns403() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);

        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRating(5);
        dto.setComment("N/A");

        mockMvc.perform(post("/api/review/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ---------- Get reviews ----------

    @Test
    void getReviews_publicNoToken_returnsList() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        createReviewDirectly(user, productId, 4, "Nice");
        createReviewDirectly(secondUser, productId, 5, "Excellent");

        mockMvc.perform(get("/api/review/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getReviews_nonExistentProduct_returns404() throws Exception {
        mockMvc.perform(get("/api/review/99999"))
                .andExpect(status().isNotFound());
    }

    // ---------- Get rating ----------

    @Test
    void getRating_returnsAverageAndCount() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        createReviewDirectly(user, productId, 4, "Nice");
        createReviewDirectly(secondUser, productId, 2, "Meh");

        mockMvc.perform(get("/api/review/" + productId + "/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(3.0))
                .andExpect(jsonPath("$.totalReviews").value(2));   // was reviewCount
    }

    @Test
    void getRating_noReviews_returnsZeroAverage() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);

        mockMvc.perform(get("/api/review/" + productId + "/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.totalReviews").value(0));    // was reviewCount
    }

    @Test
    void getRating_nonExistentProduct_returns404() throws Exception {
        mockMvc.perform(get("/api/review/99999/rating"))
                .andExpect(status().isNotFound());
    }

    // ---------- Delete review ----------

    @Test
    void deleteReview_asOwner_returns200() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Long reviewId = createReviewDirectly(user, productId, 4, "Nice");

        mockMvc.perform(delete("/api/review/" + reviewId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReview_asAdmin_returns200() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Long reviewId = createReviewDirectly(user, productId, 4, "Nice");

        mockMvc.perform(delete("/api/review/" + reviewId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReview_notOwnerNotAdmin_returns403() throws Exception {
        Long productId = createProductDirectly("Widget", new BigDecimal("10.00"), 5);
        Long reviewId = createReviewDirectly(user, productId, 4, "Nice");

        mockMvc.perform(delete("/api/review/" + reviewId)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReview_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/review/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // ---------- Helpers ----------

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

    private void createDeliveredOrderWithProduct(User owner, Long productId, int quantity) {
        createOrderWithProduct(owner, productId, quantity, Status.CONFIRMED);
    }

    private void createOrderWithProduct(User owner, Long productId, int quantity, Status status) {
        Order order = new Order();
        order.setUser(owner);
        order.setTotalAmount(new BigDecimal("10.00"));
        Order savedOrder = orderRepository.save(order); // INSERT — @PrePersist may override status here

        Product product = productRepository.findById(productId).orElseThrow();
        OrderItem item = new OrderItem();
        item.setOrder(savedOrder);
        item.setProduct(product);
        item.setQuantity(quantity);
        orderItemRepository.save(item);

        savedOrder.setOrderItems(List.of(item));
        savedOrder.setStatus(status);                  // set AFTER insert
        savedOrder.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(savedOrder);               // UPDATE — sticks
    }

    private Long createReviewDirectly(User author, Long productId, int rating, String comment) {
        Review review = new Review();
        review.setUser(author);
        review.setProduct(productRepository.findById(productId).orElseThrow());
        review.setRating(rating);
        review.setComment(comment);
        return reviewRepository.save(review).getId();
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