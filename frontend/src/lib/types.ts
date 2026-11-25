export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED";

export type PaymentStatus = "PENDING" | "PAID" | "FAILED" | "REFUNDED";

export type Role = "USER" | "ADMIN";

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stock: number;
  img_url: string | null;
  createdAt: string;
  categoryId: number | null;
  categoryName: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface Category {
  id: number;
  name: string;
  description: string | null;
}

export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productImage: string | null;
  quantity: number;
  priceAtAdding: number;
  subtotal: number;
}

export interface Cart {
  id: number;
  userId: number;
  items: CartItem[];
  total: number;
  updatedAt: string;
}

export interface OrderItem {
  quantity: number;
  price: number;
  productId: number;
  productName?: string | null;
  productImage?: string | null;
}

export interface Order {
  id: number;
  userId: number;
  status: OrderStatus;
  orderDate: string;
  totalAmount: number;
  paymentStatus: PaymentStatus;
  paymentReference: string | null;
  items: OrderItem[];
}

export interface Payment {
  id: number;
  orderId: number;
  paymentReference: string;
  amount: number;
  status: PaymentStatus;
  failureReason: string | null;
  createdAt: string;
  processedAt: string | null;
}

export interface Review {
  id: number;
  productId: number;
  productName: string;
  userId: number;
  userFullName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface ProductRating {
  productId: number;
  averageRating: number;
  totalReviews: number;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string | null;
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  dateOfBirth: string | null;
  createdAt: string;
  roleName: string;
}

export interface ProductInput {
  name: string;
  description: string;
  price: number;
  stock: number;
  img_url: string;
  categoryId: number | null;
}

export interface ShopQuery {
  q?: string;
  category?: number | null;
  minPrice?: number | null;
  maxPrice?: number | null;
  sort?: string;
  order?: "ASC" | "DESC";
  page?: number;
  size?: number;
}
