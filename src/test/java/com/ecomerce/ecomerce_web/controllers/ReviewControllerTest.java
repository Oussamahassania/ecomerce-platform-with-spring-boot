package com.ecomerce.ecomerce_web.controllers;

import com.ecomerce.ecomerce_web.config.SecurityConfig;
import com.ecomerce.ecomerce_web.config.TestSecurityConfig;
import com.ecomerce.ecomerce_web.controller.ReviewController;
import com.ecomerce.ecomerce_web.dtos.ProductRatingDto;
import com.ecomerce.ecomerce_web.dtos.ReviewRequestDto;
import com.ecomerce.ecomerce_web.dtos.ReviewResponseDto;
import com.ecomerce.ecomerce_web.security.JwtUtils;
import com.ecomerce.ecomerce_web.security.UserDetailsServiceImpl;
import com.ecomerce.ecomerce_web.services.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ReviewService reviewService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private ReviewResponseDto reviewResponse;
    private ReviewRequestDto reviewRequest;

    @BeforeEach
    void setUp() {
        reviewResponse = new ReviewResponseDto();
        reviewResponse.setId(1L);
        reviewResponse.setRating(5);
        reviewResponse.setComment("Great product");

        reviewRequest = new ReviewRequestDto();
        reviewRequest.setRating(5);
        reviewRequest.setComment("Great product");
    }

    @Nested
    @DisplayName("POST /api/review/{productId}")
    class AddReview {

        @Test
        @DisplayName("user should add review")
        void userShouldAddReview() throws Exception {
            when(reviewService.addReview(eq(1L), any(ReviewRequestDto.class), any(UserDetails.class)))
                    .thenReturn(reviewResponse);

            mockMvc.perform(post("/api/review/1")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rating").value(5));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(post("/api/review/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 400 when rating is null")
        void shouldRejectNullRating() throws Exception {
            reviewRequest.setRating(null);

            mockMvc.perform(post("/api/review/1")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.rating").exists());

            verifyNoInteractions(reviewService);
        }

        @Test
        @DisplayName("should return 400 when rating is out of range")
        void shouldRejectRatingOutOfRange() throws Exception {
            reviewRequest.setRating(6);

            mockMvc.perform(post("/api/review/1")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.rating").exists());

            verifyNoInteractions(reviewService);
        }

        @Test
        @DisplayName("should return 400 when comment exceeds 500 characters")
        void shouldRejectLongComment() throws Exception {
            reviewRequest.setComment("a".repeat(501));

            mockMvc.perform(post("/api/review/1")
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.comment").exists());

            verifyNoInteractions(reviewService);
        }
    }

    @Nested
    @DisplayName("GET /api/review/{productId}")
    class GetReviews {

        @Test
        @DisplayName("should return reviews for product (public)")
        void shouldGetReviews() throws Exception {
            when(reviewService.getProductReviews(1L)).thenReturn(List.of(reviewResponse));

            mockMvc.perform(get("/api/review/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].rating").value(5));
        }
    }

    @Nested
    @DisplayName("GET /api/review/{productId}/rating")
    class GetRating {

        @Test
        @DisplayName("should return average rating (public)")
        void shouldGetRating() throws Exception {
            ProductRatingDto ratingDto = new ProductRatingDto(1L, 4.5, 10L);
            when(reviewService.getProductRating(1L)).thenReturn(ratingDto);

            mockMvc.perform(get("/api/review/1/rating"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageRating").value(4.5))
                    .andExpect(jsonPath("$.totalReviews").value(10));
        }
    }

    @Nested
    @DisplayName("DELETE /api/review/{reviewId}")
    class DeleteReview {

        @Test
        @DisplayName("user should delete their review")
        void userShouldDelete() throws Exception {
            doNothing().when(reviewService).deleteReview(eq(1L), any(UserDetails.class));

            mockMvc.perform(delete("/api/review/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Review deleted successfully"));
        }

        @Test
        @DisplayName("unauthenticated should get 403")
        void unauthenticatedShouldBeDenied() throws Exception {
            mockMvc.perform(delete("/api/review/1"))
                    .andExpect(status().isForbidden());
        }
    }
}