package dev.aryan.ecommerceapi.entity

/**
 * Machine-readable stock status, mapped from dummyjson's display strings during
 * ingestion (e.g. "In Stock" -> [IN_STOCK]) and stored via `@Enumerated(EnumType.STRING)`
 * - never `EnumType.ORDINAL`, which would silently break if this enum's declaration
 * order ever changed.
 *
 * Kept as its own field despite [Product.stock] existing - it encodes the source
 * system's business logic (thresholds, possible manual overrides) that isn't
 * derivable from the raw stock count alone.
 */
enum class AvailabilityStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
}
