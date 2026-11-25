import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowUpRight, Pencil, Plus, Tags } from "lucide-react";
import { toast } from "sonner";
import {
  useAllProducts,
  useCategories,
  useCreateCategory,
  useUpdateCategory,
} from "@/hooks/queries";
import type { Category } from "@/lib/types";
import {
  Badge,
  Button,
  Field,
  Input,
  Skeleton,
  Textarea,
} from "@/components/ui";
import { Drawer } from "@/components/ui/Overlay";
import { AdminPageHeader, TableShell } from "@/components/admin/AdminUI";

function CategoryForm({
  category,
  onClose,
}: {
  category: Category | null;
  onClose: () => void;
}) {
  const createCategory = useCreateCategory();
  const updateCategory = useUpdateCategory();
  const [name, setName] = useState(category?.name ?? "");
  const [description, setDescription] = useState(category?.description ?? "");
  const [error, setError] = useState<string | null>(null);

  const busy = createCategory.isPending || updateCategory.isPending;

  function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!name.trim()) {
      setError("A category needs a name.");
      return;
    }
    const data = { name: name.trim(), description: description.trim() };
    if (category) {
      updateCategory.mutate(
        { id: category.id, data },
        {
          onSuccess: () => {
            toast.success("Category updated");
            onClose();
          },
          onError: (mutationError) => setError(mutationError.message),
        },
      );
    } else {
      createCategory.mutate(data, {
        onSuccess: () => {
          toast.success("Category created");
          onClose();
        },
        onError: (mutationError) => setError(mutationError.message),
      });
    }
  }

  return (
    <Drawer
      open
      onClose={onClose}
      title={category ? "Edit category" : "New category"}
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button type="submit" form="category-form" disabled={busy}>
            {busy ? "Saving…" : category ? "Save changes" : "Create category"}
          </Button>
        </div>
      }
    >
      <form id="category-form" onSubmit={submit} noValidate className="space-y-5 p-6">
        {error ? (
          <div
            role="alert"
            className="rounded-sm border border-danger/25 bg-danger/[0.05] px-4 py-3 text-[12px] text-danger"
          >
            {error}
          </div>
        ) : null}
        <Field label="Name" htmlFor="category-name">
          <Input
            id="category-name"
            value={name}
            onChange={(event) => {
              setName(event.target.value);
              setError(null);
            }}
            placeholder="Audio"
            autoFocus
          />
        </Field>
        <Field
          label="Description"
          htmlFor="category-description"
          hint="Shown on the shop page when this category is selected."
        >
          <Textarea
            id="category-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Speakers and headphones chosen for long listening."
            className="min-h-24"
          />
        </Field>
      </form>
    </Drawer>
  );
}

export function AdminCategoriesPage() {
  const { data: categories, isLoading } = useCategories();
  const { data: products } = useAllProducts();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);

  const counts = useMemo(() => {
    const map = new Map<number, number>();
    for (const product of products ?? []) {
      if (product.categoryId) {
        map.set(
          product.categoryId,
          (map.get(product.categoryId) ?? 0) + 1,
        );
      }
    }
    return map;
  }, [products]);

  const uncategorised =
    products?.filter((product) => !product.categoryId).length ?? 0;

  function openNew() {
    setEditing(null);
    setFormOpen(true);
  }

  function openEdit(category: Category) {
    setEditing(category);
    setFormOpen(true);
  }

  return (
    <div>
      <AdminPageHeader
        eyebrow="Catalogue"
        title="Categories"
        description="The five ways into the shop. Rename or rewrite them here; products keep their assignment."
        action={
          <button
            type="button"
            onClick={openNew}
            className="inline-flex h-10 items-center gap-2 rounded-full bg-cobalt px-5 text-[13px] font-medium text-white transition-colors hover:bg-cobalt-ink"
          >
            <Plus className="h-4 w-4" />
            New category
          </button>
        }
      />

      <div className="mt-6 grid gap-3 sm:grid-cols-3">
        <div className="border border-line bg-paper p-4">
          <p className="text-[10px] uppercase tracking-[0.12em] text-muted">
            Categories
          </p>
          <p className="mt-3 font-display text-3xl tracking-[-0.04em] tabular-nums">
            {categories?.length ?? "—"}
          </p>
        </div>
        <div className="border border-line bg-paper p-4">
          <p className="text-[10px] uppercase tracking-[0.12em] text-muted">
            Products assigned
          </p>
          <p className="mt-3 font-display text-3xl tracking-[-0.04em] tabular-nums">
            {products ? products.length - uncategorised : "—"}
          </p>
        </div>
        <div className="border border-line bg-paper p-4">
          <p className="text-[10px] uppercase tracking-[0.12em] text-muted">
            Uncategorised
          </p>
          <p className="mt-3 font-display text-3xl tracking-[-0.04em] tabular-nums">
            {products ? uncategorised : "—"}
          </p>
        </div>
      </div>

      {isLoading ? (
        <div className="mt-6 grid gap-3 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-40 w-full" />
          ))}
        </div>
      ) : categories && categories.length > 0 ? (
        <div className="mt-6 grid gap-3 md:grid-cols-2">
          {categories.map((category) => (
            <article
              key={category.id}
              className="group border border-line bg-paper p-5 transition-colors hover:border-ink/25"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-3">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-ink/[0.05]">
                    <Tags className="h-4 w-4" />
                  </span>
                  <div>
                    <h2 className="font-display text-2xl tracking-[-0.04em]">
                      {category.name}
                    </h2>
                    <p className="mt-1 text-[11px] tabular-nums text-muted">
                      {counts.get(category.id) ?? 0}{" "}
                      {(counts.get(category.id) ?? 0) === 1
                        ? "product"
                        : "products"}
                    </p>
                  </div>
                </div>
                <Badge tone="outline">#{category.id}</Badge>
              </div>

              <p className="mt-4 min-h-10 text-[12px] leading-relaxed text-muted">
                {category.description || "No description yet."}
              </p>

              <div className="mt-4 flex items-center justify-between border-t border-line pt-3">
                <Link
                  to={`/shop?category=${category.id}`}
                  className="inline-flex items-center gap-1.5 text-[11px] text-muted transition-colors hover:text-ink"
                >
                  View in shop
                  <ArrowUpRight className="h-3 w-3" />
                </Link>
                <button
                  type="button"
                  onClick={() => openEdit(category)}
                  className="inline-flex items-center gap-1.5 text-[11px] font-medium text-ink transition-colors hover:text-cobalt"
                >
                  <Pencil className="h-3 w-3" />
                  Edit
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <TableShell className="mt-6">
          <div className="p-10 text-center">
            <p className="font-display text-2xl tracking-[-0.03em]">
              No categories yet.
            </p>
            <p className="mt-2 text-[12px] text-muted">
              Create one to give products a place in the shop.
            </p>
            <Button onClick={openNew} className="mt-5">
              <Plus className="h-4 w-4" />
              New category
            </Button>
          </div>
        </TableShell>
      )}

      <p className="mt-4 text-[11px] leading-relaxed text-muted">
        The backend does not expose category deletion. Rename a category or
        reassign its products instead — this keeps historical orders and links
        stable.
      </p>

      {formOpen ? (
        <CategoryForm
          key={editing?.id ?? "new"}
          category={editing}
          onClose={() => {
            setFormOpen(false);
            setEditing(null);
          }}
        />
      ) : null}
    </div>
  );
}
