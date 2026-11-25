import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowUpRight,
  RefreshCw,
  Search,
  Trash2,
  X,
} from "lucide-react";
import { toast } from "sonner";
import {
  useAdminOrders,
  useAdminUsers,
  useCancelOrder,
  useDeleteOrder,
  useUpdateOrderStatus,
} from "@/hooks/queries";
import { formatDate, initials, money, pluralize } from "@/lib/format";
import type { Order, OrderStatus } from "@/lib/types";
import {
  Input,
  OrderStatusBadge,
  PaymentStatusBadge,
  Select,
  Skeleton,
  cn,
} from "@/components/ui";
import { ConfirmDialog } from "@/components/ui/Overlay";
import {
  AdminEmpty,
  AdminPageHeader,
  TableShell,
} from "@/components/admin/AdminUI";

const FILTERS: { value: "ALL" | OrderStatus; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "PENDING", label: "Pending" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "SHIPPED", label: "Shipped" },
  { value: "DELIVERED", label: "Delivered" },
  { value: "CANCELLED", label: "Cancelled" },
];

const STATUS_OPTIONS: OrderStatus[] = [
  "PENDING",
  "CONFIRMED",
  "SHIPPED",
  "DELIVERED",
];

export function AdminOrdersPage() {
  const { data: orders, isLoading, isFetching, refetch } = useAdminOrders();
  const { data: users } = useAdminUsers();
  const updateStatus = useUpdateOrderStatus();
  const cancelOrder = useCancelOrder();
  const deleteOrder = useDeleteOrder();

  const [filter, setFilter] = useState<"ALL" | OrderStatus>("ALL");
  const [search, setSearch] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<Order | null>(null);
  const [cancelTarget, setCancelTarget] = useState<Order | null>(null);

  const userMap = useMemo(
    () => new Map((users ?? []).map((user) => [user.id, user])),
    [users],
  );

  const counts = useMemo(() => {
    const map = new Map<string, number>();
    for (const order of orders ?? []) {
      map.set(order.status, (map.get(order.status) ?? 0) + 1);
    }
    return map;
  }, [orders]);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return (orders ?? [])
      .filter((order) => {
        if (filter !== "ALL" && order.status !== filter) return false;
        if (!term) return true;
        const customer = userMap.get(order.userId);
        return (
          String(order.id).includes(term) ||
          customer?.fullName.toLowerCase().includes(term) ||
          customer?.email.toLowerCase().includes(term)
        );
      })
      .sort(
        (a, b) =>
          new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime(),
      );
  }, [filter, orders, search, userMap]);

  function changeStatus(order: Order, status: OrderStatus) {
    if (order.status === status) return;
    updateStatus.mutate(
      { orderId: order.id, status },
      {
        onSuccess: () =>
          toast.success(
            `FRM-${String(order.id).padStart(5, "0")} marked ${status.toLowerCase()}`,
          ),
        onError: (error) => toast.error(error.message),
      },
    );
  }

  function confirmCancel() {
    if (!cancelTarget) return;
    cancelOrder.mutate(cancelTarget.id, {
      onSuccess: () => {
        setCancelTarget(null);
        toast.success("Order cancelled and stock restored");
      },
      onError: (error) => toast.error(error.message),
    });
  }

  function confirmDelete() {
    if (!deleteTarget) return;
    deleteOrder.mutate(deleteTarget.id, {
      onSuccess: () => {
        setDeleteTarget(null);
        toast.success("Order deleted");
      },
      onError: (error) => toast.error(error.message),
    });
  }

  return (
    <div>
      <AdminPageHeader
        eyebrow="Store"
        title="Orders"
        description="Move orders through fulfilment, cancel unpaid ones, and open any order for payment or refund actions."
        action={
          <button
            type="button"
            onClick={() => refetch()}
            className="inline-flex h-10 items-center gap-2 rounded-full border border-ink/20 px-5 text-[13px] font-medium transition-colors hover:border-ink"
          >
            <RefreshCw
              className={cn("h-3.5 w-3.5", isFetching && "animate-spin")}
            />
            Refresh
          </button>
        }
      />

      <div className="mt-5 flex flex-col gap-3 lg:flex-row lg:items-center">
        <div className="no-scrollbar flex items-center gap-1 overflow-x-auto">
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
        <div className="relative lg:ml-auto lg:w-64">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Order, name or email"
            aria-label="Search orders"
            className="h-10 rounded-full pl-10 text-[13px]"
          />
        </div>
      </div>

      <TableShell className="mt-4">
        {isLoading ? (
          <div className="space-y-4 p-5">
            {Array.from({ length: 5 }).map((_, index) => (
              <div key={index} className="flex items-center gap-4">
                <Skeleton className="h-3 w-20" />
                <Skeleton className="h-3 flex-1" />
                <Skeleton className="h-3 w-24" />
                <Skeleton className="h-9 w-32" />
              </div>
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <AdminEmpty
            title="No orders match."
            description="Try another status tab, or clear the search."
            action={
              <button
                type="button"
                onClick={() => {
                  setFilter("ALL");
                  setSearch("");
                }}
                className="inline-flex h-10 items-center rounded-full bg-ink px-5 text-[13px] font-medium text-paper"
              >
                Show all orders
              </button>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-[13px]">
              <thead className="border-b border-line bg-paper-3/60 text-[10px] uppercase tracking-[0.12em] text-muted">
                <tr>
                  <th className="px-5 py-3 font-medium">Order</th>
                  <th className="px-5 py-3 font-medium">Customer</th>
                  <th className="px-5 py-3 font-medium">Date</th>
                  <th className="px-5 py-3 font-medium">Items</th>
                  <th className="px-5 py-3 font-medium">Total</th>
                  <th className="px-5 py-3 font-medium">Payment</th>
                  <th className="px-5 py-3 font-medium">Fulfilment</th>
                  <th className="px-5 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {filtered.map((order) => {
                  const customer = userMap.get(order.userId);
                  const itemCount = order.items.reduce(
                    (total, item) => total + item.quantity,
                    0,
                  );
                  const statusBusy =
                    updateStatus.isPending &&
                    updateStatus.variables?.orderId === order.id;
                  const canCancel =
                    order.status !== "CANCELLED" &&
                    order.paymentStatus !== "PAID" &&
                    order.paymentStatus !== "REFUNDED";
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
                          user #{order.userId}
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
                      <td className="px-5 py-3.5">
                        <p className="text-[12px]">
                          {itemCount} {pluralize(itemCount, "item")}
                        </p>
                        <p className="max-w-40 truncate text-[10px] text-muted">
                          {order.items
                            .map((item) => item.productName)
                            .filter(Boolean)
                            .join(", ") || "—"}
                        </p>
                      </td>
                      <td className="px-5 py-3.5 tabular-nums">
                        {money(order.totalAmount)}
                      </td>
                      <td className="px-5 py-3.5">
                        <PaymentStatusBadge status={order.paymentStatus} />
                      </td>
                      <td className="px-5 py-3.5">
                        {order.status === "CANCELLED" ? (
                          <OrderStatusBadge status={order.status} />
                        ) : (
                          <Select
                            value={order.status}
                            disabled={statusBusy}
                            onChange={(event) =>
                              changeStatus(
                                order,
                                event.target.value as OrderStatus,
                              )
                            }
                            aria-label={`Fulfilment status for order ${order.id}`}
                            className="h-9 min-w-32 rounded-full text-[12px]"
                          >
                            {STATUS_OPTIONS.map((status) => (
                              <option key={status} value={status}>
                                {status.charAt(0) +
                                  status.slice(1).toLowerCase()}
                              </option>
                            ))}
                          </Select>
                        )}
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center justify-end gap-1">
                          <Link
                            to={`/orders/${order.id}`}
                            aria-label={`Open order ${order.id}`}
                            className="flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-ink/[0.06] hover:text-ink"
                          >
                            <ArrowUpRight className="h-3.5 w-3.5" />
                          </Link>
                          {canCancel ? (
                            <button
                              type="button"
                              onClick={() => setCancelTarget(order)}
                              aria-label={`Cancel order ${order.id}`}
                              className="flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-butter/30 hover:text-ink"
                            >
                              <X className="h-3.5 w-3.5" />
                            </button>
                          ) : null}
                          <button
                            type="button"
                            onClick={() => setDeleteTarget(order)}
                            aria-label={`Delete order ${order.id}`}
                            className="flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-danger/[0.08] hover:text-danger"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </TableShell>

      <p className="mt-4 text-[11px] leading-relaxed text-muted">
        Cancelling here restores stock. For paid orders, open the order and use
        refund instead — a plain cancellation would leave the payment marked as
        paid.
      </p>

      <ConfirmDialog
        open={Boolean(cancelTarget)}
        onClose={() => setCancelTarget(null)}
        onConfirm={confirmCancel}
        title={`Cancel FRM-${String(cancelTarget?.id ?? 0).padStart(5, "0")}?`}
        description="Stock is returned to the shelf and the order becomes cancelled. The customer receives a status email in a configured environment."
        confirmLabel="Cancel order"
        loading={cancelOrder.isPending}
        destructive={false}
      />
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
        title="Delete this order?"
        description="This is a hard delete in the backend. Payment records may be left behind and the action cannot be undone. Prefer cancelling."
        confirmLabel="Delete order"
        loading={deleteOrder.isPending}
      />
    </div>
  );
}
