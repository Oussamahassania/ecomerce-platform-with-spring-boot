package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.dtos.ProductRatingDto;
import com.ecomerce.ecomerce_web.dtos.ReviewRequestDto;
import com.ecomerce.ecomerce_web.dtos.ReviewResponseDto;
import com.ecomerce.ecomerce_web.entity.Product;
import com.ecomerce.ecomerce_web.entity.Review;
import com.ecomerce.ecomerce_web.entity.User;
import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import com.ecomerce.ecomerce_web.exception.UnauthorizedActionException;
import com.ecomerce.ecomerce_web.mapper.ReviewMapper;
import com.ecomerce.ecomerce_web.repository.ProductRepository;
import com.ecomerce.ecomerce_web.repository.ReviewRepository;
import com.ecomerce.ecomerce_web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewMapper reviewMapper;
    @Mock private ReviewRepository reviewRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserDetails userDetails;

    private ReviewService reviewService;

    private User user;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewMapper, reviewRepository, userRepository, productRepository);
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
    }

    @Nested
    @DisplayName("addReview")
    class AddReview {

        @Test
        @DisplayName("should add review when user ordered product and hasn't reviewed yet")
        void shouldAddReview() {
            when(userDetails.getUsername()).thenReturn("user@test.com");
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            Product product = new Product();
            product.setId(100L);
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            when(reviewRepository.hasUserOrderedProduct(1L, 100L)).thenReturn(true);
            when(reviewRepository.findByUserIdAndProductId(1L, 100L)).thenReturn(Optional.empty());

            Review saved = new Review();
            when(reviewRepository.save(any(Review.class))).thenReturn(saved);
            ReviewResponseDto responseDto = new ReviewResponseDto();
            when(reviewMapper.toDto(saved)).thenReturn(responseDto);

            ReviewRequestDto dto = new ReviewRequestDto();
            dto.setRating(5);
            dto.setComment("Great product");

            ReviewResponseDto result = reviewService.addReview(100L, dto, userDetails);

            assertThat(result).isEqualTo(responseDto);
        }

        @Test
        @DisplayName("should reject review when user never ordered the product")
        void shouldRejectReviewWithoutPurchase() {
            when(userDetails.getUsername()).thenReturn("user@test.com");
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            Product product = new Product();
            product.setId(100L);
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            when(reviewRepository.hasUserOrderedProduct(1L, 100L)).thenReturn(false);

            ReviewRequestDto dto = new ReviewRequestDto();
            dto.setRating(5);

            assertThatThrownBy(() -> reviewService.addReview(100L, dto, userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("only review product");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject duplicate review for the same product")
        void shouldRejectDuplicateReview() {
            when(userDetails.getUsername()).thenReturn("user@test.com");
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            Product product = new Product();
            product.setId(100L);
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            when(reviewRepository.hasUserOrderedProduct(1L, 100L)).thenReturn(true);
            when(reviewRepository.findByUserIdAndProductId(1L, 100L))
                    .thenReturn(Optional.of(new Review()));

            ReviewRequestDto dto = new ReviewRequestDto();
            dto.setRating(4);

            assertThatThrownBy(() -> reviewService.addReview(100L, dto, userDetails))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("already reviewed");
        }
    }

    @Nested
    @DisplayName("getProductRating")
    class GetProductRating {

        @Test
        @DisplayName("should round average rating to 1 decimal place")
        void shouldRoundAverageRating() {
            when(productRepository.existsById(100L)).thenReturn(true);
            when(reviewRepository.findAverageRatingByProductId(100L)).thenReturn(4.267);
            when(reviewRepository.countByProductId(100L)).thenReturn(3L);

            ProductRatingDto result = reviewService.getProductRating(100L);

            assertThat(result.getAverageRating()).isEqualTo(4.3);
            assertThat(result.getTotalReviews()).isEqualTo(3L);
        }

        @Test
        @DisplayName("should return 0.0 average when product has no reviews")
        void shouldReturnZeroWhenNoReviews() {
            when(productRepository.existsById(100L)).thenReturn(true);
            when(reviewRepository.findAverageRatingByProductId(100L)).thenReturn(null);
            when(reviewRepository.countByProductId(100L)).thenReturn(0L);

            ProductRatingDto result = reviewService.getProductRating(100L);

            assertThat(result.getAverageRating()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should throw when product does not exist")
        void shouldThrowWhenProductMissing() {
            when(productRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.getProductRating(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteReview")
    class DeleteReview {

        @Test
        @DisplayName("owner can delete their own review")
        void ownerCanDelete() {
            doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
            when(userDetails.getUsername()).thenReturn("user@test.com");

            Review review = new Review();
            review.setUser(user);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            reviewService.deleteReview(1L, userDetails);

            verify(reviewRepository).delete(review);
        }

        @Test
        @DisplayName("non-owner non-admin cannot delete another user's review")
        void nonOwnerCannotDelete() {
            doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();
            when(userDetails.getUsername()).thenReturn("stranger@test.com");

            Review review = new Review();
            review.setUser(user); // owned by "user@test.com"
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.deleteReview(1L, userDetails))
                    .isInstanceOf(UnauthorizedActionException.class);

            verify(reviewRepository, never()).delete(any());
        }

        @Test
        @DisplayName("admin can delete any review (after ROLE_ADMIN fix)")
        void adminCanDeleteAnyReview() {
            doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(userDetails).getAuthorities();

            Review review = new Review();
            review.setUser(user);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

            assertThatCode(() -> reviewService.deleteReview(1L, userDetails))
                    .doesNotThrowAnyException();
        }
    }
}