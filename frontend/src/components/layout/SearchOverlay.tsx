import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { ArrowRight, Search, X } from "lucide-react";
import { useCategories, useShopProducts } from "@/hooks/queries";
import { useUi } from "@/stores/ui";
import { money } from "@/lib/format";
import { productImage } from "@/lib/images";
import { Skeleton } from "@/components/ui";

export function SearchOverlay() {
  const open = useUi((state) => state.searchOpen);
  const close = useUi((state) => state.closeSearch);
  const [term, setTerm] = useState("");
  const [debounced, setDebounced] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const reduce = useReducedMotion();
  const { data: categories } = useCategories();

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(term.trim()), 280);
    return () => window.clearTimeout(timer);
  }, [term]);

  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const timer = window.setTimeout(() => inputRef.current?.focus(), 120);
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") close();
    }
    document.addEventListener("keydown", onKey);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.clearTimeout(timer);
      document.removeEventListener("keydown", onKey);
    };
  }, [open, close]);

  useEffect(() => {
    if (!open) {
      setTerm("");
      setDebounced("");
    }
  }, [open]);

  const { data, isFetching } = useShopProducts({
    q: debounced,
    size: 6,
    sort: "createdAt",
    order: "DESC",
  });

  const showResults = debounced.length >= 2;

  function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!term.trim()) return;
    close();
    navigate(`/shop?q=${encodeURIComponent(term.trim())}`);
  }

  return (
    <AnimatePresence>
      {open ? (
        <div className="fixed inset-0 z-[75]">
          <motion.button
            type="button"
            aria-label="Close search"
            className="absolute inset-0 h-full w-full cursor-default bg-ink/40 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: reduce ? 0 : 0.22 }}
            onClick={close}
            tabIndex={-1}
          />
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label="Search the shop"
            initial={{ y: reduce ? 0 : -24, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: reduce ? 0 : -16, opacity: 0 }}
            transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
            className="relative mx-auto mt-0 max-h-[100dvh] w-full overflow-y-auto bg-paper pb-10 shadow-[0_30px_80px_rgba(20,21,15,0.18)] md:mt-4 md:max-w-5xl md:rounded-sm"
          >
            <div className="container-x py-5 md:px-8">
              <div className="flex items-center justify-between">
                <p className="text-[12px] text-muted">Search the shop</p>
                <button
                  type="button"
                  onClick={close}
                  aria-label="Close search"
                  className="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-ink/[0.06]"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <form onSubmit={submit} className="mt-4 flex items-center gap-3 border-b border-ink pb-4">
                <Search className="h-5 w-5 shrink-0 text-muted" />
                <input
                  ref={inputRef}
                  value={term}
                  onChange={(event) => setTerm(event.target.value)}
                  placeholder="Speaker, lamp, tote…"
                  aria-label="Search products"
                  className="w-full bg-transparent font-display text-[clamp(1.6rem,4vw,2.6rem)] tracking-[-0.04em] text-ink placeholder:text-ink/25 focus:outline-none"
                />
                {term ? (
                  <button
                    type="button"
                    onClick={() => setTerm("")}
                    className="text-[12px] text-muted hover:text-ink"
                  >
                    Clear
                  </button>
                ) : null}
              </form>

              {!showResults ? (
                <div className="mt-8">
                  <p className="text-[12px] text-muted">Browse by category</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {categories?.map((category) => (
                      <Link
                        key={category.id}
                        to={`/shop?category=${category.id}`}
                        onClick={close}
                        className="rounded-full border border-ink/15 px-4 py-2 text-[13px] transition-colors hover:border-ink hover:bg-ink hover:text-paper"
                      >
                        {category.name}
                      </Link>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="mt-8">
                  {isFetching && !data ? (
                    <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
                      {Array.from({ length: 3 }).map((_, index) => (
                        <div key={index} className="space-y-2">
                          <Skeleton className="aspect-square w-full" />
                          <Skeleton className="h-3 w-2/3" />
                        </div>
                      ))}
                    </div>
                  ) : data && data.content.length > 0 ? (
                    <>
                      <div className="grid grid-cols-2 gap-x-4 gap-y-6 md:grid-cols-3">
                        {data.content.map((product) => (
                          <Link
                            key={product.id}
                            to={`/product/${product.id}`}
                            onClick={close}
                            className="group"
                          >
                            <div className="aspect-square overflow-hidden bg-paper-2">
                              <img
                                src={productImage(product.img_url)}
                                alt={product.name}
                                className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.04]"
                              />
                            </div>
                            <div className="mt-2 flex items-baseline justify-between gap-3">
                              <span className="truncate text-[13px] font-medium">
                                {product.name}
                              </span>
                              <span className="shrink-0 text-[12px] tabular-nums text-muted">
                                {money(product.price)}
                              </span>
                            </div>
                          </Link>
                        ))}
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          close();
                          navigate(`/shop?q=${encodeURIComponent(debounced)}`);
                        }}
                        className="mt-8 inline-flex items-center gap-2 text-[13px] font-medium text-cobalt hover:underline"
                      >
                        View all {data.totalElements}{" "}
                        {data.totalElements === 1 ? "result" : "results"}
                        <ArrowRight className="h-4 w-4" />
                      </button>
                    </>
                  ) : (
                    <p className="py-8 text-sm text-muted">
                      No objects match “{debounced}”. Try a broader word, like
                      “light” or “audio”.
                    </p>
                  )}
                </div>
              )}
            </div>
          </motion.div>
        </div>
      ) : null}
    </AnimatePresence>
  );
}
