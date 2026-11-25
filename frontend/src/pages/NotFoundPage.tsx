import { Link } from "react-router-dom";
import { motion, useReducedMotion } from "motion/react";
import { ArrowLeft, ArrowUpRight } from "lucide-react";

export function NotFoundPage() {
  const reduce = useReducedMotion();

  return (
    <div className="container-x py-16 md:py-24">
      <div className="grid items-center gap-10 border-y border-line py-12 md:grid-cols-12 md:py-16">
        <div className="md:col-span-7">
          <p className="text-[11px] uppercase tracking-[0.16em] text-muted">
            404 — off the shelf
          </p>
          <motion.h1
            initial={{ opacity: 0, y: reduce ? 0 : 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="mt-4 font-display text-[clamp(4rem,12vw,9rem)] font-semibold leading-[0.82] tracking-[-0.065em]"
          >
            Not
            <br />
            here<span className="text-cobalt">.</span>
          </motion.h1>
          <p className="mt-6 max-w-md text-sm leading-relaxed text-muted">
            The page you were looking for has moved, sold out, or never existed.
            The shop, however, is still very much where you left it.
          </p>
          <div className="mt-8 flex flex-wrap gap-2">
            <Link
              to="/shop"
              className="group inline-flex h-12 items-center gap-2 rounded-full bg-ink px-6 text-sm font-medium text-paper transition-colors hover:bg-cobalt"
            >
              Browse the shop
              <ArrowUpRight className="h-4 w-4 transition-transform group-hover:rotate-45" />
            </Link>
            <Link
              to="/"
              className="inline-flex h-12 items-center gap-2 rounded-full border border-ink/25 px-6 text-sm font-medium transition-colors hover:border-ink"
            >
              <ArrowLeft className="h-4 w-4" />
              Back home
            </Link>
          </div>
        </div>

        <div className="md:col-span-4 md:col-start-9">
          <div className="border border-line bg-paper-3/50 p-6">
            <p className="text-[11px] uppercase tracking-[0.14em] text-muted">
              Try instead
            </p>
            <ul className="mt-4 space-y-2 text-[13px]">
              {[
                { to: "/shop", label: "All objects" },
                { to: "/shop?sort=createdAt&order=DESC", label: "New in" },
                { to: "/orders", label: "Your orders" },
                { to: "/wishlist", label: "Wishlist" },
                { to: "/account", label: "Account" },
              ].map((link) => (
                <li key={link.to}>
                  <Link
                    to={link.to}
                    className="group flex items-center justify-between border-b border-line py-2 text-ink-2 transition-colors hover:text-ink"
                  >
                    {link.label}
                    <ArrowUpRight className="h-3.5 w-3.5 text-muted transition-transform group-hover:rotate-45 group-hover:text-ink" />
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
