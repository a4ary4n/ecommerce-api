package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Review
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<Review, Long>
