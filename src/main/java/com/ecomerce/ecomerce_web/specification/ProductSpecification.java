package com.ecomerce.ecomerce_web.specification;

import com.ecomerce.ecomerce_web.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    public static Specification<Product>isActive(){
        return (root,query,cb) -> cb.isTrue(root.get("active"));
    }
    public static Specification<Product>hasNameLike(String name){
        if (name == null || name.isBlank()) return null;
        return ((root, query, cb)
                -> cb.like(cb.lower(root.get("name")),
                "%" + name.toLowerCase() + "%"));
    }
    public static Specification<Product>priceBetween(BigDecimal min,BigDecimal max){
        if (max==null && min==null)return null;
        return (root, query, criteriaBuilder) -> {
            if (min!=null && max!=null)return criteriaBuilder.between(root.get("price"),min,max);
            if (min!=null)return criteriaBuilder.greaterThanOrEqualTo(root.get("price"),min);
            return criteriaBuilder.lessThanOrEqualTo(root.get("price"),max);
        };
    }
    public static Specification<Product>hasCategoryId(Long categoryId){
        if (categoryId==null)return null;
        return (root,query,cb)
                -> cb.equal(root.get("category").get("id"),categoryId);
    }
    public static Specification<Product>build(String name,BigDecimal minPrice,BigDecimal maxPrice,Long categoryId){
        Specification<Product>spec = Specification.where(isActive());
        Specification<Product>byName = hasNameLike(name);
        Specification<Product>byPrice = priceBetween(minPrice,maxPrice);
        Specification<Product>byCategory = hasCategoryId(categoryId);

        if (byName!=null) spec = spec.and(byName);
        if (byPrice!=null) spec = spec.and(byPrice);
        if (byCategory!=null) spec = spec.and(byCategory);

        return spec;


    }

}
