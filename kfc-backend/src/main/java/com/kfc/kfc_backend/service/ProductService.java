package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.Product;
import com.kfc.kfc_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product save(Product product) {
        if (product.getStock() == null) {
            product.setStock(0);
        }
        if (product.getMinStock() == null) {
            product.setMinStock(0);
        }
        if (product.getPurchasePrice() == null) {
            product.setPurchasePrice(0.0);
        }
        if (product.getSalePrice() == null) {
            product.setSalePrice(0.0);
        }
        return productRepository.save(product);
    }

    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
}

