---
description: Product Definition and product vision for the dual-interface digital ecosystem connecting customers and merchants through inventory-aware order orchestration.
globs: ["**/*.kt", "**/docs/**/*.md"]
alwaysApply: true
---

# Product Definition — Dual-Interface Commerce Ecosystem

## Table of Contents

- [Product Overview](#product-overview)
- [Target Users & Personas](#target-users--personas)
  - [Customer Persona](#customer-persona)
  - [Merchant Persona](#merchant-persona)
- [Core Value Proposition](#core-value-proposition)
- [Product Architecture](#product-architecture)
  - [Ecosystem Overview](#ecosystem-overview)
  - [Interface Responsibilities](#interface-responsibilities)
- [Key User Flows](#key-user-flows)
  - [Customer — Product Discovery to Reservation](#customer--product-discovery-to-reservation)
  - [Merchant — Order Reception to Fulfillment](#merchant--order-reception-to-fulfillment)
- [Out of Scope](#out-of-scope)
- [Glossary](#glossary)

---

## Product Overview

This product is a **dual-interface digital ecosystem** designed to bridge the gap between a customer's digital purchase intent and a merchant's physical inventory availability. It is not a traditional e-commerce platform or a Point of Sale (POS) system — its core specialization is **order orchestration** and **real-time stock visibility**.

The ecosystem allows retailers to digitize their product offering without the administrative overhead of processing online payments or managing complex accounting systems. It is designed to be fully operable from a mobile device with a minimal learning curve.

The system consists of two tightly integrated interfaces:
- **Customer Mobile Application** — a UX-first product discovery and reservation experience.
- **Merchant Operational Tool** — a real-time inventory and order management console.

---

## Target Users & Personas

### Customer Persona

| Attribute | Description |
|---|---|
| Role | End consumer browsing and reserving products |
| Goal | Discover available products and place a purchase-intent reservation |
| Pain Point | Arriving at a store to find items out of stock, or discovering products online that are actually unavailable |
| Key Need | Real-time stock visibility before committing to a visit or pickup |
| Device | Mobile (primary) |

### Merchant Persona

| Attribute | Description |
|---|---|
| Role | Store owner or sales staff managing inventory and fulfilling orders |
| Goal | Digitize product catalog and manage inbound reservations efficiently |
| Pain Point | Manual stock tracking, phone-based order coordination, lack of customer purchase history |
| Key Need | A simple, mobile-first tool to manage products and fulfill orders in real time |
| Device | Mobile (100% mobile-operated) |

---

## Core Value Proposition

The ecosystem solves a specific, underserved problem in physical retail: the disconnect between digital intent and physical reality. It operates at the intersection of three capabilities:

```
Digital Catalog Visibility  +  Real-Time Stock Awareness  +  Order Intent Orchestration
         │                              │                               │
         ▼                              ▼                               ▼
  Customer discovers          Customer sees only what         Merchant receives structured
  products digitally          is actually in stock            purchase-intent orders
```

**What this product is NOT:**
- It does not process online payments or replace a payment gateway.
- It does not replace a full POS or accounting system.
- It does not manage logistics or last-mile delivery infrastructure.

---

## Product Architecture

### Ecosystem Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Dual-Interface Ecosystem                   │
├──────────────────────────┬──────────────────────────────────┤
│   Customer Mobile App    │   Merchant Operational Tool       │
│                          │                                   │
│  • Product Discovery     │  • Inventory Management           │
│  • Reservation Engine    │  • Order Management Console       │
│  • Interaction Profile   │  • Catalog Sync Controls          │
└──────────────────────────┴──────────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │      Shared Backend      │
              │  • Product Catalog       │
              │  • Stock Ledger          │
              │  • Order State Machine   │
              │  • Customer Profiles     │
              └──────────────────────────┘
```

### Interface Responsibilities

| Interface | Primary Actor | Core Responsibility | Platform |
|---|---|---|---|
| Customer Mobile App | Customer | Browse catalog, place reservation orders | Mobile (Android) |
| Merchant Operational Tool | Store owner / Sales staff | Manage inventory, fulfill orders | Mobile (Android) |

---

## Key User Flows

### Customer — Product Discovery to Reservation

```
1. Customer opens app → browses merchant's available catalog
2. Customer selects a product and variant (e.g., size M, color Blue)
3. Customer adds items to order
4. Customer submits Purchase Intent Order
5. System generates digital ticket; optionally locks stock
6. Customer awaits merchant confirmation
7. Merchant confirms → Customer is notified of status update
8. Customer coordinates pickup or delivery with merchant
```

### Merchant — Order Reception to Fulfillment

```
1. Merchant receives real-time reservation notification in Order Management Console
2. Merchant reviews order items and verifies stock availability
3. Merchant confirms or rejects the order
4. If confirmed: Merchant contacts customer to coordinate Delivery or Pick-up
5. Merchant marks order as Ready for Pickup / Out for Delivery
6. Merchant marks order as Completed upon handoff
```

---

## Out of Scope

The following capabilities are explicitly **not** part of this product and should not be implemented:

| Capability | Reason |
|---|---|
| Online payment processing | Increases regulatory and compliance burden; merchant handles payment offline |
| Accounting / bookkeeping | Out of product scope; handled by external tools |
| Last-mile delivery logistics | Merchant coordinates directly with customer; no logistics platform |
| Multi-merchant marketplace | Single-merchant per customer session; no aggregator view |
| Web interface (browser-based) | Both interfaces are mobile-native; no web app planned |

---

## Glossary

| Term | Definition |
|---|---|
| **PIO** | Purchase Intent Order — the core transaction object. Represents a customer's intent to purchase, not a confirmed or paid transaction. |
| **Stock Lock** | A temporary reservation of inventory quantity upon PIO creation, preventing overselling during the merchant's confirmation window. |
| **Catalog Sync** | The merchant's ability to instantly toggle product visibility in the customer-facing catalog via a single control. |
| **Availability-Assisted Navigation** | The customer catalog experience in which only in-stock, visible products are displayed, powered by real-time stock data. |
| **Delivery Method** | The fulfillment mode for a confirmed order: either **Pick-up** (customer collects at merchant location) or **Delivery** (merchant arranges delivery to customer). |
| **Interaction Profile** | A lightweight customer profile maintained by the merchant, storing favorites and reservation history to facilitate re-engagement and loyalty. |
| **Variant** | A product attribute combination (e.g., size + color) that differentiates stock-keeping units within a single product listing. |

---

This document is the authoritative Product Definition for the dual-interface commerce ecosystem. All feature development, API design, and domain modeling decisions must align with the product vision, business rules, and domain model defined here. Proposed scope changes require review and an update to this document before implementation begins.
