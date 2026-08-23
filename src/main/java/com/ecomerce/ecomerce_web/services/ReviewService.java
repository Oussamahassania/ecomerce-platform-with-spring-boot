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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewMapper reviewMapper;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private User getUser(UserDetails userDetails){
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
    }

    @Transactional
    public ReviewResponseDto addReview(
            Long productId,
            ReviewRequestDto dto,
            UserDetails userDetails
    ){
        User user = getUser(userDetails);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product Not Found"));

        if (!reviewRepository.hasUserOrderedProduct(user.getId(),productId)){
            throw new InvalidRequestException("You can only review product u have ordered");

        }
        if (reviewRepository.findByUserIdAndProductId(user.getId(),productId).isPresent()){
            throw new InvalidRequestException("You have already reviewed this product");
        }

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        Review saved = reviewRepository.save(review);
        log.info(" [{}] Added review for product {}",
                userDetails.getUsername(), productId);

        return reviewMapper.toDto(saved);

    }
    @Transactional(readOnly = true)
    public List<ReviewResponseDto>getProductReviews(Long productId){
        if (!productRepository.existsById(productId))
            throw new ResourceNotFoundException(
                    "Product not found");

        return reviewMapper.toDtoList(
                reviewRepository.findByProductId(productId));
    }
    @Transactional(readOnly = true)
    public ProductRatingDto getProductRating(Long productId){
        if (!productRepository.existsById(productId))
            throw new ResourceNotFoundException(
                    "Product not found");
        Double avg = reviewRepository.findAverageRatingByProductId(productId);
        Long count = reviewRepository.countByProductId(productId);

        return new ProductRatingDto(
                productId,
                avg!=null
                   ?Math.round(avg * 10.0) / 10.0
                   : 0.0,
                count
        );
    }
    @Transactional
    public void deleteReview(Long reviewId,UserDetails userDetails){
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found"));
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_ADMIN"));
        if (!isAdmin && !review.getUser().getEmail().equals(userDetails.getUsername())){
            throw new UnauthorizedActionException("You can only delete your own reviews");
        }
        reviewRepository.delete(review);
        reviewRepository.flush();
        log.info(" [{}] Deleted review {}",
                userDetails.getUsername(), reviewId);

    }
}
