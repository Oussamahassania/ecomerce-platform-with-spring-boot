import { useMemo } from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import {
  ArrowRight,
  ArrowUpRight,
  CircleAlert,
  CircleDollarSign,
  Package,
  Plus,
  ShoppingBag,
  Tags,
  Users,
} from "lucide-react";
import {
  useAdminOrderCount,
  useAdminOrders,
  useAdminUsers,
  useAllProducts,
  useCategories,
  useLowStock,
} from "@/hooks/queries";
import { formatDate, initials, money } from "@/lib/format";
import { productImage } from "@/lib/images";
import type { Order } from "@/lib/types";
import {
  OrderStatusBadge,
  PaymentStatusBadge,
  Skeleton,
} from "@/components/ui";
import {
  AdminEmpty,
  AdminPageHeader,
  StatCard,
  TableShell,
} from "@/components/admin/AdminUI";

function isPaid(order: Order) {
  return order.paymentStatus === "PAID";
}

export function AdminDashboardPage() {
  const { data: orderCount } = useAdminOrderCount();
  const { data: orders, isLoading: ordersLoading } = useAdminOrders();
  const { data: users } = useAdminUsers();
  const { data: lowStock } = useLowStock();
  const { data: products } = useAllProducts();
  const { data: categories } = useCategories();

  const sortedOrders = useMemo(
    () =>
      [...(orders ?? [])].sort(
        (a, b) =>
          new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime(),
      ),
    [orders],
  );

  const paidRevenue = useMemo(
    () =>
      (orders ?? [])
        .filter(isPaid)
        .reduce((total, order) => total + Number(order.totalAmount), 0),
    [orders],
  );

  const paidCount = orders?.filter(isPaid).length ?? 0;
  const adminCount =
    users?.filter((user) => user.roleName === "ADMIN").length ?? 0;
  const customerCount = (users?.length ?? 0) - adminCount;

  const userMap = useMemo(
    () => new Map((users ?? []).map((user) => [user.id, user])),
    [users],
  );

  const categoryRows = useMemo(() => {
    const counts = new Map<number, number>();
    for (const product of products ?? []) {
      if (product.categoryId) {
        counts.set(
          product.categoryId,
          (counts.get(product.categoryId) ?? 0) + 1,
        );
      }
    }
    const max = Math.max(1, ...counts.values());
    return (categories ?? []).map((category) => ({
      ...category,
      count: counts.get(category.id) ?? 0,
      percent: Math.round(((counts.get(category.id) ?? 0) / max) * 100),
    }));
  }, [categories, products]);

  const recent = sortedOrders.slice(0, 6);

  return (
    <div>
      <AdminPageHeader
        eyebrow="Overview"
        title="The shop at a glance."
        description="Orders, stock and the people behind them. Numbers update as the backend changes."
        action={
          <>
            <Link
              to="/admin/products?new=1"
              className="inline-flex h-10 items-center gap-2 rounded-full bg-cobalt px-5 text-[13px] font-medium text-white transition-colors hover:bg-cobalt-ink"
            >
              <Plus className="h-4 w-4" />
              New product
            </Link>
            <Link
              to="/admin/orders"
              className="inline-flex h-10 items-center gap-2 rounded-full border border-ink/20 px-5 text-[13px] font-medium transition-colors hover:border-ink"
            >
              All orders
              <ArrowUpRight className="h-3.5 w-3.5" />
            </Link>
          </>
        }
      />

      <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Paid revenue"
          value={money(paidRevenue)}
          icon={<CircleDollarSign className="h-4 w-4" />}
          detail={`${paidCount} paid ${paidCount === 1 ? "order" : "orders"}`}
          tone="cobalt"
        />
        <StatCard
          label="Orders"
          value={orderCount ?? "—"}
          icon={<ShoppingBag className="h-4 w-4" />}
          detail="All statuses, all time"
        />
        <StatCard
          label="Customers"
          value={users?.length ?? "—"}
          icon={<Users className="h-4 w-4" />}
          detail={`${adminCount} admin${adminCount === 1 ? "" : "s"} · ${customerCount} customer${customerCount === 1 ? "" : "s"}`}
        />
        <StatCard
          label="Low stock"
          value={lowStock?.length ?? "—"}
          icon={<CircleAlert className="h-4 w-4" />}
          detail="Active products under 10 units"
          tone={lowStock && lowStock.length > 0 ? "butter" : "neutral"}
        />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-12">
        <section className="xl:col-span-8">
          <div className="flex items-end justify-between gap-4 pb-3">
            <div>
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Recent orders
              </h2>
              <p className="mt-0.5 text-[12px] text-muted">
                Newest first. Open an order for payment and delivery actions.
              </p>
            </div>
            <Link
              to="/admin/orders"
              className="link-underline inline-flex items-center gap-1.5 text-[12px] font-medium"
            >
              View all
              <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </div>

          <TableShell>
            {ordersLoading ? (
              <div className="space-y-4 p-5">
                {Array.from({ length: 4 }).map((_, index) => (
                  <div key={index} className="flex items-center gap-4">
                    <Skeleton className="h-10 w-10 rounded-full" />
                    <Skeleton className="h-3 flex-1" />
                    <Skeleton className="h-3 w-20" />
                    <Skeleton className="h-6 w-24" />
                  </div>
                ))}
              </div>
            ) : recent.length === 0 ? (
              <AdminEmpty
                title="No orders yet."
                description="When a customer checks out, the order appears here."
              />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[640px] text-left text-[13px]">
                  <thead className="border-b border-line bg-paper-3/60 text-[10px] uppercase tracking-[0.12em] text-muted">
                    <tr>
                      <th className="px-5 py-3 font-medium">Order</th>
                      <th className="px-5 py-3 font-medium">Customer</th>
                      <th className="px-5 py-3 font-medium">Date</th>
                      <th className="px-5 py-3 font-medium">Total</th>
                      <th className="px-5 py-3 font-medium">Status</th>
                      <th className="px-5 py-3" />
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-line">
                    {recent.map((order) => {
                      const customer = userMap.get(order.userId);
                      return (
                        <tr key={order.id} className="table-row-hover">
                          <td className="px-5 py-3.5">
                            <Link
                              to={`/orders/${order.id}`}
                              className="font-medium tabular-nums hover:text-cobalt"
                            >
                              FRM-{String(order.id).padStart(5, "0")}
                            </Link>
                            <p className="mt-0.5 text-[10px] text-muted">
                              {order.items.length} lines
                            </p>
                          </td>
                          <td className="px-5 py-3.5">
                            <div className="flex items-center gap-2.5">
                              <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-paper-2 text-[9px] font-semibold text-ink-2">
                                {initials(customer?.fullName)}
                              </span>
                              <div className="min-w-0">
                                <p className="max-w-36 truncate text-[12px]">
                                  {customer?.fullName ?? `User #${order.userId}`}
                                </p>
                                <p className="max-w-36 truncate text-[10px] text-muted">
                                  {customer?.email ?? "—"}
                                </p>
                              </div>
                            </div>
                          </td>
                          <td className="px-5 py-3.5 text-[12px] text-muted">
                            {formatDate(order.orderDate)}
                          </td>
                          <td className="px-5 py-3.5 tabular-nums">
                            {money(order.totalAmount)}
                          </td>
                          <td className="px-5 py-3.5">
                            <div className="flex flex-wrap gap-1">
                              <OrderStatusBadge status={order.status} />
                              <PaymentStatusBadge
                                status={order.paymentStatus}
                              />
                            </div>
                          </td>
                          <td className="px-5 py-3.5 text-right">
                            <Link
                              to={`/orders/${order.id}`}
                              aria-label={`Open order ${order.id}`}
                              className="inline-flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-ink hover:text-paper"
                            >
                              <ArrowUpRight className="h-3.5 w-3.5" />
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </TableShell>
        </section>

        <section className="space-y-6 xl:col-span-4">
          <div>
            <div className="flex items-end justify-between gap-3 pb-3">
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Low stock
              </h2>
              <Link
                to="/admin/products"
                className="text-[11px] text-muted hover:text-ink"
              >
                Manage
              </Link>
            </div>
            <div className="border border-line bg-paper">
              {!lowStock ? (
                <div className="space-y-3 p-4">
                  {Array.from({ length: 3 }).map((_, index) => (
                    <Skeleton key={index} className="h-12 w-full" />
                  ))}
                </div>
              ) : lowStock.length === 0 ? (
                <div className="p-5 text-[12px] leading-relaxed text-muted">
                  Every active product has at least ten units. Nothing needs
                  restocking today.
                </div>
              ) : (
                <ul className="divide-y divide-line">
                  {lowStock.slice(0, 4).map((product) => (
                    <li key={product.id} className="flex items-center gap-3 p-3.5">
                      <div className="h-11 w-9 shrink-0 overflow-hidden bg-paper-2">
                        <img
                          src={productImage(product.img_url)}
                          alt=""
                          className="h-full w-full object-cover"
                        />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-[12px] font-medium">
                          {product.name}
                        </p>
                        <p className="text-[10px] text-muted">
                          {product.categoryName ?? "No category"}
                        </p>
                      </div>
                      <span className="rounded-full bg-butter px-2 py-1 text-[10px] font-medium tabular-nums text-ink">
                        {product.stock} left
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          <div>
            <div className="flex items-end justify-between gap-3 pb-3">
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Catalogue
              </h2>
              <Link
                to="/admin/categories"
                className="text-[11px] text-muted hover:text-ink"
              >
                Categories
              </Link>
            </div>
            <div className="border border-line bg-paper p-5">
              <p className="text-[11px] uppercase tracking-[0.12em] text-muted">
                Active products by category
              </p>
              <div className="mt-4 space-y-3.5">
                {categoryRows.map((category) => (
                  <div key={category.id}>
                    <div className="flex items-baseline justify-between text-[12px]">
                      <span>{category.name}</span>
                      <span className="tabular-nums text-muted">
                        {category.count}
                      </span>
                    </div>
                    <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-ink/[0.07]">
                      <motion.div
                        initial={{ width: 0 }}
                        animate={{ width: `${category.percent}%` }}
                        transition={{ duration: 0.7, ease: "easeOut" }}
                        className="h-full rounded-full bg-cobalt"
                      />
                    </div>
                  </div>
                ))}
                {categoryRows.length === 0 ? (
                  <p className="text-[12px] text-muted">No categories yet.</p>
                ) : null}
              </div>
              <div className="mt-5 flex items-center justify-between border-t border-line pt-4 text-[12px]">
                <span className="text-muted">Products in catalogue</span>
                <span className="font-medium tabular-nums">
                  {products?.length ?? 0}
                </span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <Link
              to="/admin/products?new=1"
              className="flex flex-col justify-between border border-line bg-paper p-4 transition-colors hover:border-ink/30"
            >
              <Plus className="h-4 w-4 text-cobalt" />
              <span className="mt-6 text-[12px] font-medium">Add a product</span>
            </Link>
            <Link
              to="/admin/categories"
              className="flex flex-col justify-between border border-line bg-paper p-4 transition-colors hover:border-ink/30"
            >
              <Tags className="h-4 w-4 text-cobalt" />
              <span className="mt-6 text-[12px] font-medium">Edit categories</span>
            </Link>
            <Link
              to="/admin/customers"
              className="flex flex-col justify-between border border-line bg-paper p-4 transition-colors hover:border-ink/30"
            >
              <Users className="h-4 w-4 text-cobalt" />
              <span className="mt-6 text-[12px] font-medium">Customers</span>
            </Link>
            <Link
              to="/admin/orders"
              className="flex flex-col justify-between border border-line bg-paper p-4 transition-colors hover:border-ink/30"
            >
              <Package className="h-4 w-4 text-cobalt" />
              <span className="mt-6 text-[12px] font-medium">Fulfil orders</span>
            </Link>
          </div>
        </section>
      </div>
    </div>
  );
}
