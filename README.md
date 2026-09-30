# FreshMart: Relational Grocery & Inventory Allocation System

FreshMart is a terminal-based grocery retail and inventory management application built with Java 17 and a normalized MySQL 8 relational database. Designed to handle the logistical demands of perishable retail goods, the system models real-world supermarket operations with an emphasis on **First-Expire, First-Out (FEFO)** stock depletion, delivery capacity management, and transactional integrity.

---

## Key Features

- **FEFO Inventory Allocation:** Tracks perishable items by discrete batches with arrival and expiration dates. Checkout logic depletes the earliest expiring unexpired stock first to minimize warehouse spoilage.
- **Delivery Slot Reservation:** Daily delivery windows enforce strict capacity caps at the database layer to eliminate overbooking during simultaneous customer checkouts.
- **Role-Based Access Control (RBAC):**
  - **Customer Portal:** Browse categorized products, add items to a real-time cart, pick eligible delivery slots, and review order receipts with assigned batch numbers.
  - **Admin Console:** Add and manage product SKUs, adjust pricing, inspect batch shelf-life alerts, monitor stock levels, and update order statuses.
- **Cryptographic Security & Pricing Snapshots:** User credentials use SHA-256 (`SHA2`) hashing at the database layer. Order line items snapshot and freeze unit prices at checkout to preserve historical accounting records against future price changes.
- **Optimized SQL Views:** Precomputed views (`v_catalog`, `v_slots`) combine product metadata, live batch availability, discount tags, and slot capacity into single-trip queries.

---

## Tech Stack & Prerequisites

- **Language:** Java 17+
- **Build Tool:** Apache Maven 3.9+
- **Database:** MySQL Server 8.0+
- **Connectivity:** JDBC (MySQL Connector/J)
- **OS:** Windows / macOS / Linux

---

## Project Structure

```text
freshmart-java/
├── pom.xml                  # Maven dependencies and exec configuration
├── upgrade.sql              # Database migrations, seed accounts, and views
└── src/
    └── main/
        └── java/
            ├── App.java     # CLI entry point, session routing, and main menu
            ├── Db.java      # JDBC connection lifecycle and queries
            ├── Shop.java    # Customer catalog, cart, and checkout workflows
            └── Admin.java   # Inventory, batch management, and admin utilities
