import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "sonner";
import App from "./App";
import { useMediaQuery } from "./hooks/useMediaQuery";
import { queryClient } from "./lib/queryClient";
import "./index.css";

function AppToaster() {
  const isMobile = useMediaQuery("(max-width: 767px)");

  return (
    <Toaster
      position={isMobile ? "top-center" : "bottom-left"}
      offset={isMobile ? 84 : 24}
      mobileOffset={84}
      closeButton
      toastOptions={{
        style: {
          background: "#14150F",
          color: "#EDEDE6",
          border: "1px solid rgba(237,237,230,0.14)",
          borderRadius: "6px",
          fontSize: "13px",
        },
        classNames: {
          description: "!text-paper/60",
          actionButton: "!bg-butter !text-ink",
          closeButton: "!bg-paper !text-ink !border-0",
        },
      }}
    />
  );
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
        <AppToaster />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
