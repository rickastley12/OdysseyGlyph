"use client";

import { useEffect, useRef } from "react";

const COLS = 48;
const ROWS = 12;
const DOT_SIZE = 8;
const GAP = 4;
const TOTAL_WIDTH = COLS * DOT_SIZE + (COLS - 1) * GAP;
const TOTAL_HEIGHT = ROWS * DOT_SIZE + (ROWS - 1) * GAP;
const TOTAL_CELLS = COLS * ROWS;

interface DotState {
  val: number;
  target: number;
  duration: number;
  elapsed: number;
  isRed: boolean;
}

export default function LedGrid() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d", { alpha: false });
    if (!ctx) return;

    const osReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const shouldReduceMotion = process.env.NODE_ENV === "production" ? osReducedMotion : false;

    // Handle high-DPI displays for crisp rendering
    const dpr = window.devicePixelRatio || 1;
    canvas.width = TOTAL_WIDTH * dpr;
    canvas.height = TOTAL_HEIGHT * dpr;
    canvas.style.width = `${TOTAL_WIDTH}px`;
    canvas.style.height = `${TOTAL_HEIGHT}px`;
    ctx.scale(dpr, dpr);

    // Initialize per-dot noise states
    const dotsState: DotState[] = Array.from({ length: TOTAL_CELLS }, () => ({
      val: Math.random() * 0.8,
      target: Math.random() * 0.8,
      duration: 300 + Math.random() * 500,
      elapsed: Math.random() * 800,
      isRed: Math.random() < 0.01,
    }));

    let animationFrameId: number;
    let lastTime = Date.now();
    let mousePos = { x: -1000, y: -1000, active: false };

    const handleMouseMove = (e: MouseEvent) => {
      mousePos.x = e.clientX;
      mousePos.y = e.clientY;
      mousePos.active = true;
    };

    const handleMouseLeave = () => {
      mousePos.active = false;
    };

    window.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseleave", handleMouseLeave);

    const updateLoop = () => {
      const now = Date.now();
      const dt = now - lastTime;
      lastTime = now;

      const rect = canvas.getBoundingClientRect();
      const isNarrow = window.innerWidth <= 768;

      ctx.fillStyle = "#000000";
      ctx.fillRect(0, 0, TOTAL_WIDTH, TOTAL_HEIGHT);

      for (let i = 0; i < TOTAL_CELLS; i++) {
        const row = Math.floor(i / COLS);
        const col = i % COLS;
        const rectX = col * (DOT_SIZE + GAP);
        const rectY = row * (DOT_SIZE + GAP);
        const dotCenterX = rect.left + rectX + DOT_SIZE / 2;
        const dotCenterY = rect.top + rectY + DOT_SIZE / 2;

        const ds = dotsState[i];

        if (!shouldReduceMotion) {
          ds.elapsed += dt;
          if (ds.elapsed >= ds.duration) {
            ds.elapsed = 0;
            ds.val = ds.target;
            
            // Re-roll noise target (mostly mid-gray, some near-white, some near-black)
            const rand = Math.random();
            if (rand < 0.1) {
              ds.target = 0.8 + Math.random() * 0.2; // 10% near-white
            } else if (rand < 0.3) {
              ds.target = 0.05 + Math.random() * 0.1; // 20% near-black
            } else {
              ds.target = 0.2 + Math.random() * 0.4; // 70% mid-gray
            }
            
            ds.duration = 300 + Math.random() * 500;
            ds.isRed = Math.random() < 0.005; // 0.5% chance to spike red
          }
        }

        const progress = ds.elapsed / ds.duration;
        let currentVal = ds.val + (ds.target - ds.val) * progress;

        // Static fallback for reduced motion
        if (shouldReduceMotion) {
          currentVal = 0.15;
          ds.isRed = false;
        }

        // Mouse proximity boost multiplier (reveals noise, doesn't override it)
        let multiplier = 1.0;
        if (mousePos.active && !isNarrow && !shouldReduceMotion) {
          const mouseDist = Math.hypot(dotCenterX - mousePos.x, dotCenterY - mousePos.y);
          const maxGlowDist = 80;

          if (mouseDist < maxGlowDist) {
            const intensity = 1 - (mouseDist / maxGlowDist);
            multiplier = 1.0 + intensity * 3.0; // Up to 4x brighter
          }
        }

        const finalVal = Math.min(1.0, currentVal * multiplier);

        if (ds.isRed) {
          // #EA3323 (R:234, G:51, B:35)
          const r = Math.round(234 * finalVal);
          const g = Math.round(51 * finalVal);
          const b = Math.round(35 * finalVal);
          ctx.fillStyle = `rgb(${r}, ${g}, ${b})`;
        } else {
          const gray = Math.round(255 * finalVal);
          ctx.fillStyle = `rgb(${gray}, ${gray}, ${gray})`;
        }

        ctx.fillRect(rectX, rectY, DOT_SIZE, DOT_SIZE);
      }

      animationFrameId = requestAnimationFrame(updateLoop);
    };

    animationFrameId = requestAnimationFrame(updateLoop);

    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseleave", handleMouseLeave);
      if (animationFrameId) cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return (
    <div style={{ padding: "1.5rem", margin: "0 auto", width: "fit-content", pointerEvents: "auto" }}>
      <canvas ref={canvasRef} style={{ display: "block" }} />
    </div>
  );
}
