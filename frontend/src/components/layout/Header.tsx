import { useEffect, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { Heart, Menu, Search, ShoppingBag, User } from "lucide-react";
import { useCart, useCategories } from "@/hooks/queries";
import { useAuth } from "@/stores/auth";
import { useUi } from "@/stores/ui";
import { useWishlist } from "@/stores/wishlist";
import { cn } from "@/components/ui";

export function Header() {
  const navigate = useNavigate();
  const token = useAuth((state) => state.token);
  const { data: categories } = useCategories();
  const { data: cart } = useCart();
  const wishlistCount = useWishlist((state) => state.items.length);
  const openCart = useUi((state) => state.openCart);
  const toggleSearch = useUi((state) => state.toggleSearch);
  const openMenu = useUi((state) => state.openMenu);
  const [scrolled, setScrolled] = useState(false);

  const bagCount =
    cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;

  useEffect(() => {
    function onScroll() {
      setScrolled(window.scrollY > 12);
    }
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const navCategories = categories?.slice(0, 5) ?? [];

  function handleBag() {
    if (token) openCart();
    else navigate("/login?next=%2Fcart");
  }

  return (
    <>
      <div className="bg-ink text-paper">
        <div className="container-x flex h-8 items-center justify-center gap-3 text-center text-[11px] tracking-[0.01em]">
          <span className="hidden sm:inline">
            Free returns within 30 days
          </span>
          <span className="hidden h-1 w-1 rounded-full bg-paper/35 sm:inline-block" />
          <span>A small shop for well-made objects</span>
        </div>
      </div>

      <header
        className={cn(
          "sticky top-0 z-50 border-b transition-colors duration-300",
          scrolled
            ? "border-line bg-paper/90 backdrop-blur-xl"
            : "border-transparent bg-paper",
        )}
      >
        <div className="container-x grid h-[68px] grid-cols-[1fr_auto_1fr] items-center gap-4 md:h-[76px]">
          <div className="flex items-center gap-5">
            <button
              type="button"
              onClick={openMenu}
              aria-label="Open menu"
              className="-ml-1 flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-ink/[0.06] lg:hidden"
            >
              <Menu className="h-5 w-5" />
            </button>
            <nav
              aria-label="Primary"
              className="hidden items-center gap-6 lg:flex"
            >
              <NavLink
                to="/shop"
                className={({ isActive }) =>
                  cn(
                    "link-underline text-[13px] font-medium text-ink-2 transition-colors hover:text-ink",
                    isActive && "text-ink",
                  )
                }
              >
                Shop all
              </NavLink>
              {navCategories.map((category) => (
                <NavLink
                  key={category.id}
                  to={`/shop?category=${category.id}`}
                  className="link-underline text-[13px] text-ink-2 transition-colors hover:text-ink"
                >
                  {category.name}
                </NavLink>
              ))}
            </nav>
          </div>

          <Link
            to="/"
            aria-label="FORMA home"
            className="justify-self-center font-display text-[26px] font-bold leading-none tracking-[-0.07em] text-ink md:text-[30px]"
          >
            FORMA
            <span className="ml-0.5 inline-block h-1.5 w-1.5 rounded-full bg-cobalt align-baseline" />
          </Link>

          <div className="flex items-center justify-end gap-1 sm:gap-2">
            <button
              type="button"
              onClick={toggleSearch}
              aria-label="Search"
              className="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-ink/[0.06]"
            >
              <Search className="h-[18px] w-[18px]" />
            </button>
            <Link
              to="/wishlist"
              aria-label={`Wishlist${wishlistCount ? ` (${wishlistCount})` : ""}`}
              className="relative hidden h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-ink/[0.06] sm:flex"
            >
              <Heart className="h-[18px] w-[18px]" />
              {wishlistCount > 0 ? (
                <span className="absolute right-1 top-1 h-1.5 w-1.5 rounded-full bg-cobalt" />
              ) : null}
            </Link>
            <Link
              to={token ? "/account" : "/login"}
              aria-label={token ? "Account" : "Sign in"}
              className="hidden h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-ink/[0.06] sm:flex"
            >
              <User className="h-[18px] w-[18px]" />
            </Link>
            <button
              type="button"
              onClick={handleBag}
              aria-label={`Bag${bagCount ? ` (${bagCount})` : ""}`}
              className="relative flex h-9 items-center gap-1.5 rounded-full pl-2 pr-2 transition-colors hover:bg-ink/[0.06] sm:pl-3"
            >
              <ShoppingBag className="h-[18px] w-[18px]" />
              <span className="hidden text-[12px] font-medium tabular-nums sm:inline">
                {bagCount}
              </span>
              {bagCount > 0 ? (
                <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-cobalt px-1 text-[9px] font-semibold text-white sm:hidden">
                  {bagCount}
                </span>
              ) : null}
            </button>
          </div>
        </div>
      </header>
    </>
  );
}
