import { Link } from "react-router-dom";
import { ArrowUp } from "lucide-react";
import { useCategories } from "@/hooks/queries";
import { useAuth } from "@/stores/auth";

export function Footer() {
  const { data: categories } = useCategories();
  const token = useAuth((state) => state.token);
  const role = useAuth((state) => state.role);

  return (
    <footer className="mt-24 bg-ink text-paper">
      <div className="container-x pb-8 pt-16">
        <div className="grid gap-12 border-b border-paper/15 pb-14 md:grid-cols-12">
          <div className="md:col-span-5">
            <p className="font-display text-3xl leading-[1.05] tracking-[-0.045em] md:text-4xl">
              Objects that earn
              <br />
              their place.
            </p>
            <p className="mt-4 max-w-xs text-[13px] leading-relaxed text-paper/60">
              A small, independent shop for audio, lighting and desk tools made
              by people who care how things age.
            </p>
            <a
              href="mailto:studio@forma.dev"
              className="mt-6 inline-block text-[13px] text-paper underline decoration-paper/40 underline-offset-4 transition-colors hover:decoration-paper"
            >
              studio@forma.dev
            </a>
          </div>

          <div className="md:col-span-3 md:col-start-7">
            <p className="text-[12px] text-paper/45">Shop</p>
            <ul className="mt-4 space-y-2.5 text-[13px]">
              <li>
                <Link to="/shop" className="text-paper/80 hover:text-paper">
                  All objects
                </Link>
              </li>
              {categories?.map((category) => (
                <li key={category.id}>
                  <Link
                    to={`/shop?category=${category.id}`}
                    className="text-paper/80 hover:text-paper"
                  >
                    {category.name}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="md:col-span-2">
            <p className="text-[12px] text-paper/45">Your account</p>
            <ul className="mt-4 space-y-2.5 text-[13px]">
              <li>
                <Link
                  to={token ? "/account" : "/login"}
                  className="text-paper/80 hover:text-paper"
                >
                  {token ? "Account" : "Sign in"}
                </Link>
              </li>
              <li>
                <Link to="/orders" className="text-paper/80 hover:text-paper">
                  Orders
                </Link>
              </li>
              <li>
                <Link
                  to="/wishlist"
                  className="text-paper/80 hover:text-paper"
                >
                  Wishlist
                </Link>
              </li>
              {role === "ADMIN" ? (
                <li>
                  <Link
                    to="/admin"
                    className="text-butter hover:text-paper"
                  >
                    Admin studio
                  </Link>
                </li>
              ) : null}
            </ul>
          </div>

          <div className="md:col-span-2">
            <p className="text-[12px] text-paper/45">Good to know</p>
            <ul className="mt-4 space-y-2.5 text-[13px] text-paper/80">
              <li>30-day returns</li>
              <li>Ships worldwide</li>
              <li>Repair-friendly</li>
            </ul>
          </div>
        </div>

        <div className="flex flex-col gap-6 pt-6 md:flex-row md:items-end md:justify-between">
          <p className="text-[11px] text-paper/40">
            © 2026 FORMA Studio. Demo storefront built with Spring Boot and
            React. Payments are simulated.
          </p>
          <button
            type="button"
            onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
            className="inline-flex items-center gap-2 self-start text-[11px] text-paper/50 transition-colors hover:text-paper md:self-auto"
          >
            Back to top
            <ArrowUp className="h-3.5 w-3.5" />
          </button>
        </div>

        <div
          aria-hidden
          className="mt-10 select-none overflow-hidden font-display text-[clamp(4rem,15vw,13rem)] font-bold leading-[0.72] tracking-[-0.075em] text-paper/[0.07]"
        >
          FORMA
        </div>
      </div>
    </footer>
  );
}
