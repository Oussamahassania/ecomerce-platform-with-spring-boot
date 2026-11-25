import { useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { motion, useReducedMotion } from "motion/react";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  CircleAlert,
  CreditCard,
  Mail,
  Package,
  RotateCcw,
  ShieldCheck,
  Truck,
  X,
} from "lucide-react";
import { toast } from "sonner";
import {
  useCancelOrder,
  useOrder,
  usePayOrder,
  useRefundOrder,
} from "@/hooks/queries";
import { formatDateTime, money, pluralize } from "@/lib/format";
import { productImage } from "@/lib/images";
import type { Order } from "@/lib/types";
import {
  Button,
  EmptyState,
  OrderStatusBadge,
  PaymentStatusBadge,
  Skeleton,
  cn,
} from "@/components/ui";
import { ConfirmDialog } from "@/components/ui/Overlay";

function newIdempotencyKey() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `forma-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function OrderTimeline({ order }: { order: Order }) {
  const reached =
    order.status === "DELIVERED"
      ? 3
      : order.status === "SHIPPED"
        ? 2
        : order.status === "CONFIRMED"
          ? 1
          : 0;

  const steps = [
    {
      label: "Order placed",
      copy: formatDateTime(order.orderDate),
      icon: Package,
    },
    {
      label: "Payment confirmed",
      copy:
        order.paymentStatus === "PAID"
          ? order.paymentReference ?? "Approved"
          : "Awaiting payment",
      icon: CreditCard,
    },
    {
      label: "Shipped",
      copy: order.status === "SHIPPED" || order.status === "DELIVERED" ? "On its way" : "Not yet",
      icon: Truck,
    },
    {
      label: "Delivered",
      copy: order.status === "DELIVERED" ? "Completed" : "Not yet",
      icon: Check,
    },
  ];

  return (
    <ol className="relative space-y-0">
      {steps.map((step, index) => {
        const done = index <= reached;
        const current = index === reached + 1 && order.status !== "DELIVERED";
        const Icon = step.icon;
        return (
          <li key={step.label} className="relative flex gap-4 pb-7 last:pb-0">
            {index < steps.length - 1 ? (
              <span
                aria-hidden
                className={cn(
                  "absolute left-[15px] top-8 h-[calc(100%-2rem)] w-px",
                  index < reached ? "bg-ink" : "bg-line",
                )}
              />
            ) : null}
            <span
              className={cn(
                "relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border transition-colors",
                done
                  ? "border-ink bg-ink text-paper"
                  : current
                    ? "border-cobalt bg-cobalt text-white"
                    : "border-line bg-paper text-muted",
              )}
            >
              <Icon className="h-3.5 w-3.5" />
            </span>
            <div className="pt-0.5">
              <p
                className={cn(
                  "text-[13px] font-medium",
                  done || current ? "text-ink" : "text-muted",
                )}
              >
                {step.label}
              </p>
              <p className="mt-0.5 text-[11px] text-muted">{step.copy}</p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}

export function OrderDetailPage() {
  const { id } = useParams();
  const orderId = Number(id);
  const [searchParams] = useSearchParams();
  const placed = searchParams.get("placed") === "1";
  const paymentFailed = searchParams.get("payment") === "failed";
  const reduce = useReducedMotion();

  const { data: order, isLoading, isError, error } = useOrder(
    Number.isFinite(orderId) ? orderId : undefined,
  );
  const pay = usePayOrder();
  const cancelOrder = useCancelOrder();
  const refundOrder = useRefundOrder();

  const [confirm, setConfirm] = useState<"cancel" | "refund" | null>(null);

  const count = useMemo(
    () => order?.items.reduce((total, item) => total + item.quantity, 0) ?? 0,
    [order],
  );

  const canPay = Boolean(
    order &&
      order.status !== "CANCELLED" &&
      order.paymentStatus !== "PAID" &&
      order.paymentStatus !== "REFUNDED",
  );
  const canRefund = Boolean(
    order && order.paymentStatus === "PAID" && order.status !== "CANCELLED",
  );
  const canCancel = Boolean(
    order &&
      order.status !== "CANCELLED" &&
      order.paymentStatus !== "PAID" &&
      order.paymentStatus !== "REFUNDED",
  );

  function handlePay() {
    if (!order) return;
    pay.mutate(
      {
        orderId: order.id,
        paymentMethod: "card",
        idempotencyKey: newIdempotencyKey(),
      },
      {
        onSuccess: (payment) => {
          if (payment.status === "PAID") {
            toast.success("Payment approved. Your order is confirmed.");
          } else {
            toast.error("The payment was declined. You can try again.", {
              description: "The simulated gateway declines roughly one in ten.",
            });
          }
        },
        onError: (mutationError) => toast.error(mutationError.message),
      },
    );
  }

  function handleCancel() {
    if (!order) return;
    cancelOrder.mutate(order.id, {
      onSuccess: () => {
        setConfirm(null);
        toast.success("Order cancelled and stock returned");
      },
      onError: (mutationError) => toast.error(mutationError.message),
    });
  }

  function handleRefund() {
    if (!order) return;
    refundOrder.mutate(order.id, {
      onSuccess: () => {
        setConfirm(null);
        toast.success("Refund processed in the demo gateway");
      },
      onError: (mutationError) => toast.error(mutationError.message),
    });
  }

  if (isError) {
    const status = (error as { status?: number })?.status;
    return (
      <div className="container-x py-16">
        <EmptyState
          title={status === 403 ? "This order is not yours." : "Order not found."}
          description={
            status === 403
              ? "It belongs to another account. If you think that is wrong, get in touch and we will sort it out."
              : "The order may have been removed, or the link may be wrong."
          }
          action={
            <Link
              to="/orders"
              className="inline-flex h-11 items-center rounded-full bg-ink px-6 text-sm font-medium text-paper"
            >
              Back to your orders
            </Link>
          }
        />
      </div>
    );
  }

  if (isLoading || !order) {
    return (
      <div className="container-x py-12">
        <Skeleton className="h-3 w-32" />
        <Skeleton className="mt-4 h-14 w-72" />
        <div className="mt-10 grid gap-10 lg:grid-cols-12">
          <div className="space-y-4 lg:col-span-8">
            <Skeleton className="h-48 w-full" />
            <Skeleton className="h-64 w-full" />
          </div>
          <div className="lg:col-span-4">
            <Skeleton className="h-80 w-full" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container-x py-10 md:py-14">
      <Link
        to="/orders"
        className="inline-flex items-center gap-1.5 text-[12px] text-muted transition-colors hover:text-ink"
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        All orders
      </Link>

      {placed ? (
        <motion.div
          initial={{ opacity: 0, y: reduce ? 0 : -8 }}
          animate={{ opacity: 1, y: 0 }}
          className="mt-5 flex flex-col gap-3 rounded-sm bg-cobalt p-5 text-white sm:flex-row sm:items-center sm:justify-between"
        >
          <div className="flex items-start gap-3">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-white/15">
              <Check className="h-4 w-4" />
            </span>
            <div>
              <p className="text-sm font-medium">Order placed. Thank you.</p>
              <p className="mt-0.5 text-[12px] text-white/75">
                A confirmation would normally land in your inbox. This demo
                logs it on the server instead.
              </p>
            </div>
          </div>
          <Link
            to="/shop"
            className="inline-flex h-9 shrink-0 items-center rounded-full bg-white px-4 text-[12px] font-medium text-cobalt"
          >
            Keep looking
          </Link>
        </motion.div>
      ) : paymentFailed ? (
        <div className="mt-5 flex items-start gap-3 rounded-sm border border-butter bg-butter/20 p-5">
          <CircleAlert className="mt-0.5 h-5 w-5 shrink-0 text-ink" />
          <div>
            <p className="text-sm font-medium">The order is saved.</p>
            <p className="mt-0.5 text-[12px] text-ink-2">
              Payment was declined by the simulated gateway. Use “Pay now” below
              to try again — each attempt gets a fresh idempotency key.
            </p>
          </div>
        </div>
      ) : null}

      <div className="mt-6 flex flex-wrap items-end justify-between gap-4 border-b border-line pb-6">
        <div>
          <p className="text-[11px] uppercase tracking-[0.14em] text-muted">
            Order FRM-{String(order.id).padStart(5, "0")}
          </p>
          <h1 className="mt-2 font-display text-[clamp(2.4rem,5vw,4.2rem)] font-semibold leading-[0.94] tracking-[-0.05em]">
            {order.status === "CANCELLED" ? "A closed order." : "On its way, in time."}
          </h1>
          <p className="mt-3 text-sm text-muted">
            Placed {formatDateTime(order.orderDate)} · {count}{" "}
            {pluralize(count, "object")}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-1.5">
          <OrderStatusBadge status={order.status} />
          <PaymentStatusBadge status={order.paymentStatus} />
        </div>
      </div>

      <div className="mt-8 grid gap-10 lg:grid-cols-12 lg:gap-14">
        <div className="lg:col-span-8">
          {order.status === "CANCELLED" ? (
            <div className="rounded-sm border border-line bg-paper-3/60 p-6">
              <div className="flex items-start gap-3">
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-ink/[0.06]">
                  {order.paymentStatus === "REFUNDED" ? (
                    <RotateCcw className="h-4 w-4" />
                  ) : (
                    <X className="h-4 w-4" />
                  )}
                </span>
                <div>
                  <p className="font-display text-xl tracking-[-0.03em]">
                    {order.paymentStatus === "REFUNDED"
                      ? "Refunded and closed."
                      : "Cancelled."}
                  </p>
                  <p className="mt-1 max-w-lg text-[13px] leading-relaxed text-muted">
                    {order.paymentStatus === "REFUNDED"
                      ? "The payment was returned and the objects went back into stock. Nothing else is needed from you."
                      : "The order was cancelled and the objects went back into stock. If a payment was pending, nothing was charged."}
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-sm border border-line p-6">
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Progress
              </h2>
              <div className="mt-6">
                <OrderTimeline order={order} />
              </div>
              {order.paymentStatus === "FAILED" ? (
                <p className="mt-5 flex items-start gap-2 border-t border-line pt-4 text-[12px] leading-relaxed text-danger">
                  <CircleAlert className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                  The last payment attempt was declined. The order is held and
                  stock is reserved until you pay or cancel.
                </p>
              ) : null}
            </div>
          )}

          <div className="mt-8">
            <div className="flex items-baseline justify-between border-b border-line pb-3">
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Objects
              </h2>
              <span className="text-[11px] tabular-nums text-muted">
                {count} {pluralize(count, "item")}
              </span>
            </div>
            <ul className="divide-y divide-line">
              {order.items.map((item, index) => (
                <li key={`${item.productId}-${index}`} className="flex gap-4 py-5">
                  <Link
                    to={`/product/${item.productId}`}
                    className="h-24 w-20 shrink-0 overflow-hidden bg-paper-2"
                  >
                    <img
                      src={productImage(item.productImage)}
                      alt={item.productName ?? "Ordered object"}
                      className="h-full w-full object-cover"
                    />
                  </Link>
                  <div className="flex min-w-0 flex-1 items-start justify-between gap-4">
                    <div className="min-w-0">
                      <Link
                        to={`/product/${item.productId}`}
                        className="font-display text-xl tracking-[-0.03em] hover:text-cobalt"
                      >
                        {item.productName ?? `Product #${item.productId}`}
                      </Link>
                      <p className="mt-1 text-[12px] text-muted">
                        {item.quantity} × {money(item.price)}
                      </p>
                    </div>
                    <p className="shrink-0 text-sm font-medium tabular-nums">
                      {money(Number(item.price) * item.quantity)}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          </div>

          <div className="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-line pt-5 text-[12px] text-muted">
            <span className="inline-flex items-center gap-1.5">
              <Truck className="h-3.5 w-3.5" /> Free worldwide shipping
            </span>
            <span className="inline-flex items-center gap-1.5">
              <ShieldCheck className="h-3.5 w-3.5" /> 30-day returns
            </span>
          </div>
        </div>

        <aside className="lg:col-span-4">
          <div className="sticky top-24 space-y-4">
            <div className="border border-ink/15 bg-paper-3/60 p-6">
              <h2 className="font-display text-2xl tracking-[-0.035em]">
                Summary
              </h2>
              <dl className="mt-5 space-y-2.5 text-[13px]">
                <div className="flex justify-between">
                  <dt className="text-muted">Subtotal</dt>
                  <dd className="tabular-nums">{money(order.totalAmount)}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-muted">Shipping</dt>
                  <dd className="text-success">Free</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-muted">Tax</dt>
                  <dd className="text-muted">Included</dd>
                </div>
              </dl>
              <div className="mt-4 flex items-baseline justify-between border-t border-line pt-4">
                <span className="text-sm font-medium">Total</span>
                <span className="font-display text-3xl tracking-[-0.04em] tabular-nums">
                  {money(order.totalAmount)}
                </span>
              </div>

              <div className="mt-5 border-t border-line pt-4">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-[12px] text-muted">Payment</span>
                  <PaymentStatusBadge status={order.paymentStatus} />
                </div>
                {order.paymentReference ? (
                  <p className="mt-2 break-all text-[11px] tabular-nums text-muted">
                    Ref. {order.paymentReference}
                  </p>
                ) : null}
              </div>

              {canPay ? (
                <Button
                  onClick={handlePay}
                  disabled={pay.isPending}
                  className="mt-5 w-full justify-between bg-cobalt hover:bg-cobalt-ink"
                  size="lg"
                >
                  <span>
                    {pay.isPending
                      ? "Talking to the gateway…"
                      : `Pay ${money(order.totalAmount)}`}
                  </span>
                  <ArrowRight className="h-4 w-4" />
                </Button>
              ) : null}

              {canPay ? (
                <p className="mt-3 text-[11px] leading-relaxed text-muted">
                  Simulated card payment. Roughly one in ten attempts is
                  declined so you can test the retry flow.
                </p>
              ) : null}
            </div>

            {canCancel || canRefund ? (
              <div className="border border-line p-5">
                <p className="text-[12px] font-medium">Need to undo this?</p>
                {canRefund ? (
                  <>
                    <p className="mt-1 text-[11px] leading-relaxed text-muted">
                      Refunding returns the payment, cancels the order and puts
                      the objects back into stock.
                    </p>
                    <Button
                      variant="dangerGhost"
                      className="mt-3 w-full justify-center"
                      onClick={() => setConfirm("refund")}
                    >
                      <RotateCcw className="h-4 w-4" />
                      Refund order
                    </Button>
                  </>
                ) : null}
                {canCancel ? (
                  <>
                    <p className="mt-1 text-[11px] leading-relaxed text-muted">
                      Cancelling releases the reserved stock. Nothing has been
                      charged.
                    </p>
                    <Button
                      variant="ghost"
                      className="mt-3 w-full justify-center text-danger hover:bg-danger/[0.06]"
                      onClick={() => setConfirm("cancel")}
                    >
                      <X className="h-4 w-4" />
                      Cancel order
                    </Button>
                  </>
                ) : null}
              </div>
            ) : null}

            <div className="border border-line p-5">
              <p className="text-[12px] font-medium">Something wrong?</p>
              <p className="mt-1 text-[11px] leading-relaxed text-muted">
                Write to the studio with your order number and we will look at
                it together.
              </p>
              <a
                href={`mailto:studio@forma.dev?subject=Order FRM-${String(order.id).padStart(5, "0")}`}
                className="mt-3 inline-flex items-center gap-1.5 text-[12px] font-medium text-ink underline decoration-ink/30 underline-offset-4 hover:decoration-ink"
              >
                <Mail className="h-3.5 w-3.5" />
                studio@forma.dev
              </a>
            </div>
          </div>
        </aside>
      </div>

      <ConfirmDialog
        open={confirm === "cancel"}
        onClose={() => setConfirm(null)}
        onConfirm={handleCancel}
        title="Cancel this order?"
        description="The reserved objects go back into stock. You can order them again while they last."
        confirmLabel="Cancel order"
        loading={cancelOrder.isPending}
      />
      <ConfirmDialog
        open={confirm === "refund"}
        onClose={() => setConfirm(null)}
        onConfirm={handleRefund}
        title="Refund this order?"
        description="The full amount is returned in the demo gateway, the order is cancelled and stock is restored. This cannot be undone."
        confirmLabel="Refund order"
        loading={refundOrder.isPending}
      />
    </div>
  );
}
