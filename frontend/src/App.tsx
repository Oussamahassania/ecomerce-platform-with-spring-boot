import { lazy, Suspense, useEffect, type ReactNode } from "react";
import { Link, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { toast } from "sonner";
import { ArrowRight, ShieldAlert } from "lucide-react";
import { useAuth } from "@/stores/auth";
import { Layout } from "@/components/layout/Layout";
import { HomePage } from "@/pages/HomePage";
import { ShopPage } from "@/pages/ShopPage";
import { ProductPage } from "@/pages/ProductPage";
import { CartPage } from "@/pages/CartPage";
import { CheckoutPage } from "@/pages/CheckoutPage";
import { OrdersPage } from "@/pages/OrdersPage";
import { OrderDetailPage } from "@/pages/OrderDetailPage";
import { AccountPage } from "@/pages/AccountPage";
import { WishlistPage } from "@/pages/WishlistPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { LoginPage } from "@/pages/auth/LoginPage";
import { RegisterPage } from "@/pages/auth/RegisterPage";
import { VerifyPage } from "@/pages/auth/VerifyPage";

const AdminLayout = lazy(() =>
  import("@/pages/admin/AdminLayout").then((module) => ({
    default: module.AdminLayout,
  })),
);
const AdminDashboardPage = lazy(() =>
  import("@/pages/admin/DashboardPage").then((module) => ({
    default: module.AdminDashboardPage,
  })),
);
const AdminProductsPage = lazy(() =>
  import("@/pages/admin/ProductsPage").then((module) => ({
    default: module.AdminProductsPage,
  })),
);
const AdminCategoriesPage = lazy(() =>
  import("@/pages/admin/CategoriesPage").then((module) => ({
    default: module.AdminCategoriesPage,
  })),
);
const AdminOrdersPage = lazy(() =>
  import("@/pages/admin/OrdersPage").then((module) => ({
    default: module.AdminOrdersPage,
  })),
);
const AdminCustomersPage = lazy(() =>
  import("@/pages/admin/CustomersPage").then((module) => ({
    default: module.AdminCustomersPage,
  })),
);

function RequireAuth({ children }: { children: ReactNode }) {
  const token = useAuth((state) => state.token);
  const location = useLocation();

  if (!token) {
    const next = location.pathname + location.search + location.hash;
    return (
      <Navigate to={`/login?next=${encodeURIComponent(next)}`} replace />
    );
  }

  return <>{children}</>;
}

function ForbiddenPage() {
  return (
    <div className="container-x flex min-h-[60vh] items-center justify-center py-16">
      <div className="max-w-md text-center">
        <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-ink text-paper">
          <ShieldAlert className="h-5 w-5" />
        </span>
        <p className="mt-5 text-[11px] uppercase tracking-[0.16em] text-muted">
          403 — admin only
        </p>
        <h1 className="mt-2 font-display text-4xl leading-none tracking-[-0.05em]">
          This door is for the studio.
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-muted">
          Your account does not have admin access. If you think that is wrong,
          ask the shop owner to change your role.
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <Link
            to="/account"
            className="inline-flex h-11 items-center gap-2 rounded-full bg-ink px-6 text-sm font-medium text-paper"
          >
            Your account
            <ArrowRight className="h-4 w-4" />
          </Link>
          <Link
            to="/shop"
            className="inline-flex h-11 items-center rounded-full border border-ink/25 px-6 text-sm font-medium transition-colors hover:border-ink"
          >
            Back to the shop
          </Link>
        </div>
      </div>
    </div>
  );
}

function AdminLoading() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-paper">
      <div className="text-center">
        <span className="mx-auto block h-8 w-8 animate-spin rounded-full border-2 border-ink/15 border-t-ink" />
        <p className="mt-3 text-[12px] text-muted">Opening the studio…</p>
      </div>
    </div>
  );
}

function RequireAdmin({ children }: { children: ReactNode }) {
  const token = useAuth((state) => state.token);
  const role = useAuth((state) => state.role);
  const location = useLocation();

  if (!token) {
    const next = location.pathname + location.search + location.hash;
    return (
      <Navigate to={`/login?next=${encodeURIComponent(next)}`} replace />
    );
  }

  if (role !== "ADMIN") return <ForbiddenPage />;

  return <>{children}</>;
}

export default function App() {
  const logout = useAuth((state) => state.logout);

  useEffect(() => {
    function handleUnauthorized() {
      logout();
      toast.error("Your session expired", {
        id: "session-expired",
        description: "Sign in again to pick up where you left off.",
      });
    }

    window.addEventListener("forma:unauthorized", handleUnauthorized);
    return () =>
      window.removeEventListener("forma:unauthorized", handleUnauthorized);
  }, [logout]);

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="shop" element={<ShopPage />} />
        <Route path="product/:id" element={<ProductPage />} />
        <Route
          path="cart"
          element={
            <RequireAuth>
              <CartPage />
            </RequireAuth>
          }
        />
        <Route
          path="checkout"
          element={
            <RequireAuth>
              <CheckoutPage />
            </RequireAuth>
          }
        />
        <Route
          path="orders"
          element={
            <RequireAuth>
              <OrdersPage />
            </RequireAuth>
          }
        />
        <Route
          path="orders/:id"
          element={
            <RequireAuth>
              <OrderDetailPage />
            </RequireAuth>
          }
        />
        <Route
          path="account"
          element={
            <RequireAuth>
              <AccountPage />
            </RequireAuth>
          }
        />
        <Route path="wishlist" element={<WishlistPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="verify" element={<VerifyPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      <Route
        path="admin"
        element={
          <RequireAdmin>
            <Suspense fallback={<AdminLoading />}>
              <AdminLayout />
            </Suspense>
          </RequireAdmin>
        }
      >
        <Route index element={<AdminDashboardPage />} />
        <Route path="products" element={<AdminProductsPage />} />
        <Route path="categories" element={<AdminCategoriesPage />} />
        <Route path="orders" element={<AdminOrdersPage />} />
        <Route path="customers" element={<AdminCustomersPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
