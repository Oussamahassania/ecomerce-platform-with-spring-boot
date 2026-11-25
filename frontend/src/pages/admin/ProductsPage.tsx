import { useMemo, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  ArrowUpRight,
  ImagePlus,
  Pencil,
  Plus,
  Search,
  Trash2,
  Upload,
} from "lucide-react";
import { toast } from "sonner";
import {
  useAllProducts,
  useCategories,
  useCreateProduct,
  useDeleteProduct,
  useUpdateProduct,
  useUploadImage,
} from "@/hooks/queries";
import { money } from "@/lib/format";
import { imageUrl, productImage } from "@/lib/images";
import type { Category, Product, ProductInput } from "@/lib/types";
import {
  Badge,
  Button,
  Field,
  Input,
  Select,
  Skeleton,
  Textarea,
} from "@/components/ui";
import { ConfirmDialog, Drawer } from "@/components/ui/Overlay";
import {
  AdminEmpty,
  AdminPageHeader,
  TableShell,
} from "@/components/admin/AdminUI";

function stockTone(stock: number) {
  if (stock <= 0) return <Badge tone="danger">Sold out</Badge>;
  if (stock <= 5) return <Badge tone="butter">{stock} left</Badge>;
  return <Badge tone="neutral">{stock} in stock</Badge>;
}

function ProductForm({
  product,
  categories,
  onClose,
}: {
  product: Product | null;
  categories: Category[] | undefined;
  onClose: () => void;
}) {
  const createProduct = useCreateProduct();
  const updateProduct = useUpdateProduct();
  const upload = useUploadImage();
  const fileRef = useRef<HTMLInputElement>(null);

  const [name, setName] = useState(product?.name ?? "");
  const [description, setDescription] = useState(product?.description ?? "");
  const [price, setPrice] = useState(
    product ? String(product.price) : "",
  );
  const [stock, setStock] = useState(
    product ? String(product.stock) : "10",
  );
  const [categoryId, setCategoryId] = useState(
    product?.categoryId ? String(product.categoryId) : "",
  );
  const [imgUrl, setImgUrl] = useState(product?.img_url ?? "");
  const [errors, setErrors] = useState<Record<string, string>>({});

  const busy = createProduct.isPending || updateProduct.isPending;

  function validate() {
    const next: Record<string, string> = {};
    if (!name.trim()) next.name = "A product needs a name.";
    if (description.trim().length < 20)
      next.description = "Write at least 20 characters — 200 maximum.";
    if (description.trim().length > 200)
      next.description = "Keep the description under 200 characters.";
    const priceNumber = Number(price);
    if (!price.trim() || Number.isNaN(priceNumber) || priceNumber <= 0)
      next.price = "Enter a price above zero.";
    const stockNumber = Number(stock);
    if (!stock.trim() || Number.isNaN(stockNumber) || stockNumber < 0)
      next.stock = "Stock cannot be negative.";
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!validate()) return;

    const data: ProductInput = {
      name: name.trim(),
      description: description.trim(),
      price: Math.round(Number(price) * 100) / 100,
      stock: Math.floor(Number(stock)),
      img_url: imgUrl.trim(),
      categoryId: categoryId ? Number(categoryId) : null,
    };

    if (product) {
      updateProduct.mutate(
        { id: product.id, data },
        {
          onSuccess: () => {
            toast.success("Product updated");
            onClose();
          },
          onError: (error) => toast.error(error.message),
        },
      );
    } else {
      createProduct.mutate(data, {
        onSuccess: () => {
          toast.success("Product added to the shelf");
          onClose();
        },
        onError: (error) => toast.error(error.message),
      });
    }
  }

  function handleUpload(file: File | undefined) {
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      toast.error("Choose an image file");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      toast.error("Keep the image under 5 MB");
      return;
    }
    upload.mutate(file, {
      onSuccess: (url) => {
        setImgUrl(url);
        toast.success("Image uploaded");
      },
      onError: (error) => toast.error(error.message),
    });
  }

  const preview = imageUrl(imgUrl.trim());

  return (
    <Drawer
      open
      onClose={onClose}
      title={product ? "Edit product" : "New product"}
      className="max-w-[560px]"
      footer={
        <div className="flex items-center justify-between gap-3">
          <p className="hidden text-[11px] text-muted sm:block">
            {product ? `Product #${product.id}` : "A new object for the shelf"}
          </p>
          <div className="flex w-full gap-2 sm:w-auto">
            <Button
              variant="ghost"
              onClick={onClose}
              disabled={busy}
              className="flex-1 sm:flex-none"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              form="product-form"
              disabled={busy || upload.isPending}
              className="flex-1 sm:flex-none"
            >
              {busy ? "Saving…" : product ? "Save changes" : "Add product"}
            </Button>
          </div>
        </div>
      }
    >
      <form id="product-form" onSubmit={submit} noValidate className="space-y-5 p-6">
        <Field label="Name" htmlFor="product-name" error={errors.name}>
          <Input
            id="product-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Orbit Speaker"
            autoFocus
          />
        </Field>

        <Field
          label="Description"
          htmlFor="product-description"
          error={errors.description}
          hint={`${description.length}/200 characters — 20 minimum`}
        >
          <Textarea
            id="product-description"
            value={description}
            maxLength={200}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="What it is, how it feels, why it earns its place."
            className="min-h-28"
          />
        </Field>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Price (EUR)" htmlFor="product-price" error={errors.price}>
            <Input
              id="product-price"
              type="number"
              min="0"
              step="0.01"
              value={price}
              onChange={(event) => setPrice(event.target.value)}
              placeholder="189.00"
              className="hide-spin"
            />
          </Field>
          <Field label="Stock" htmlFor="product-stock" error={errors.stock}>
            <Input
              id="product-stock"
              type="number"
              min="0"
              step="1"
              value={stock}
              onChange={(event) => setStock(event.target.value)}
              placeholder="10"
              className="hide-spin"
            />
          </Field>
        </div>

        <Field label="Category" htmlFor="product-category">
          <Select
            id="product-category"
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value)}
          >
            <option value="">No category</option>
            {categories?.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </Select>
        </Field>

        <div>
          <p className="mb-2 text-[13px] font-medium text-ink-2">Image</p>
          <div className="grid grid-cols-[96px_1fr] gap-4">
            <div className="relative aspect-[4/5] overflow-hidden border border-line bg-paper-2">
              {preview ? (
                <img
                  src={preview}
                  alt=""
                  className="h-full w-full object-cover"
                  onError={(event) => {
                    event.currentTarget.src = "/images/products/placeholder.svg";
                  }}
                />
              ) : (
                <div className="flex h-full flex-col items-center justify-center gap-1 text-muted">
                  <ImagePlus className="h-5 w-5" />
                  <span className="text-[10px]">No image</span>
                </div>
              )}
            </div>
            <div className="space-y-2">
              <Input
                value={imgUrl}
                onChange={(event) => setImgUrl(event.target.value)}
                placeholder="/images/products/… or upload"
                className="h-10 text-[12px]"
                aria-label="Image URL"
              />
              <input
                ref={fileRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(event) => {
                  handleUpload(event.target.files?.[0]);
                  event.target.value = "";
                }}
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => fileRef.current?.click()}
                disabled={upload.isPending}
              >
                <Upload className="h-3.5 w-3.5" />
                {upload.isPending ? "Uploading…" : "Upload image"}
              </Button>
              <p className="text-[10px] leading-relaxed text-muted">
                JPG, PNG or WebP under 5 MB. Files are served from the backend
                uploads folder.
              </p>
            </div>
          </div>
        </div>
      </form>
    </Drawer>
  );
}

export function AdminProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: products, isLoading } = useAllProducts();
  const { data: categories } = useCategories();
  const deleteProduct = useDeleteProduct();

  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [formOpen, setFormOpen] = useState(searchParams.get("new") === "1");
  const [editing, setEditing] = useState<Product | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return (products ?? []).filter((product) => {
      if (term && !product.name.toLowerCase().includes(term)) return false;
      if (category && String(product.categoryId) !== category) return false;
      return true;
    });
  }, [category, products, search]);

  function openNew() {
    setEditing(null);
    setFormOpen(true);
  }

  function openEdit(product: Product) {
    setEditing(product);
    setFormOpen(true);
  }

  function closeForm() {
    setFormOpen(false);
    setEditing(null);
    if (searchParams.has("new")) {
      const next = new URLSearchParams(searchParams);
      next.delete("new");
      setSearchParams(next, { replace: true });
    }
  }

  function confirmDelete() {
    if (!deleteTarget) return;
    deleteProduct.mutate(deleteTarget.id, {
      onSuccess: () => {
        setDeleteTarget(null);
        toast.success(`${deleteTarget.name} removed from the shelf`);
      },
      onError: (error) => toast.error(error.message),
    });
  }

  return (
    <div>
      <AdminPageHeader
        eyebrow="Catalogue"
        title="Products"
        description="Everything currently on the shelf. Deleting a product hides it from the storefront but keeps past orders intact."
        action={
          <button
            type="button"
            onClick={openNew}
            className="inline-flex h-10 items-center gap-2 rounded-full bg-cobalt px-5 text-[13px] font-medium text-white transition-colors hover:bg-cobalt-ink"
          >
            <Plus className="h-4 w-4" />
            New product
          </button>
        }
      />

      <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="relative flex-1 sm:max-w-xs">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search products"
            aria-label="Search products"
            className="h-10 rounded-full pl-10 text-[13px]"
          />
        </div>
        <div className="sm:w-52">
          <Select
            value={category}
            onChange={(event) => setCategory(event.target.value)}
            aria-label="Filter by category"
            className="h-10 rounded-full text-[13px]"
          >
            <option value="">All categories</option>
            {categories?.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </Select>
        </div>
        <p className="text-[11px] tabular-nums text-muted sm:ml-auto">
          {filtered.length} of {products?.length ?? 0} products
        </p>
      </div>

      <TableShell className="mt-4">
        {isLoading ? (
          <div className="space-y-4 p-5">
            {Array.from({ length: 5 }).map((_, index) => (
              <div key={index} className="flex items-center gap-4">
                <Skeleton className="h-12 w-10" />
                <Skeleton className="h-3 flex-1" />
                <Skeleton className="h-3 w-20" />
                <Skeleton className="h-8 w-16" />
              </div>
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <AdminEmpty
            title={search || category ? "No products match." : "The shelf is empty."}
            description={
              search || category
                ? "Try a different search or category."
                : "Add the first product to start building the catalogue."
            }
            action={
              <button
                type="button"
                onClick={openNew}
                className="inline-flex h-10 items-center gap-2 rounded-full bg-ink px-5 text-[13px] font-medium text-paper"
              >
                <Plus className="h-4 w-4" />
                New product
              </button>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-[13px]">
              <thead className="border-b border-line bg-paper-3/60 text-[10px] uppercase tracking-[0.12em] text-muted">
                <tr>
                  <th className="px-5 py-3 font-medium">Product</th>
                  <th className="px-5 py-3 font-medium">Category</th>
                  <th className="px-5 py-3 font-medium">Price</th>
                  <th className="px-5 py-3 font-medium">Stock</th>
                  <th className="px-5 py-3 font-medium">Added</th>
                  <th className="px-5 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {filtered.map((product) => (
                  <tr key={product.id} className="table-row-hover">
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <div className="h-12 w-10 shrink-0 overflow-hidden bg-paper-2">
                          <img
                            src={productImage(product.img_url)}
                            alt=""
                            className="h-full w-full object-cover"
                          />
                        </div>
                        <div className="min-w-0">
                          <button
                            type="button"
                            onClick={() => openEdit(product)}
                            className="max-w-56 truncate text-left font-medium hover:text-cobalt"
                          >
                            {product.name}
                          </button>
                          <p className="max-w-56 truncate text-[10px] text-muted">
                            {product.description}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-[12px] text-muted">
                      {product.categoryName ?? "—"}
                    </td>
                    <td className="px-5 py-3.5 tabular-nums">
                      {money(product.price)}
                    </td>
                    <td className="px-5 py-3.5">{stockTone(product.stock)}</td>
                    <td className="px-5 py-3.5 text-[12px] text-muted">
                      {new Date(product.createdAt).toLocaleDateString("en-GB", {
                        day: "numeric",
                        month: "short",
                        year: "numeric",
                      })}
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1">
                        <Link
                          to={`/product/${product.id}`}
                          aria-label={`View ${product.name} in storefront`}
                          className="flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-ink/[0.06] hover:text-ink"
                        >
                          <ArrowUpRight className="h-3.5 w-3.5" />
                        </Link>
                        <button
                          type="button"
                          onClick={() => openEdit(product)}
                          aria-label={`Edit ${product.name}`}
                          className="flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-ink/[0.06] hover:text-ink"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteTarget(product)}
                          aria-label={`Delete ${product.name}`}
                          className="flex h-8 w-8 items-center justify-center rounded-full text-muted transition-colors hover:bg-danger/[0.08] hover:text-danger"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </TableShell>

      <p className="mt-4 text-[11px] leading-relaxed text-muted">
        Deleting is a soft delete: the product leaves the storefront and search
        but stays attached to historical orders.
      </p>

      {formOpen ? (
        <ProductForm
          key={editing?.id ?? "new"}
          product={editing}
          categories={categories}
          onClose={closeForm}
        />
      ) : null}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
        title={`Remove ${deleteTarget?.name ?? "this product"}?`}
        description="It will disappear from the storefront and search. Past orders keep their record and images."
        confirmLabel="Remove product"
        loading={deleteProduct.isPending}
      />
    </div>
  );
}
