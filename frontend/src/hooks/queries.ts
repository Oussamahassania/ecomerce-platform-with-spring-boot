import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api, queryString } from "@/lib/api";
import type {
  AuthResponse,
  Cart,
  Category,
  Order,
  OrderStatus,
  Page,
  Payment,
  PaymentStatus,
  Product,
  ProductInput,
  ProductRating,
  Review,
  ShopQuery,
  User,
} from "@/lib/types";
import { useAuth } from "@/stores/auth";
import { useUi } from "@/stores/ui";

export const keys = {
  products: ["products"] as const,
  shop: (params: ShopQuery) => ["products", "shop", params] as const,
  product: (id: number) => ["products", "detail", id] as const,
  categories: ["categories"] as const,
  cart: ["cart"] as const,
  orders: ["orders"] as const,
  order: (id: number) => ["orders", "detail", id] as const,
  reviews: (productId: number) => ["reviews", productId] as const,
  rating: (productId: number) => ["rating", productId] as const,
  me: ["me"] as const,
  users: ["admin", "users"] as const,
  allOrders: ["admin", "orders"] as const,
  lowStock: ["admin", "low-stock"] as const,
  orderCount: ["admin", "order-count"] as const,
  revenue: ["admin", "revenue"] as const,
};

/* ---------------------------------- shop --------------------------------- */

export function useShopProducts(params: ShopQuery) {
  const search = queryString({
    name: params.q,
    minPrice: params.minPrice,
    maxPrice: params.maxPrice,
    categoryId: params.category,
    sort: params.sort ?? "createdAt",
    order: params.order ?? "DESC",
    page: params.page ?? 0,
    size: params.size ?? 12,
  });

  return useQuery({
    queryKey: keys.shop(params),
    queryFn: () => api<Page<Product>>(`/api/products/search${search}`),
    placeholderData: keepPreviousData,
  });
}

export function useAllProducts() {
  return useQuery({
    queryKey: [...keys.products, "all"],
    queryFn: () => api<Product[]>("/api/products/AllProducts"),
  });
}

export function useProduct(id: number | undefined) {
  return useQuery({
    queryKey: keys.product(id ?? 0),
    queryFn: () => api<Product>(`/api/products/${id}`),
    enabled: Boolean(id),
  });
}

export function useCategories() {
  return useQuery({
    queryKey: keys.categories,
    queryFn: () => api<Category[]>("/api/categories"),
    staleTime: 10 * 60_000,
  });
}

export function useCategoryProducts(categoryId: number | null, size = 8) {
  return useQuery({
    queryKey: [...keys.products, "category", categoryId, size],
    queryFn: () =>
      api<Page<Product>>(
        `/api/products/filter${queryString({
          categoryId,
          page: 0,
          size,
          sort: "createdAt",
          order: "DESC",
        })}`,
      ),
    enabled: Boolean(categoryId),
  });
}

export function useReviews(productId: number | undefined) {
  return useQuery({
    queryKey: keys.reviews(productId ?? 0),
    queryFn: () => api<Review[]>(`/api/review/${productId}`),
    enabled: Boolean(productId),
  });
}

export function useProductRating(productId: number | undefined) {
  return useQuery({
    queryKey: keys.rating(productId ?? 0),
    queryFn: () => api<ProductRating>(`/api/review/${productId}/rating`),
    enabled: Boolean(productId),
  });
}

/* ---------------------------------- cart --------------------------------- */

export function useCart() {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: keys.cart,
    queryFn: () => api<Cart>("/api/cart"),
    enabled: Boolean(token),
  });
}

export function useAddToCart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { productId: number; quantity: number }) =>
      api<Cart>("/api/cart/add", { method: "POST", body: input }),
    onSuccess: (cart) => {
      queryClient.setQueryData(keys.cart, cart);
      useUi.getState().openCart();
    },
  });
}

export function useUpdateCartItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { itemId: number; quantity: number }) =>
      api<Cart>(`/api/cart/update/${input.itemId}?quantity=${input.quantity}`, {
        method: "PUT",
      }),
    onMutate: async (input) => {
      await queryClient.cancelQueries({ queryKey: keys.cart });
      const previous = queryClient.getQueryData<Cart>(keys.cart);
      if (previous) {
        const items = previous.items.map((item) =>
          item.id === input.itemId
            ? {
                ...item,
                quantity: input.quantity,
                subtotal: Number(item.priceAtAdding) * input.quantity,
              }
            : item,
        );
        queryClient.setQueryData<Cart>(keys.cart, {
          ...previous,
          items,
          total: items.reduce((sum, item) => sum + Number(item.subtotal), 0),
        });
      }
      return { previous };
    },
    onError: (_error, _input, context) => {
      if (context?.previous) {
        queryClient.setQueryData(keys.cart, context.previous);
      }
    },
    onSuccess: (cart) => queryClient.setQueryData(keys.cart, cart),
  });
}

export function useRemoveCartItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (itemId: number) =>
      api<Cart>(`/api/cart/remove/${itemId}`, { method: "DELETE" }),
    onMutate: async (itemId) => {
      await queryClient.cancelQueries({ queryKey: keys.cart });
      const previous = queryClient.getQueryData<Cart>(keys.cart);
      if (previous) {
        const items = previous.items.filter((item) => item.id !== itemId);
        queryClient.setQueryData<Cart>(keys.cart, {
          ...previous,
          items,
          total: items.reduce((sum, item) => sum + Number(item.subtotal), 0),
        });
      }
      return { previous };
    },
    onError: (_error, _itemId, context) => {
      if (context?.previous) {
        queryClient.setQueryData(keys.cart, context.previous);
      }
    },
    onSuccess: (cart) => queryClient.setQueryData(keys.cart, cart),
  });
}

export function useClearCart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api<string>("/api/cart/clear", { method: "DELETE" }),
    onSuccess: () => {
      queryClient.setQueryData(keys.cart, null);
      queryClient.invalidateQueries({ queryKey: keys.cart });
    },
  });
}

export function useCheckout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      api<Order>("/api/cart/checkout", { method: "POST" }),
    onSuccess: (order) => {
      queryClient.invalidateQueries({ queryKey: keys.cart });
      queryClient.invalidateQueries({ queryKey: keys.orders });
      queryClient.setQueryData(keys.order(order.id), order);
    },
  });
}

/* --------------------------------- orders -------------------------------- */

export function useMyOrders() {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: keys.orders,
    queryFn: () => api<Order[]>("/api/orders/my-orders"),
    enabled: Boolean(token),
  });
}

export function useOrder(id: number | undefined) {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: keys.order(id ?? 0),
    queryFn: () => api<Order>(`/api/orders/order/${id}`),
    enabled: Boolean(id && token),
  });
}

export function usePayOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      orderId: number;
      paymentMethod: string;
      idempotencyKey: string;
    }) =>
      api<Payment>(`/api/payment/pay/${input.orderId}`, {
        method: "POST",
        body: { paymentMethod: input.paymentMethod },
        headers: { "idempotency-key": input.idempotencyKey },
      }),
    onSuccess: (payment) => {
      queryClient.invalidateQueries({ queryKey: keys.order(payment.orderId) });
      queryClient.invalidateQueries({ queryKey: keys.orders });
      queryClient.invalidateQueries({ queryKey: keys.allOrders });
      queryClient.invalidateQueries({ queryKey: keys.revenue });
      queryClient.invalidateQueries({ queryKey: keys.orderCount });
    },
  });
}

export function useCancelOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orderId: number) =>
      api<Order>(`/api/orders/${orderId}/cancel`, { method: "PUT" }),
    onSuccess: (order) => {
      queryClient.setQueryData(keys.order(order.id), order);
      queryClient.invalidateQueries({ queryKey: keys.orders });
      queryClient.invalidateQueries({ queryKey: keys.allOrders });
      queryClient.invalidateQueries({ queryKey: keys.products });
    },
  });
}

export function useRefundOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (orderId: number) =>
      api<Payment>(`/api/payment/refund/${orderId}`, { method: "POST" }),
    onSuccess: (payment) => {
      queryClient.invalidateQueries({ queryKey: keys.order(payment.orderId) });
      queryClient.invalidateQueries({ queryKey: keys.orders });
      queryClient.invalidateQueries({ queryKey: keys.allOrders });
      queryClient.invalidateQueries({ queryKey: keys.products });
    },
  });
}

/* ---------------------------------- auth --------------------------------- */

export function useMe() {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: keys.me,
    queryFn: () => api<User>("/api/users/me"),
    enabled: Boolean(token),
    staleTime: 5 * 60_000,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();
  const setSession = useAuth((state) => state.setSession);
  return useMutation({
    mutationFn: (input: { email: string; password: string }) =>
      api<AuthResponse>("/api/auth/login", {
        method: "POST",
        body: input,
        auth: false,
      }),
    onSuccess: (response) => {
      setSession(response.accessToken);
      queryClient.invalidateQueries({ queryKey: keys.me });
      queryClient.invalidateQueries({ queryKey: keys.cart });
      queryClient.invalidateQueries({ queryKey: keys.orders });
    },
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: (input: {
      fullName: string;
      email: string;
      password: string;
      dateOfBirth?: string | null;
    }) =>
      api<string>("/api/auth/registration", {
        method: "POST",
        body: input,
        auth: false,
      }),
  });
}

export function useVerifyEmail() {
  return useMutation({
    mutationFn: (token: string) =>
      api<string>(`/api/auth/verify?token=${encodeURIComponent(token)}`, {
        auth: false,
      }),
  });
}

export function useDevVerify() {
  return useMutation({
    mutationFn: (email: string) =>
      api<string>(
        `/api/dev/verify?email=${encodeURIComponent(email)}`,
        { method: "POST", auth: false },
      ),
  });
}

export function useAddReview(productId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { rating: number; comment: string }) =>
      api<Review>(`/api/review/${productId}`, {
        method: "POST",
        body: input,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.reviews(productId) });
      queryClient.invalidateQueries({ queryKey: keys.rating(productId) });
    },
  });
}

export function useDeleteReview(productId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reviewId: number) =>
      api<string>(`/api/review/${reviewId}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.reviews(productId) });
      queryClient.invalidateQueries({ queryKey: keys.rating(productId) });
    },
  });
}

/* ---------------------------------- admin -------------------------------- */

export function useAdminUsers() {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: keys.users,
    queryFn: () => api<User[]>("/api/users/all"),
    enabled: Boolean(token),
  });
}

export function useAdminOrders() {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: keys.allOrders,
    queryFn: () => api<Order[]>("/api/orders/AllOrders"),
    enabled: Boolean(token),
  });
}

export function useAdminOrderCount() {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: keys.orderCount,
    queryFn: () => api<number>("/api/orders/countOrders"),
    enabled: Boolean(token),
  });
}

export function useAdminRevenue() {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: keys.revenue,
    queryFn: () => api<number>("/api/orders/totalRevenue"),
    enabled: Boolean(token),
  });
}

export function useLowStock() {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: keys.lowStock,
    queryFn: () => api<Product[]>("/api/products/low-stock"),
    enabled: Boolean(token),
  });
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: ProductInput) =>
      api<Product>("/api/products/createProduct", {
        method: "POST",
        body: input,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.products });
      queryClient.invalidateQueries({ queryKey: keys.lowStock });
    },
  });
}

export function useUpdateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; data: ProductInput }) =>
      api<Product>(`/api/products/updateProduct/${input.id}`, {
        method: "PUT",
        body: input.data,
      }),
    onSuccess: (product) => {
      queryClient.invalidateQueries({ queryKey: keys.products });
      queryClient.setQueryData(keys.product(product.id), product);
      queryClient.invalidateQueries({ queryKey: keys.lowStock });
    },
  });
}

export function useDeleteProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      api<string>(`/api/products/delete/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.products });
      queryClient.invalidateQueries({ queryKey: keys.lowStock });
    },
  });
}

export function useUpdateStock() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; stock: number }) =>
      api<Product>(`/api/products/${input.id}/stock`, {
        method: "PUT",
        body: { stock: input.stock },
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.products });
      queryClient.invalidateQueries({ queryKey: keys.lowStock });
    },
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; description: string }) =>
      api<Category>("/api/categories/create", { method: "POST", body: input }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: keys.categories }),
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      id: number;
      data: { name: string; description: string };
    }) =>
      api<Category>(`/api/categories/update/${input.id}`, {
        method: "PUT",
        body: input.data,
      }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: keys.categories }),
  });
}

export function useChangeRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { userId: number; roleName: "USER" | "ADMIN" }) =>
      api<string>(
        `/api/roles/change?userId=${input.userId}&roleName=${input.roleName}`,
        { method: "PUT" },
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.users }),
  });
}

export function useDeleteUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      api<string>(`/api/users/delete/${id}`, { method: "DELETE" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.users }),
  });
}

export function useUpdateOrderStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { orderId: number; status: OrderStatus }) =>
      api<Order>(
        `/api/orders/${input.orderId}/status?status=${input.status}`,
        { method: "PUT" },
      ),
    onSuccess: (order) => {
      queryClient.setQueryData(keys.order(order.id), order);
      queryClient.invalidateQueries({ queryKey: keys.allOrders });
      queryClient.invalidateQueries({ queryKey: keys.orders });
    },
  });
}

export function useDeleteOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      api<string>(`/api/orders/delete/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.allOrders });
      queryClient.invalidateQueries({ queryKey: keys.orderCount });
      queryClient.invalidateQueries({ queryKey: keys.revenue });
    },
  });
}

export function useUploadImage() {
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData();
      form.append("file", file);
      return api<string>("/api/files/uplaod", {
        method: "POST",
        body: form,
      });
    },
  });
}

export function usePaymentStatus(orderId: number | undefined, enabled = true) {
  const token = useAuth((state) => state.token);
  return useQuery({
    queryKey: [...keys.order(orderId ?? 0), "payment"],
    queryFn: () => api<Payment>(`/api/payment/${orderId}`),
    enabled: Boolean(orderId && token && enabled),
  });
}

export type { PaymentStatus };
