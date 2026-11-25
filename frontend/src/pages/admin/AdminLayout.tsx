import { useEffect, useState } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { motion, useReducedMotion } from "motion/react";
import {
  ArrowUpRight,
  LayoutDashboard,
  LogOut,
  Menu,
  Package,
  ShoppingBag,
  Tags,
  Users,
  type LucideIcon,
} from "lucide-react";
import { useMe } from "@/hooks/queries";
import { initials } from "@/lib/format";
import { useAuth } from "@/stores/auth";
import { cn } from "@/components/ui";
import { Drawer } from "@/components/ui/Overlay";

const NAV: {
  section: string;
  items: { to: string; label: string; icon: LucideIcon; end?: boolean }[];
}[] = [
  {
    section: "Overview",
    items: [
      { to: "/admin", label: "Dashboard", icon: LayoutDashboard, end: true },
    ],
  },
  {
    section: "Catalogue",
    items: [
      { to: "/admin/products", label: "Products", icon: Package },
      { to: "/admin/categories", label: "Categories", icon: Tags },
    ],
  },
  {
    section: "Store",
    items: [
      { to: "/admin/orders", label: "Orders", icon: ShoppingBag },
      { to: "/admin/customers", label: "Customers", icon: Users },
    ],
  },
];

const SECTION_TITLES: Record<string, string> = {
  "/admin": "Dashboard",
  "/admin/products": "Products",
  "/admin/categories": "Categories",
  "/admin/orders": "Orders",
  "/admin/customers": "Customers",
};

function AdminNav({ onNavigate }: { onNavigate?: () => void }) {
  const email = useAuth((state) => state.email);
  const { data: me } = useMe();
  const logout = useAuth((state) => state.logout);
  const navigate = useNavigate();

  function signOut() {
    logout();
    onNavigate?.();
    navigate("/");
  }

  return (
    <div className="flex h-full flex-col bg-ink text-paper">
      <div className="px-6 pb-6 pt-7">
        <Link
          to="/admin"
          onClick={onNavigate}
          className="inline-flex items-baseline gap-2"
        >
          <span className="font-display text-2xl font-bold leading-none tracking-[-0.07em]">
            FORMA
            <span className="ml-0.5 inline-block h-1.5 w-1.5 rounded-full bg-butter align-baseline" />
          </span>
          <span className="text-[10px] uppercase tracking-[0.16em] text-paper/40">
            studio
          </span>
        </Link>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 pb-6">
        {NAV.map((group) => (
          <div key={group.section} className="mb-6">
            <p className="px-3 pb-2 text-[10px] uppercase tracking-[0.16em] text-paper/35">
              {group.section}
            </p>
            <ul className="space-y-0.5">
              {group.items.map((item) => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.end}
                    onClick={onNavigate}
                    className={({ isActive }) =>
                      cn(
                        "group flex items-center gap-3 rounded-sm px-3 py-2.5 text-[13px] transition-colors",
                        isActive
                          ? "bg-paper text-ink"
                          : "text-paper/65 hover:bg-paper/[0.07] hover:text-paper",
                      )
                    }
                  >
                    {({ isActive }) => (
                      <>
                        <item.icon
                          className={cn(
                            "h-4 w-4",
                            isActive ? "text-cobalt" : "text-paper/45",
                          )}
                        />
                        {item.label}
                      </>
                    )}
                  </NavLink>
                </li>
              ))}
            </ul>
          </div>
        ))}

        <div className="mt-2 border-t border-paper/10 pt-4">
          <Link
            to="/"
            onClick={onNavigate}
            className="flex items-center justify-between rounded-sm px-3 py-2.5 text-[13px] text-paper/60 transition-colors hover:bg-paper/[0.07] hover:text-paper"
          >
            View storefront
            <ArrowUpRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      </nav>

      <div className="border-t border-paper/10 p-4">
        <div className="flex items-center gap-3">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-paper/10 text-[11px] font-semibold text-paper">
            {initials(me?.fullName ?? email)}
          </span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-[12px] font-medium">
              {me?.fullName ?? "Admin"}
            </p>
            <p className="truncate text-[10px] text-paper/45">{email}</p>
          </div>
          <button
            type="button"
            onClick={signOut}
            aria-label="Sign out"
            className="flex h-8 w-8 items-center justify-center rounded-full text-paper/50 transition-colors hover:bg-paper/10 hover:text-paper"
          >
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

export function AdminLayout() {
  const location = useLocation();
  const reduce = useReducedMotion();
  const [menuOpen, setMenuOpen] = useState(false);
  const title = SECTION_TITLES[location.pathname] ?? "Studio";

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "instant" as ScrollBehavior });
  }, [location.pathname]);

  return (
    <div className="min-h-screen bg-paper">
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 lg:block">
        <AdminNav />
      </aside>

      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 border-b border-line bg-paper/90 backdrop-blur-xl">
          <div className="flex h-16 items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setMenuOpen(true)}
                aria-label="Open admin menu"
                className="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-ink/[0.06] lg:hidden"
              >
                <Menu className="h-5 w-5" />
              </button>
              <p className="text-[11px] uppercase tracking-[0.14em] text-muted">
                Admin studio
                <span className="mx-2 text-line">/</span>
                <span className="text-ink">{title}</span>
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Link
                to="/"
                className="hidden items-center gap-1.5 text-[12px] text-muted transition-colors hover:text-ink sm:inline-flex"
              >
                View storefront
                <ArrowUpRight className="h-3.5 w-3.5" />
              </Link>
              <span className="hidden h-4 w-px bg-line sm:block" />
              <span className="rounded-full bg-ink px-3 py-1 text-[10px] font-medium uppercase tracking-[0.1em] text-paper">
                Admin
              </span>
            </div>
          </div>
        </header>

        <main className="px-4 py-6 sm:px-6 md:py-8 lg:px-8">
          <motion.div
            key={location.pathname}
            initial={{ opacity: 0, y: reduce ? 0 : 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: reduce ? 0 : 0.35, ease: [0.22, 1, 0.36, 1] }}
          >
            <Outlet />
          </motion.div>
        </main>
      </div>

      <Drawer
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        title="Admin studio"
        side="left"
        className="max-w-[300px]"
      >
        <AdminNav onNavigate={() => setMenuOpen(false)} />
      </Drawer>
    </div>
  );
}
