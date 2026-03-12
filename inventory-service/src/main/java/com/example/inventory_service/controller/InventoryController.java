package com.example.inventory_service.controller;

import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/inventory")
@Slf4j
public class InventoryController {

    @GetMapping("/{productId}")
    public boolean isInStock(@PathVariable String productId, @RequestParam int quantity) {
        log.info("Checking inventory for product {} with quantity {}", productId, quantity);
        return "P100".equals(productId) && quantity <= 10;
    }
}