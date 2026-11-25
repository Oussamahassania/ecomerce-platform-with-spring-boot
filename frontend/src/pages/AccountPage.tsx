import { Link, useNavigate } from "react-router-dom";
import { motion } from "motion/react";
import {
  ArrowRight,
  Heart,
  LogOut,
  Package,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
} from "lucide-react";
import { toast } from "sonner";
import { useMe, useMyOrders } from "@/hooks/queries";
import { formatDate, initials, money, pluralize } from "@/lib/format";
import { productImage } from "@/lib/images";
import { useAuth } from "@/stores/auth";
import { useWishlist } from "@/stores/wishlist";
import {
  EmptyState,
  OrderStatusBadge,
  PaymentStatusBadge,
  Skeleton,
} from "@/components/ui";

export function AccountPage() {
  const navigate = useNavigate();
  const logout = useAuth((state) => state.logout);
  const role = useAuth((state) => state.role);
  const { data: me, isLoading } = useMe();
  const { data: orders } = useMyOrders();
  const wishlistCount = useWishlist((state) => state.items.length);

  const objectCount =
    orders?.reduce(
      (total, order) =>
        total + order.items.reduce((sum, item) => sum + item.quantity, 0),
      0,
    ) ?? 0;
  const spent =
    orders
      ?.filter((order) => order.paymentStatus === "PAID")
      .reduce((total, order) => total + Number(order.totalAmount), 0) ?? 0;
  const recent = orders?.slice(0, 3) ?? [];

  function signOut() {
    logout();
    toast.success("Signed out");
    navigate("/");
  }

  if (isLoading) {
    return (
      <div className="container-x py-12">
        <Skeleton className="h-3 w-24" />
        <Skeleton className="mt-4 h-14 w-80" />
        <div className="mt-10 grid gap-8 lg:grid-cols-12">
          <div className="lg:col-span-4">
            <Skeleton className="h-80 w-full" />
          </div>
          <div className="space-y-5 lg:col-span-8">
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-64 w-full" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container-x py-10 md:py-14">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line pb-6">
        <div>
          <p className="text-[11px] uppercase tracking-[0.14em] text-muted">
            Your account
          </p>
          <h1 className="mt-2 font-display text-[clamp(2.6rem,5.6vw,4.6rem)] font-semibold leading-[0.94] tracking-[-0.05em]">
            Hello, {me?.fullName.split(" ")[0] ?? "there"}
            <span className="text-cobalt">.</span>
          </h1>
          <p className="mt-3 max-w-lg text-sm leading-relaxed text-muted">
            Your orders, saved objects and the small details of your account in
            one place.
          </p>
        </div>
        {role === "ADMIN" ? (
          <Link
            to="/admin"
            className="inline-flex h-10 items-center gap-2 rounded-full bg-ink px-5 text-[13px] font-medium text-paper transition-colors hover:bg-ink-2"
          >
            <ShieldCheck className="h-4 w-4" />
            Admin studio
          </Link>
        ) : null}
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-12 lg:gap-12">
        <aside className="lg:col-span-4">
          <div className="border border-line bg-paper-3/50 p-6">
            <div className="flex items-center gap-4">
              <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-cobalt font-display text-xl font-semibold text-white">
                {initials(me?.fullName)}
              </span>
              <div className="min-w-0">
                <p className="truncate font-display text-2xl tracking-[-0.035em]">
                  {me?.fullName}
                </p>
                <p className="truncate text-[12px] text-muted">{me?.email}</p>
              </div>
            </div>

            <dl className="mt-6 space-y-3 border-t border-line pt-5 text-[12px]">
              <div className="flex items-center justify-between">
                <dt className="text-muted">Member since</dt>
                <dd>{formatDate(me?.createdAt)}</dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted">Account type</dt>
                <dd className="inline-flex items-center gap-1.5">
                  {role === "ADMIN" ? (
                    <Sparkles className="h-3.5 w-3.5 text-cobalt" />
                  ) : null}
                  {role === "ADMIN" ? "Shop admin" : "Customer"}
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-muted">Date of birth</dt>
                <dd>{formatDate(me?.dateOfBirth)}</dd>
              </div>
            </dl>

            <nav className="mt-6 space-y-1 border-t border-line pt-5">
              <Link
                to="/orders"
                className="flex items-center justify-between rounded-sm px-2 py-2.5 text-[13px] transition-colors hover:bg-ink/[0.05]"
              >
                <span className="inline-flex items-center gap-2.5">
                  <Package className="h-4 w-4 text-muted" />
                  Orders
                </span>
                <span className="text-[11px] tabular-nums text-muted">
                  {orders?.length ?? 0}
                </span>
              </Link>
              <Link
                to="/wishlist"
                className="flex items-center justify-between rounded-sm px-2 py-2.5 text-[13px] transition-colors hover:bg-ink/[0.05]"
              >
                <span className="inline-flex items-center gap-2.5">
                  <Heart className="h-4 w-4 text-muted" />
                  Wishlist
                </span>
                <span className="text-[11px] tabular-nums text-muted">
                  {wishlistCount}
                </span>
              </Link>
              <button
                type="button"
                onClick={signOut}
                className="flex w-full items-center gap-2.5 rounded-sm px-2 py-2.5 text-left text-[13px] text-danger transition-colors hover:bg-danger/[0.06]"
              >
                <LogOut className="h-4 w-4" />
                Sign out
              </button>
            </nav>
          </div>

          <p className="mt-4 px-1 text-[11px] leading-relaxed text-muted">
            This demo does not include profile editing. Your name and email come
            from the Spring Boot account record.
          </p>
        </aside>

        <div className="space-y-8 lg:col-span-8">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {[
              { label: "Orders", value: orders?.length ?? 0, icon: Package },
              { label: "Objects", value: objectCount, icon: ShoppingBag },
              { label: "Wishlist", value: wishlistCount, icon: Heart },
              { label: "Paid", value: money(spent), icon: Sparkles, wide: true },
            ].map((stat, index) => (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05, duration: 0.35 }}
                className={
                  stat.wide
                    ? "col-span-2 border border-line bg-paper-3/40 p-4 sm:col-span-1"
                    : "border border-line bg-paper-3/40 p-4"
                }
              >
                <stat.icon className="h-4 w-4 text-muted" />
                <p className="mt-4 font-display text-2xl leading-none tracking-[-0.04em] tabular-nums">
                  {stat.value}
                </p>
                <p className="mt-1 text-[11px] text-muted">{stat.label}</p>
              </motion.div>
            ))}
          </div>

          <section>
            <div className="flex items-end justify-between gap-4 border-b border-line pb-3">
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Recent orders
              </h2>
              <Link
                to="/orders"
                className="link-underline inline-flex items-center gap-1.5 text-[12px] font-medium"
              >
                All orders
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>

            {recent.length === 0 ? (
              <EmptyState
                title="No orders yet."
                description="The first one is always a good one. Start with the newest objects on the shelf."
                action={
                  <Link
                    to="/shop"
                    className="inline-flex h-11 items-center rounded-full bg-ink px-6 text-sm font-medium text-paper"
                  >
                    Browse the shop
                  </Link>
                }
              />
            ) : (
              <ul className="divide-y divide-line">
                {recent.map((order) => {
                  const count = order.items.reduce(
                    (total, item) => total + item.quantity,
                    0,
                  );
                  return (
                    <li key={order.id}>
                      <Link
                        to={`/orders/${order.id}`}
                        className="group flex flex-wrap items-center justify-between gap-3 py-4"
                      >
                        <div className="flex items-center gap-4">
                          <div className="h-14 w-12 shrink-0 overflow-hidden bg-paper-2">
                            <img
                              src={productImage(order.items[0]?.productImage)}
                              alt=""
                              className="h-full w-full object-cover"
                            />
                          </div>
                          <div>
                            <p className="text-[12px] font-medium">
                              FRM-{String(order.id).padStart(5, "0")}
                            </p>
                            <p className="text-[11px] text-muted">
                              {formatDate(order.orderDate)} · {count}{" "}
                              {pluralize(count, "object")}
                            </p>
                            <div className="mt-1.5 flex flex-wrap gap-1.5">
                              <OrderStatusBadge status={order.status} />
                              <PaymentStatusBadge
                                status={order.paymentStatus}
                              />
                            </div>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-sm font-medium tabular-nums">
                            {money(order.totalAmount)}
                          </span>
                          <ArrowRight className="h-4 w-4 text-muted transition-transform group-hover:translate-x-0.5 group-hover:text-ink" />
                        </div>
                      </Link>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
