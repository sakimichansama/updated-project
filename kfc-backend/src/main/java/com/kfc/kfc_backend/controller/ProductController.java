package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.entity.Product;
import com.kfc.kfc_backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")   // Use plural path
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"}) // Allow frontend CORS
public class ProductController {

    @Autowired
    private ProductService productService;

    // GET /api/products   -> Get all products
    @GetMapping
    public List<Product> list() {
        return productService.findAll();
    }

    // POST /api/products  -> New Product
    @PostMapping
    public Product add(@RequestBody Product product) {
        return productService.save(product);
    }

    // PUT /api/products/{id} -> Update product
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        return productService.save(product);
    }

    // DELETE /api/products/{id} -> DeleteProduct
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productService.deleteById(id);
    }
}

