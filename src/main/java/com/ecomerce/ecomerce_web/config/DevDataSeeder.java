package com.ecomerce.ecomerce_web.config;

import com.ecomerce.ecomerce_web.entity.*;
import com.ecomerce.ecomerce_web.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds a local H2 database so the storefront has something real to show.
 * Only runs with the "dev" profile and never overwrites existing data.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN", null)));
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER", null)));

        User admin = userRepository.findByEmail("admin@forma.dev").orElseGet(() ->
                userRepository.save(User.builder()
                        .fullName("Oussama Hassania")
                        .email("admin@forma.dev")
                        .password(passwordEncoder.encode("Admin123!"))
                        .dateOfBirth(LocalDate.of(1998, 4, 12))
                        .createdAt(LocalDateTime.now().minusMonths(8))
                        .role(adminRole)
                        .emailVerified(true)
                        .build()));

        User demo = userRepository.findByEmail("demo@forma.dev").orElseGet(() ->
                userRepository.save(User.builder()
                        .fullName("Nora Bennett")
                        .email("demo@forma.dev")
                        .password(passwordEncoder.encode("Demo1234!"))
                        .dateOfBirth(LocalDate.of(1994, 9, 3))
                        .createdAt(LocalDateTime.now().minusMonths(4))
                        .role(userRole)
                        .emailVerified(true)
                        .build()));

        Category audio = category("Audio", "Speakers and headphones tuned for rooms, not showrooms.");
        Category lighting = category("Lighting", "Lamps that shape a room after the sun goes down.");
        Category desk = category("Desk", "Tools that make the workday feel less like a workstation.");
        Category carry = category("Carry", "Bags built around the things you actually carry.");
        Category objects = category("Objects", "Small, useful things with a long life ahead of them.");

        if (productRepository.count() == 0) {
            Map<String, Product> products = new LinkedHashMap<>();
            products.put("Orbit Speaker", product("Orbit Speaker",
                    "A palm-sized speaker with a warm low end and a machined aluminium shell.",
                    "189.00", 24, "/images/products/orbit-speaker.jpg", audio, 1));
            products.put("Fold Headphones", product("Fold Headphones",
                    "Over-ear headphones tuned for long listening, with replaceable pads and cable.",
                    "310.00", 12, "/images/products/fold-headphones.jpg", audio, 2));
            products.put("Halo Task Light", product("Halo Task Light",
                    "A dimmable desk light with a weighted base and a soft, glare-free beam.",
                    "240.00", 18, "/images/products/halo-task-light.jpg", lighting, 3));
            products.put("Column Lamp", product("Column Lamp",
                    "A slim ambient lamp that casts a quiet pool of light across a shelf.",
                    "165.00", 9, "/images/products/column-lamp.jpg", lighting, 4));
            products.put("Slab Tray", product("Slab Tray",
                    "A hand-finished stone tray for keys, pens and the things that wander.",
                    "64.00", 40, "/images/products/slab-tray.jpg", desk, 5));
            products.put("Pebble Clock", product("Pebble Clock",
                    "A silent, single-hand clock in cast mineral composite.",
                    "95.00", 21, "/images/products/pebble-clock.jpg", desk, 6));
            products.put("Arc Charger", product("Arc Charger",
                    "A low-profile wireless charger milled from a single block of aluminium.",
                    "75.00", 15, "/images/products/arc-charger.jpg", desk, 7));
            products.put("Carry Tote 01", product("Carry Tote 01",
                    "A structured canvas tote with a cobalt strap and an inside pocket.",
                    "120.00", 30, "/images/products/carry-tote.jpg", carry, 8));
            products.put("Loop Vase", product("Loop Vase",
                    "A mouth-blown vase with a looping handle, made in small batches.",
                    "88.00", 14, "/images/products/loop-vase.jpg", objects, 9));
            products.put("Fold Notebook", product("Fold Notebook",
                    "A lay-flat notebook with dotted pages and a linen cover that ages well.",
                    "28.00", 34, "/images/products/fold-notebook.jpg", objects, 10));

            if (orderRepository.count() == 0) {
                seedDeliveredOrder(demo, products.get("Halo Task Light"), products.get("Pebble Clock"));
            }
            if (reviewRepository.count() == 0) {
                review(demo, products.get("Halo Task Light"), 5,
                        "Softer than it looks in photos. It made my desk feel calmer within a day.");
                review(demo, products.get("Pebble Clock"), 4,
                        "Quiet, heavy in a good way, and the single hand is genuinely easy to read.");
                review(admin, products.get("Orbit Speaker"), 5,
                        "The low end is surprising for the size. It lives on my kitchen shelf now.");
            }
        }

        log.info("""
                
                ============================================================
                  FORMA dev data ready
                  Storefront : http://localhost:5173
                  Admin      : admin@forma.dev / Admin123!
                  Customer   : demo@forma.dev  / Demo1234!
                ============================================================
                """);

        // A request can reach the storefront between Tomcat starting and this
        // runner finishing. Drop any empty results cached during that window.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    clearCaches();
                }
            });
        } else {
            clearCaches();
        }
    }

    private void clearCaches() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private Category category(String name, String description) {
        return categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            return categoryRepository.save(category);
        });
    }

    private Product product(String name, String description, String price, int stock,
                            String image, Category category, int daysAgo) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setImg_url(image);
        product.setCategory(category);
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        return productRepository.save(product);
    }

    private void seedDeliveredOrder(User user, Product first, Product second) {
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(first.getPrice().add(second.getPrice()));

        OrderItem one = new OrderItem();
        one.setOrder(order);
        one.setProduct(first);
        one.setQuantity(1);
        one.setPrice(first.getPrice());

        OrderItem two = new OrderItem();
        two.setOrder(order);
        two.setProduct(second);
        two.setQuantity(1);
        two.setPrice(second.getPrice());

        order.getOrderItems().add(one);
        order.getOrderItems().add(two);

        order = orderRepository.save(order);
        order.setStatus(Status.DELIVERED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentReference("PAY-DEMO0001");
        order.setOrderDate(LocalDateTime.now().minusDays(10));
        order.setPaidAt(LocalDateTime.now().minusDays(10));
        orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentReference("PAY-DEMO0001");
        payment.setIdempotencyKey("seed-demo-order-payment");
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PAID);
        payment.setProcessedAt(LocalDateTime.now().minusDays(10));
        paymentRepository.save(payment);
    }

    private void review(User user, Product product, int rating, String comment) {
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);
    }
}
