import { useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { MessageSquare, Star, Trash2 } from "lucide-react";
import { toast } from "sonner";
import {
  useAddReview,
  useDeleteReview,
  useMe,
  useMyOrders,
  useProductRating,
  useReviews,
} from "@/hooks/queries";
import type { Review } from "@/lib/types";
import { formatDate, initials } from "@/lib/format";
import { useAuth } from "@/stores/auth";
import {
  Button,
  Field,
  Skeleton,
  Stars,
  Textarea,
  cn,
} from "@/components/ui";
import { ConfirmDialog } from "@/components/ui/Overlay";

function StarPicker({
  value,
  onChange,
}: {
  value: number;
  onChange: (value: number) => void;
}) {
  const [hover, setHover] = useState(0);
  const active = hover || value;

  return (
    <div
      className="flex items-center gap-1"
      role="radiogroup"
      aria-label="Your rating"
      onMouseLeave={() => setHover(0)}
    >
      {Array.from({ length: 5 }).map((_, index) => {
        const score = index + 1;
        return (
          <button
            key={score}
            type="button"
            role="radio"
            aria-checked={value === score}
            aria-label={`${score} star${score === 1 ? "" : "s"}`}
            onMouseEnter={() => setHover(score)}
            onFocus={() => setHover(score)}
            onBlur={() => setHover(0)}
            onClick={() => onChange(score)}
            className="rounded-full p-0.5 transition-transform hover:scale-110"
          >
            <Star
              className={cn(
                "h-7 w-7 transition-colors",
                score <= active
                  ? "fill-butter text-butter"
                  : "fill-transparent text-ink/25",
              )}
              strokeWidth={1.5}
            />
          </button>
        );
      })}
      <span className="ml-2 text-[12px] tabular-nums text-muted">
        {value ? `${value} / 5` : "Select a rating"}
      </span>
    </div>
  );
}

function ReviewCard({
  review,
  isMine,
  onDelete,
}: {
  review: Review;
  isMine: boolean;
  onDelete: () => void;
}) {
  return (
    <motion.li
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
      className="py-6"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-paper-2 text-[12px] font-semibold text-ink-2">
            {initials(review.userFullName)}
          </span>
          <div>
            <p className="text-[13px] font-medium">
              {review.userFullName}
              {isMine ? (
                <span className="ml-2 text-[11px] font-normal text-muted">
                  you
                </span>
              ) : null}
            </p>
            <div className="mt-0.5 flex items-center gap-2">
              <Stars rating={review.rating} size={12} />
              <span className="text-[11px] text-muted">
                {formatDate(review.createdAt)}
              </span>
            </div>
          </div>
        </div>
        {isMine ? (
          <button
            type="button"
            onClick={onDelete}
            aria-label="Delete your review"
            className="flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-danger/[0.08] hover:text-danger"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        ) : null}
      </div>
      {review.comment ? (
        <p className="mt-3 max-w-2xl text-sm leading-relaxed text-ink-2">
          {review.comment}
        </p>
      ) : null}
    </motion.li>
  );
}

export function ProductReviews({
  productId,
  productName,
}: {
  productId: number;
  productName: string;
}) {
  const token = useAuth((state) => state.token);
  const location = useLocation();
  const reduce = useReducedMotion();
  const { data: reviews, isLoading } = useReviews(productId);
  const { data: rating } = useProductRating(productId);
  const { data: me } = useMe();
  const { data: orders, isLoading: ordersLoading } = useMyOrders();
  const addReview = useAddReview(productId);
  const deleteReview = useDeleteReview(productId);

  const [score, setScore] = useState(5);
  const [comment, setComment] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Review | null>(null);

  const myReview = reviews?.find((review) => review.userId === me?.id);
  const hasPurchased = useMemo(
    () =>
      Boolean(
        orders?.some(
          (order) =>
            order.status !== "CANCELLED" &&
            order.items.some((item) => item.productId === productId),
        ),
      ),
    [orders, productId],
  );

  const distribution = useMemo(() => {
    const buckets = [5, 4, 3, 2, 1].map((value) => ({
      value,
      count: reviews?.filter((review) => review.rating === value).length ?? 0,
    }));
    const total = reviews?.length ?? 0;
    return buckets.map((bucket) => ({
      ...bucket,
      percent: total ? Math.round((bucket.count / total) * 100) : 0,
    }));
  }, [reviews]);

  const average = rating?.averageRating ?? 0;
  const total = rating?.totalReviews ?? reviews?.length ?? 0;

  function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!score) {
      toast.error("Choose a rating first");
      return;
    }
    addReview.mutate(
      { rating: score, comment: comment.trim() },
      {
        onSuccess: () => {
          setComment("");
          setScore(5);
          setShowForm(false);
          toast.success("Thank you — your review is live");
        },
        onError: (error) => toast.error(error.message),
      },
    );
  }

  function confirmDelete() {
    if (!deleteTarget) return;
    deleteReview.mutate(deleteTarget.id, {
      onSuccess: () => {
        setDeleteTarget(null);
        toast.success("Review removed");
      },
      onError: (error) => toast.error(error.message),
    });
  }

  return (
    <section id="reviews" className="scroll-mt-32">
      <div className="grid gap-10 border-t border-line pt-10 lg:grid-cols-12 lg:gap-14">
        <div className="lg:col-span-4">
          <h2 className="font-display text-[clamp(2rem,3.6vw,3rem)] leading-none tracking-[-0.045em]">
            Reviews
          </h2>
          <div className="mt-6 flex items-end gap-4">
            <span className="font-display text-6xl leading-none tracking-[-0.06em] tabular-nums">
              {average ? average.toFixed(1) : "—"}
            </span>
            <div className="pb-1">
              <Stars rating={average} size={16} />
              <p className="mt-1 text-[12px] text-muted">
                {total === 0
                  ? "No reviews yet"
                  : `${total} ${total === 1 ? "review" : "reviews"}`}
              </p>
            </div>
          </div>

          <div className="mt-6 space-y-2">
            {distribution.map((bucket) => (
              <div key={bucket.value} className="flex items-center gap-3">
                <span className="w-3 text-right text-[11px] tabular-nums text-muted">
                  {bucket.value}
                </span>
                <Star className="h-3 w-3 fill-ink/20 text-ink/20" />
                <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-ink/[0.08]">
                  <motion.div
                    initial={{ width: 0 }}
                    whileInView={{ width: `${bucket.percent}%` }}
                    viewport={{ once: true }}
                    transition={{ duration: reduce ? 0 : 0.7, ease: "easeOut" }}
                    className="h-full rounded-full bg-ink"
                  />
                </div>
                <span className="w-5 text-right text-[11px] tabular-nums text-muted">
                  {bucket.count}
                </span>
              </div>
            ))}
          </div>

          <div className="mt-7 border-t border-line pt-5">
            {!token ? (
              <>
                <p className="text-sm leading-relaxed text-muted">
                  Bought this piece? Sign in to leave a review.
                </p>
                <Link
                  to={`/login?next=${encodeURIComponent(
                    location.pathname + "#reviews",
                  )}`}
                  className="mt-4 inline-flex h-10 items-center rounded-full bg-ink px-5 text-[13px] font-medium text-paper transition-colors hover:bg-ink-2"
                >
                  Sign in to review
                </Link>
              </>
            ) : myReview ? (
              <p className="text-sm leading-relaxed text-muted">
                You have already reviewed {productName}. Thank you for the
                notes.
              </p>
            ) : ordersLoading ? (
              <Skeleton className="h-10 w-40" />
            ) : hasPurchased ? (
              showForm ? null : (
                <>
                  <p className="text-sm leading-relaxed text-muted">
                    This piece is in your order history. Tell other people how
                    it has settled in.
                  </p>
                  <Button
                    variant="outline"
                    className="mt-4"
                    onClick={() => setShowForm(true)}
                  >
                    <MessageSquare className="h-4 w-4" />
                    Write a review
                  </Button>
                </>
              )
            ) : (
              <p className="text-sm leading-relaxed text-muted">
                Reviews are reserved for customers who have ordered this piece.
                We would love your notes once it arrives.
              </p>
            )}
          </div>
        </div>

        <div className="lg:col-span-8">
          <AnimatePresence initial={false}>
            {showForm && !myReview ? (
              <motion.form
                key="form"
                initial={{ opacity: 0, height: reduce ? "auto" : 0 }}
                animate={{ opacity: 1, height: "auto" }}
                exit={{ opacity: 0, height: reduce ? "auto" : 0 }}
                transition={{ duration: reduce ? 0 : 0.35, ease: [0.22, 1, 0.36, 1] }}
                onSubmit={submit}
                className="overflow-hidden"
              >
                <div className="mb-8 space-y-5 rounded-sm border border-line bg-paper-3/60 p-5 md:p-6">
                  <div>
                    <p className="text-[13px] font-medium text-ink-2">
                      Your rating
                    </p>
                    <div className="mt-2">
                      <StarPicker value={score} onChange={setScore} />
                    </div>
                  </div>
                  <Field
                    label="Your notes"
                    htmlFor="review-comment"
                    hint={`${comment.length}/500 characters — optional`}
                  >
                    <Textarea
                      id="review-comment"
                      value={comment}
                      maxLength={500}
                      onChange={(event) => setComment(event.target.value)}
                      placeholder="How does it feel, sound, or sit in the room? What surprised you?"
                      className="min-h-28 bg-paper"
                    />
                  </Field>
                  <div className="flex flex-wrap items-center gap-2">
                    <Button type="submit" disabled={addReview.isPending}>
                      {addReview.isPending ? "Publishing…" : "Publish review"}
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={() => setShowForm(false)}
                    >
                      Cancel
                    </Button>
                  </div>
                </div>
              </motion.form>
            ) : null}
          </AnimatePresence>

          {isLoading ? (
            <div className="divide-y divide-line">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={index} className="space-y-3 py-6">
                  <div className="flex items-center gap-3">
                    <Skeleton className="h-10 w-10 rounded-full" />
                    <div className="space-y-2">
                      <Skeleton className="h-3 w-32" />
                      <Skeleton className="h-3 w-20" />
                    </div>
                  </div>
                  <Skeleton className="h-3 w-full" />
                  <Skeleton className="h-3 w-2/3" />
                </div>
              ))}
            </div>
          ) : reviews && reviews.length > 0 ? (
            <ul className="divide-y divide-line">
              {reviews.map((review) => (
                <ReviewCard
                  key={review.id}
                  review={review}
                  isMine={review.userId === me?.id}
                  onDelete={() => setDeleteTarget(review)}
                />
              ))}
            </ul>
          ) : (
            <div className="flex h-full min-h-56 flex-col items-center justify-center rounded-sm border border-dashed border-line px-6 text-center">
              <Stars rating={0} size={18} />
              <p className="mt-3 font-display text-2xl tracking-[-0.03em]">
                No notes yet.
              </p>
              <p className="mt-1 max-w-xs text-[13px] leading-relaxed text-muted">
                Be the first to say how {productName} lives with you.
              </p>
            </div>
          )}
        </div>
      </div>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
        title="Remove your review?"
        description="This cannot be undone. You would need to write it again."
        confirmLabel="Remove review"
        loading={deleteReview.isPending}
      />
    </section>
  );
}
