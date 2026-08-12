package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.dtos.ProductRatingDto;
import com.ecomerce.ecomerce_web.dtos.ReviewRequestDto;
import com.ecomerce.ecomerce_web.dtos.ReviewResponseDto;
import com.ecomerce.ecomerce_web.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;


    @PostMapping("/{productId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ReviewResponseDto> addReview(
            @PathVariable Long productId,
            @RequestBody @Valid ReviewRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                reviewService.addReview(
                        productId, dto, userDetails));
    }
    @GetMapping("/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviews(
            @PathVariable Long productId) {
        return ResponseEntity.ok(
                reviewService.getProductReviews(productId));
    }

    @GetMapping("/{productId}/rating")
    public ResponseEntity<ProductRatingDto> getRating(
            @PathVariable Long productId) {
        return ResponseEntity.ok(
                reviewService.getProductRating(productId));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.deleteReview(reviewId, userDetails);
        return ResponseEntity.ok(
                "Review deleted successfully");
    }
}
