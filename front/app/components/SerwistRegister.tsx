"use client";
import { useEffect } from "react";

export default function SerwistRegister() {
  useEffect(() => {
    if (process.env.NODE_ENV === "production" && "serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js");
    }
  }, []);
  return null;
}
