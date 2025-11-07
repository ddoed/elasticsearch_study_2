package com.example.coupangapiserver.product.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.example.coupangapiserver.product.repository.ProductRepository;
import com.example.coupangapiserver.product.domain.Product;
import com.example.coupangapiserver.product.domain.ProductDocument;
import com.example.coupangapiserver.product.dto.CreateProductRequestDto;

import java.util.ArrayList;
import java.util.List;

import com.example.coupangapiserver.product.repository.ProductDocumentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductDocumentRepository productDocumentRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public ProductService(ProductRepository productRepository, ProductDocumentRepository productDocumentRepository, ElasticsearchOperations elasticsearchOperations) {
        this.productRepository = productRepository;
        this.productDocumentRepository = productDocumentRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public List<Product> getProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return productRepository.findAll(pageable).getContent();
    }

    public List<String> getSuggestions(String query) {
        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .type(TextQueryType.BoolPrefix)
                .fields("name.auto_complete", "name.auto_complete._2gram", "name.auto_complete._3gram")
        )._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(multiMatchQuery)
                .withPageable(PageRequest.of(0, 5))
                .build();
        // ProductDocument라는 Index에 대해 search Query 실행
        SearchHits<ProductDocument> searchHits = this.elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                .map(hit -> {
                    ProductDocument productDocument = hit.getContent();
                    return productDocument.getName();
                })
                .toList();
    }

    public Product createProduct(CreateProductRequestDto createProductRequestDto) {
        Product product = new Product(
                createProductRequestDto.getName(),
                createProductRequestDto.getDescription(),
                createProductRequestDto.getPrice(),
                createProductRequestDto.getRating(),
                createProductRequestDto.getCategory()
        );
        Product savedProduct = productRepository.save(product);
        ProductDocument productDocument = new ProductDocument(
                savedProduct.getId().toString(),
                savedProduct.getName(),
                savedProduct.getDescription(),
                savedProduct.getPrice(),
                savedProduct.getRating(),
                savedProduct.getCategory()
        );
        productDocumentRepository.save(productDocument);
        return savedProduct;
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        productDocumentRepository.deleteById(id.toString());
    }
}
