# FORMA — Spring Boot + React storefront

FORMA is a small design-object shop: a Spring Boot commerce API with a React storefront and an admin studio. The frontend covers the full backend surface — catalogue, search and filters, cart, simulated checkout and payment, orders, reviews, wishlist, account, and admin management.

## Stack

- **Backend:** Spring Boot 4, Spring Security, JPA/Hibernate, JWT, H2 (dev), Maven
- **Frontend:** React 19, TypeScript, Vite 8, Tailwind CSS v4, Motion, TanStack Query, Zustand, React Router 7, Sonner
- **Design:** Bricolage Grotesque + Instrument Sans, paper / ink / cobalt / butter palette

## Quick start

### 1. Backend

Java 17+ and the included Maven wrapper are all you need. The `dev` profile uses a local H2 file database, a simple in-memory cache, and seeds demo data — no Postgres, Redis, or SMTP required.

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

```bash
# macOS / Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The API runs at `http://localhost:8080`. The H2 database lives in `./data/forma`; delete that folder to reseed from scratch.

### 2. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` to the backend, so keep both processes running.

Useful scripts:

```powershell
npm run typecheck   # TypeScript only
npm run build       # typecheck + production build
npm run preview     # serve the production build
```

## Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Customer | `demo@forma.dev` | `Demo1234!` |
| Admin | `admin@forma.dev` | `Admin123!` |

The login page has a “Local demo accounts” helper that fills either account.

## What you can try

### Storefront

- Animated home page with a hero, new arrivals rail, category index, editorial sections, and marquee
- Shop with live search, category/price filters, sorting, and pagination
- Product pages with stock state, quantity, wishlist, reviews, rating breakdown, and related objects
- Persistent server-side cart with optimistic quantity updates and a slide-over bag
- Simulated checkout: delivery details stay in the browser; payment is processed against the order
- Orders list and detail with payment retry, cancellation, and refund actions
- Account overview, review writing (requires a non-cancelled order for that product), and local wishlist
- Responsive layouts, reduced-motion support, page transitions, and keyboard-friendly overlays

### Admin studio (`/admin`)

- Dashboard with paid revenue, order/user counts, low stock, category distribution, and recent orders
- Product CRUD with image upload, stock edits, and soft delete
- Category management
- Order fulfilment status, cancellation, and refunds
- Customer list with role changes and account deletion

## Dev-profile notes

- **Email verification:** registration works, but there is no SMTP server in dev. The app tries to send mail and logs the failure. Use the helper on the Register page or:
  ```http
  POST http://localhost:8080/api/dev/verify?email=you@example.com
  ```
- **Payments:** the gateway is simulated and approves roughly 9 in 10 attempts. A declined payment leaves the order in place with a **Pay now** retry action. Retries use a fresh idempotency key.
- **Uploads:** product images are written to `uploads/products` and served from `/api/files/{name}`. The endpoint is spelled `POST /api/files/uplaod` (multipart field `file`) in the original backend.
- **CORS:** allowed origins for dev are `localhost` and `127.0.0.1` on ports `5173` and `4173`. Use `http://localhost:5173` when testing in a browser.
- **Seeded data:** 10 products across 5 categories, 3 reviews, 1 delivered order, and the two demo accounts. Seeding never overwrites an existing database.

## Project layout

```
ecommerce-platform/
├─ src/main/java/...          Spring Boot API
├─ src/main/resources/
│  └─ application-dev.yaml    H2 + dev CORS + uploads + JWT
├─ data/                      H2 database files (created on first run)
├─ uploads/                   Uploaded product images
└─ frontend/
   ├─ public/images/products/ Curated product photography
   └─ src/
      ├─ components/          Layout, product, UI, admin, auth
      ├─ pages/               Storefront and admin routes
      ├─ hooks/               React Query API hooks
      ├─ stores/              Auth, cart drawer, UI, wishlist
      └─ lib/                 API client, formatting, types
```

## API surface used by the frontend

- `POST /api/auth/registration`, `POST /api/auth/login`, `GET /api/auth/verify`, `GET /api/users/me`
- `GET /api/products/AllProducts`, `GET /api/products/{id}`, `GET /api/products/search`, `GET /api/products/filter`
- `GET /api/categories`, `GET /api/review/{productId}`, `GET /api/review/{productId}/rating`
- `GET /api/cart`, `POST /api/cart/add`, `PUT /api/cart/update/{itemId}`, `DELETE /api/cart/remove/{itemId}`, `POST /api/cart/checkout`
- `GET /api/orders/my-orders`, `GET /api/orders/order/{id}`, `PUT /api/orders/{id}/cancel`
- `POST /api/payment/pay/{orderId}`, `GET /api/payment/{orderId}`, `POST /api/payment/refund/{orderId}`
- Admin: `/api/orders/AllOrders`, `/api/orders/{id}/status`, product/category CRUD, user roles, low-stock and dashboard stats

All payment, delivery, and email behaviour is simulated for the demo. No real card is charged and no real delivery is arranged.
