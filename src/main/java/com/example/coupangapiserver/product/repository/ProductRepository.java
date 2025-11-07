package com.example.coupangapiserver.product.repository;

import com.example.coupangapiserver.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
