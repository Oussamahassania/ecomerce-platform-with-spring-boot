import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Search, ShieldCheck, Trash2, UserRound } from "lucide-react";
import { toast } from "sonner";
import {
  useAdminOrders,
  useAdminUsers,
  useChangeRole,
  useDeleteUser,
  useMe,
} from "@/hooks/queries";
import { formatDate, initials, money } from "@/lib/format";
import type { User } from "@/lib/types";
import {
  Badge,
  Input,
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

type RoleFilter = "ALL" | "ADMIN" | "USER";

export function AdminCustomersPage() {
  const { data: users, isLoading } = useAdminUsers();
  const { data: orders } = useAdminOrders();
  const { data: me } = useMe();
  const changeRole = useChangeRole();
  const deleteUser = useDeleteUser();

  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("ALL");
  const [deleteTarget, setDeleteTarget] = useState<User | null>(null);

  const orderStats = useMemo(() => {
    const map = new Map<number, { count: number; paid: number }>();
    for (const order of orders ?? []) {
      const current = map.get(order.userId) ?? { count: 0, paid: 0 };
      current.count += 1;
      if (order.paymentStatus === "PAID") {
        current.paid += Number(order.totalAmount);
      }
      map.set(order.userId, current);
    }
    return map;
  }, [orders]);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return (users ?? [])
      .filter((user) => {
        if (roleFilter !== "ALL" && user.roleName !== roleFilter) return false;
        if (!term) return true;
        return (
          user.fullName.toLowerCase().includes(term) ||
          user.email.toLowerCase().includes(term)
        );
      })
      .sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      );
  }, [roleFilter, search, users]);

  const adminCount =
    users?.filter((user) => user.roleName === "ADMIN").length ?? 0;

  function change(user: User, roleName: "USER" | "ADMIN") {
    if (user.roleName === roleName) return;
    changeRole.mutate(
      { userId: user.id, roleName },
      {
        onSuccess: () =>
          toast.success(`${user.fullName} is now ${roleName.toLowerCase()}`),
        onError: (error) => toast.error(error.message),
      },
    );
  }

  function confirmDelete() {
    if (!deleteTarget) return;
    deleteUser.mutate(deleteTarget.id, {
      onSuccess: () => {
        setDeleteTarget(null);
        toast.success("Customer account deleted");
      },
      onError: (error) => toast.error(error.message),
    });
  }

  return (
    <div>
      <AdminPageHeader
        eyebrow="People"
        title="Customers"
        description="Everyone with an account. Change roles carefully — admins can edit the whole shop."
      />

      <div className="mt-6 grid gap-3 sm:grid-cols-3">
        <div className="border border-line bg-paper p-4">
          <p className="text-[10px] uppercase tracking-[0.12em] text-muted">
            Accounts
          </p>
          <p className="mt-3 font-display text-3xl tracking-[-0.04em] tabular-nums">
            {users?.length ?? "—"}
          </p>
        </div>
        <div className="border border-line bg-paper p-4">
          <p className="text-[10px] uppercase tracking-[0.12em] text-muted">
            Admins
          </p>
          <p className="mt-3 font-display text-3xl tracking-[-0.04em] tabular-nums">
            {adminCount}
          </p>
        </div>
        <div className="border border-line bg-paper p-4">
          <p className="text-[10px] uppercase tracking-[0.12em] text-muted">
            Customers
          </p>
          <p className="mt-3 font-display text-3xl tracking-[-0.04em] tabular-nums">
            {users ? users.length - adminCount : "—"}
          </p>
        </div>
      </div>

      <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="relative flex-1 sm:max-w-xs">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search name or email"
            aria-label="Search customers"
            className="h-10 rounded-full pl-10 text-[13px]"
          />
        </div>
        <div className="no-scrollbar flex items-center gap-1 overflow-x-auto">
          {(
            [
              { value: "ALL", label: "Everyone" },
              { value: "USER", label: "Customers" },
              { value: "ADMIN", label: "Admins" },
            ] as { value: RoleFilter; label: string }[]
          ).map((item) => (
            <button
              key={item.value}
              type="button"
              onClick={() => setRoleFilter(item.value)}
              aria-pressed={roleFilter === item.value}
              className={cn(
                "shrink-0 rounded-full px-3.5 py-1.5 text-[12px] font-medium transition-colors",
                roleFilter === item.value
                  ? "bg-ink text-paper"
                  : "text-ink-2 hover:bg-ink/[0.06]",
              )}
            >
              {item.label}
            </button>
          ))}
        </div>
        <p className="text-[11px] tabular-nums text-muted sm:ml-auto">
          {filtered.length} of {users?.length ?? 0}
        </p>
      </div>

      <TableShell className="mt-4">
        {isLoading ? (
          <div className="space-y-4 p-5">
            {Array.from({ length: 5 }).map((_, index) => (
              <div key={index} className="flex items-center gap-4">
                <Skeleton className="h-9 w-9 rounded-full" />
                <Skeleton className="h-3 flex-1" />
                <Skeleton className="h-3 w-24" />
                <Skeleton className="h-9 w-28" />
              </div>
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <AdminEmpty
            title="No customers match."
            description="Try another search term or role filter."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[820px] text-left text-[13px]">
              <thead className="border-b border-line bg-paper-3/60 text-[10px] uppercase tracking-[0.12em] text-muted">
                <tr>
                  <th className="px-5 py-3 font-medium">Customer</th>
                  <th className="px-5 py-3 font-medium">Joined</th>
                  <th className="px-5 py-3 font-medium">Orders</th>
                  <th className="px-5 py-3 font-medium">Paid value</th>
                  <th className="px-5 py-3 font-medium">Role</th>
                  <th className="px-5 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {filtered.map((user) => {
                  const stats = orderStats.get(user.id) ?? {
                    count: 0,
                    paid: 0,
                  };
                  const isSelf = me?.id === user.id;
                  const roleBusy =
                    changeRole.isPending &&
                    changeRole.variables?.userId === user.id;
                  return (
                    <tr key={user.id} className="table-row-hover">
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <span
                            className={cn(
                              "flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-[11px] font-semibold",
                              user.roleName === "ADMIN"
                                ? "bg-cobalt text-white"
                                : "bg-paper-2 text-ink-2",
                            )}
                          >
                            {initials(user.fullName)}
                          </span>
                          <div className="min-w-0">
                            <p className="max-w-52 truncate text-[12px] font-medium">
                              {user.fullName}
                              {isSelf ? (
                                <span className="ml-1.5 text-[10px] font-normal text-muted">
                                  you
                                </span>
                              ) : null}
                            </p>
                            <p className="max-w-52 truncate text-[10px] text-muted">
                              {user.email}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-3.5 text-[12px] text-muted">
                        {formatDate(user.createdAt)}
                      </td>
                      <td className="px-5 py-3.5">
                        {stats.count > 0 ? (
                          <Link
                            to="/admin/orders"
                            className="tabular-nums hover:text-cobalt"
                          >
                            {stats.count}
                          </Link>
                        ) : (
                          <span className="text-muted">0</span>
                        )}
                      </td>
                      <td className="px-5 py-3.5 tabular-nums">
                        {money(stats.paid)}
                      </td>
                      <td className="px-5 py-3.5">
                        {isSelf ? (
                          <Badge tone="cobalt">
                            <ShieldCheck className="h-3 w-3" />
                            Admin
                          </Badge>
                        ) : (
                          <Select
                            value={user.roleName === "ADMIN" ? "ADMIN" : "USER"}
                            disabled={roleBusy}
                            onChange={(event) =>
                              change(
                                user,
                                event.target.value as "USER" | "ADMIN",
                              )
                            }
                            aria-label={`Role for ${user.fullName}`}
                            className="h-9 min-w-28 rounded-full text-[12px]"
                          >
                            <option value="USER">Customer</option>
                            <option value="ADMIN">Admin</option>
                          </Select>
                        )}
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center justify-end">
                          {isSelf ? (
                            <span className="inline-flex items-center gap-1.5 text-[10px] text-muted">
                              <UserRound className="h-3.5 w-3.5" />
                              current account
                            </span>
                          ) : (
                            <button
                              type="button"
                              onClick={() => setDeleteTarget(user)}
                              aria-label={`Delete ${user.fullName}`}
                              className="flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-danger/[0.08] hover:text-danger"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          )}
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
        Role changes take effect on the next request. Deleting an account with
        orders may be rejected by the database — prefer keeping the record for
        history.
      </p>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
        title={`Delete ${deleteTarget?.fullName ?? "this account"}?`}
        description="The account and its sign-in are removed. This cannot be undone, and orders may block the delete if they reference this user."
        confirmLabel="Delete account"
        loading={deleteUser.isPending}
      />
    </div>
  );
}
