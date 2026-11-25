import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import { ArrowRight, Package, ShoppingBag } from "lucide-react";
import { useMyOrders } from "@/hooks/queries";
import { formatDate, money, pluralize } from "@/lib/format";
import { productImage } from "@/lib/images";
import type { Order, OrderStatus } from "@/lib/types";
import {
  EmptyState,
  OrderStatusBadge,
  PaymentStatusBadge,
  Skeleton,
  cn,
} from "@/components/ui";

const FILTERS: { value: "ALL" | OrderStatus; label: string }[] = [
  { value: "ALL", label: "All orders" },
  { value: "PENDING", label: "Pending" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "SHIPPED", label: "Shipped" },
  { value: "DELIVERED", label: "Delivered" },
  { value: "CANCELLED", label: "Cancelled" },
];

function itemCount(order: Order) {
  return order.items.reduce((total, item) => total + item.quantity, 0);
}

function OrderCard({ order, index }: { order: Order; index: number }) {
  const count = itemCount(order);
  const names = order.items.map((item) => item.productName).filter(Boolean);

  return (
    <motion.li
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05, duration: 0.4 }}
      className="group border-b border-line py-6"
    >
      <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex items-start gap-4 sm:gap-5">
          <div className="flex shrink-0 -space-x-3">
            {order.items.slice(0, 3).map((item, itemIndex) => (
              <div
                key={`${item.productId}-${itemIndex}`}
                className="h-20 w-16 overflow-hidden border-2 border-paper bg-paper-2 sm:h-24 sm:w-20"
                style={{ zIndex: 3 - itemIndex }}
              >
                <img
                  src={productImage(item.productImage)}
                  alt={item.productName ?? "Ordered object"}
                  className="h-full w-full object-cover"
                />
              </div>
            ))}
            {order.items.length > 3 ? (
              <div className="flex h-20 w-16 items-center justify-center border-2 border-paper bg-ink text-[11px] font-medium text-paper sm:h-24 sm:w-20">
                +{order.items.length - 3}
              </div>
            ) : null}
          </div>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-[11px] uppercase tracking-[0.12em] text-muted">
                FRM-{String(order.id).padStart(5, "0")}
              </p>
              <span className="h-1 w-1 rounded-full bg-line" />
              <p className="text-[11px] text-muted">
                {formatDate(order.orderDate)}
              </p>
            </div>
            <p className="mt-1.5 max-w-md truncate font-display text-xl tracking-[-0.03em] sm:text-2xl">
              {names.length > 0 ? names.join(", ") : "Your order"}
            </p>
            <p className="mt-1 text-[12px] text-muted">
              {count} {pluralize(count, "object")} · {money(order.totalAmount)}
            </p>
            <div className="mt-3 flex flex-wrap items-center gap-1.5">
              <OrderStatusBadge status={order.status} />
              <PaymentStatusBadge status={order.paymentStatus} />
            </div>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2 lg:pl-6">
          {order.paymentStatus !== "PAID" &&
          order.paymentStatus !== "REFUNDED" &&
          order.status !== "CANCELLED" ? (
            <Link
              to={`/orders/${order.id}`}
              className="inline-flex h-10 items-center rounded-full bg-cobalt px-5 text-[13px] font-medium text-white transition-colors hover:bg-cobalt-ink"
            >
              Complete payment
            </Link>
          ) : null}
          <Link
            to={`/orders/${order.id}`}
            className="group/link inline-flex h-10 items-center gap-1.5 rounded-full border border-ink/20 px-5 text-[13px] font-medium transition-colors hover:border-ink hover:bg-ink hover:text-paper"
          >
            View order
            <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover/link:translate-x-0.5" />
          </Link>
        </div>
      </div>
    </motion.li>
  );
}

export function OrdersPage() {
  const { data: orders, isLoading } = useMyOrders();
  const [filter, setFilter] = useState<"ALL" | OrderStatus>("ALL");

  const counts = useMemo(() => {
    const map = new Map<string, number>();
    for (const order of orders ?? []) {
      map.set(order.status, (map.get(order.status) ?? 0) + 1);
    }
    return map;
  }, [orders]);

  const filtered = useMemo(
    () =>
      filter === "ALL"
        ? (orders ?? [])
        : (orders ?? []).filter((order) => order.status === filter),
    [filter, orders],
  );

  return (
    <div className="container-x py-10 md:py-14">
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-line pb-6">
        <div>
          <p className="text-[11px] uppercase tracking-[0.14em] text-muted">
            Your account
          </p>
          <h1 className="mt-2 font-display text-[clamp(2.6rem,5.6vw,4.6rem)] font-semibold leading-[0.94] tracking-[-0.05em]">
            Orders
          </h1>
          <p className="mt-3 max-w-lg text-sm leading-relaxed text-muted">
            Everything you have ordered, with payment and delivery status in
            one place.
          </p>
        </div>
        <Link
          to="/shop"
          className="link-underline inline-flex items-center gap-1.5 pb-1 text-[13px] font-medium"
        >
          Shop all objects
          <ArrowRight className="h-3.5 w-3.5" />
        </Link>
      </div>

      <div className="no-scrollbar mt-6 flex items-center gap-1.5 overflow-x-auto border-b border-line pb-3">
        {FILTERS.map((item) => {
          const count =
            item.value === "ALL"
              ? (orders?.length ?? 0)
              : (counts.get(item.value) ?? 0);
          return (
            <button
              key={item.value}
              type="button"
              onClick={() => setFilter(item.value)}
              aria-pressed={filter === item.value}
              className={cn(
                "shrink-0 rounded-full px-3.5 py-1.5 text-[12px] font-medium transition-colors",
                filter === item.value
                  ? "bg-ink text-paper"
                  : "text-ink-2 hover:bg-ink/[0.06]",
              )}
            >
              {item.label}
              <span
                className={cn(
                  "ml-1.5 tabular-nums",
                  filter === item.value ? "text-paper/60" : "text-muted",
                )}
              >
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {isLoading ? (
        <div className="divide-y divide-line">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="flex gap-5 py-6">
              <Skeleton className="h-24 w-20" />
              <div className="flex-1 space-y-3">
                <Skeleton className="h-3 w-40" />
                <Skeleton className="h-5 w-2/3" />
                <Skeleton className="h-3 w-28" />
              </div>
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          title={
            orders && orders.length > 0
              ? "No orders with that status."
              : "No orders yet."
          }
          description={
            orders && orders.length > 0
              ? "Try a different tab — the order may still be under another status."
              : "When you order something, it will appear here with its payment and delivery progress."
          }
          action={
            <Link
              to="/shop"
              className="inline-flex h-11 items-center gap-2 rounded-full bg-ink px-6 text-sm font-medium text-paper"
            >
              <ShoppingBag className="h-4 w-4" />
              Find something
            </Link>
          }
        />
      ) : (
        <>
          <ul>
            {filtered.map((order, index) => (
              <OrderCard key={order.id} order={order} index={index} />
            ))}
          </ul>
          <p className="mt-8 flex items-center gap-2 text-[11px] text-muted">
            <Package className="h-3.5 w-3.5" />
            Payments and deliveries are simulated in this demo store.
          </p>
        </>
      )}
    </div>
  );
}
