import { Link, useNavigate } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { useCategories, useMe } from "@/hooks/queries";
import { useAuth } from "@/stores/auth";
import { useUi } from "@/stores/ui";
import { useWishlist } from "@/stores/wishlist";
import { Drawer } from "@/components/ui/Overlay";
import { cn } from "@/components/ui";

export function MobileMenu() {
  const open = useUi((state) => state.menuOpen);
  const close = useUi((state) => state.closeMenu);
  const token = useAuth((state) => state.token);
  const role = useAuth((state) => state.role);
  const logout = useAuth((state) => state.logout);
  const navigate = useNavigate();
  const { data: categories } = useCategories();
  const { data: me } = useMe();
  const wishlistCount = useWishlist((state) => state.items.length);

  function signOut() {
    logout();
    close();
    navigate("/");
  }

  const linkClass =
    "flex items-center justify-between border-b border-line py-3.5 font-display text-2xl tracking-[-0.04em] text-ink transition-colors hover:text-cobalt";

  return (
    <Drawer open={open} onClose={close} title="Menu" side="left">
      <div className="px-6 py-6">
        <nav aria-label="Mobile navigation">
          <Link to="/shop" onClick={close} className={linkClass}>
            Shop all
            <ArrowUpRight className="h-5 w-5" />
          </Link>
          {categories?.map((category) => (
            <Link
              key={category.id}
              to={`/shop?category=${category.id}`}
              onClick={close}
              className={linkClass}
            >
              {category.name}
              <ArrowUpRight className="h-5 w-5 text-muted" />
            </Link>
          ))}
        </nav>

        <div className="mt-8 space-y-1">
          <Link
            to="/wishlist"
            onClick={close}
            className="flex items-center justify-between rounded-sm px-2 py-3 text-sm hover:bg-ink/[0.04]"
          >
            <span>Wishlist</span>
            <span className="text-[12px] tabular-nums text-muted">
              {wishlistCount}
            </span>
          </Link>
          {token ? (
            <>
              <Link
                to="/orders"
                onClick={close}
                className="flex items-center justify-between rounded-sm px-2 py-3 text-sm hover:bg-ink/[0.04]"
              >
                <span>Orders</span>
              </Link>
              <Link
                to="/account"
                onClick={close}
                className="flex items-center justify-between rounded-sm px-2 py-3 text-sm hover:bg-ink/[0.04]"
              >
                <span>Account</span>
                <span className="max-w-[60%] truncate text-[12px] text-muted">
                  {me?.email ?? ""}
                </span>
              </Link>
              {role === "ADMIN" ? (
                <Link
                  to="/admin"
                  onClick={close}
                  className="flex items-center justify-between rounded-sm px-2 py-3 text-sm text-cobalt hover:bg-cobalt/[0.06]"
                >
                  <span>Admin studio</span>
                </Link>
              ) : null}
              <button
                type="button"
                onClick={signOut}
                className={cn(
                  "flex w-full items-center justify-between rounded-sm px-2 py-3 text-left text-sm text-danger hover:bg-danger/[0.06]",
                )}
              >
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                onClick={close}
                className="mt-2 flex h-12 items-center justify-center rounded-full bg-ink text-sm font-medium text-paper"
              >
                Sign in
              </Link>
              <Link
                to="/register"
                onClick={close}
                className="mt-2 flex h-12 items-center justify-center rounded-full border border-ink/20 text-sm font-medium"
              >
                Create an account
              </Link>
            </>
          )}
        </div>

        <p className="mt-10 text-[12px] leading-relaxed text-muted">
          Free returns within 30 days.
          <br />
          Questions?{" "}
          <a
            href="mailto:studio@forma.dev"
            className="text-ink underline underline-offset-4"
          >
            studio@forma.dev
          </a>
        </p>
      </div>
    </Drawer>
  );
}
