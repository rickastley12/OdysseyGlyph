"use client";

import { useEffect, useRef } from "react";
import { useReducedMotion } from "framer-motion";

const GRID_SIZE = 25;
const DOT_SIZE = 8;
const GAP = 4;
const TOTAL_CELLS = GRID_SIZE * GRID_SIZE;

export default function LedGrid() {
  const containerRef = useRef<HTMLDivElement>(null);
  const dotsRef = useRef<(HTMLDivElement | null)[]>([]);
  const shouldReduceMotion = useReducedMotion();

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    let animationFrameId: number;
    let startTime = Date.now();
    let mousePos = { x: -1000, y: -1000, active: false };

    const handlePointerMove = (e: PointerEvent) => {
      mousePos = { x: e.clientX, y: e.clientY, active: true };
    };

    const handlePointerLeave = () => {
      mousePos.active = false;
    };

    window.addEventListener("pointermove", handlePointerMove);
    document.addEventListener("pointerleave", handlePointerLeave);

    const updateLoop = () => {
      const dots = dotsRef.current;
      const rect = container.getBoundingClientRect();
      const time = (Date.now() - startTime) / 1000;
      const isNarrow = window.innerWidth <= 768;

      for (let i = 0; i < TOTAL_CELLS; i++) {
        const dot = dots[i];
        if (!dot) continue;

        const row = Math.floor(i / GRID_SIZE);
        const col = i % GRID_SIZE;

        const dotX = rect.left + col * (DOT_SIZE + GAP) + DOT_SIZE / 2;
        const dotY = rect.top + row * (DOT_SIZE + GAP) + DOT_SIZE / 2;

        let brightness = 0.1;

        if (shouldReduceMotion) {
          brightness = 0.2;
        } else if (mousePos.active) {
          // Interactive flashlight effect when mouse is active
          const dist = Math.hypot(dotX - mousePos.x, dotY - mousePos.y);
          const maxGlowDist = 160;

          if (dist < maxGlowDist) {
            const intensity = 1 - dist / maxGlowDist;
            brightness = 0.1 + intensity * 0.9;
          }
        } else if (isNarrow) {
          // Fallback radial pulse for mobile screen when no mouse active
          const centerX = rect.left + rect.width / 2;
          const centerY = rect.top + rect.height / 2;
          const dist = Math.hypot(dotX - centerX, dotY - centerY);
          const maxDist = Math.hypot(rect.width / 2, rect.height / 2);

          const phase = (dist / maxDist) * Math.PI * 2 - time * 2;
          brightness = 0.1 + Math.max(0, Math.sin(phase)) * 0.4;
        }

        dot.style.opacity = brightness.toFixed(3);
      }

      animationFrameId = requestAnimationFrame(updateLoop);
    };

    animationFrameId = requestAnimationFrame(updateLoop);

    return () => {
      window.removeEventListener("pointermove", handlePointerMove);
      document.removeEventListener("pointerleave", handlePointerLeave);
      if (animationFrameId) cancelAnimationFrame(animationFrameId);
    };
  }, [shouldReduceMotion]);

  return (
    <div
      ref={containerRef}
      style={{
        display: "grid",
        gridTemplateColumns: `repeat(${GRID_SIZE}, ${DOT_SIZE}px)`,
        gap: `${GAP}px`,
        padding: "1.5rem",
        margin: "0 auto",
        width: "fit-content",
        pointerEvents: "none", // Allow mouse events to pass through cleanly
      }}
    >
      {Array.from({ length: TOTAL_CELLS }).map((_, i) => (
        <div
          key={i}
          ref={(el) => {
            dotsRef.current[i] = el;
          }}
          style={{
            width: `${DOT_SIZE}px`,
            height: `${DOT_SIZE}px`,
            backgroundColor: "var(--foreground)",
            opacity: 0.1,
            transition: "opacity 0.08s ease-out",
          }}
        />
      ))}
    </div>
  );
}
