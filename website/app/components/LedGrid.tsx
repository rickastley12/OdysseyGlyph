"use client";

import { useEffect, useRef } from "react";
import { useReducedMotion } from "framer-motion";

const COLS = 48;
const ROWS = 12;
const DOT_SIZE = 8;
const GAP = 4;
const TOTAL_CELLS = COLS * ROWS;

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
      const dots = dotsRef.current;
      const rect = container.getBoundingClientRect();
      const time = (Date.now() - startTime) / 1000;
      const isNarrow = window.innerWidth <= 768;

      // Scan line moves left to right (speed: 15 columns per sec)
      const scanSpeed = 15;
      const scanCol = (time * scanSpeed) % (COLS + 20) - 10;

      for (let i = 0; i < TOTAL_CELLS; i++) {
        const dot = dots[i];
        if (!dot) continue;

        const row = Math.floor(i / COLS);
        const col = i % COLS;

        const dotX = rect.left + col * (DOT_SIZE + GAP) + DOT_SIZE / 2;
        const dotY = rect.top + row * (DOT_SIZE + GAP) + DOT_SIZE / 2;

        let brightness = 0.15; // Base idle brightness

        if (shouldReduceMotion) {
          brightness = 0.15;
        } else {
          // Add slight ambient flicker to base
          brightness += Math.random() * 0.02;

          // Scan line layer
          const distToScan = scanCol - col;
          if (distToScan >= 0 && distToScan < 12) {
            // Wake trails behind the scan line
            const wakeIntensity = 1 - (distToScan / 12);
            // Peak at ~0.60 brightness, plus some noisy flicker in the wake
            brightness += (0.45 * wakeIntensity) + (Math.random() * 0.05 * wakeIntensity);
          }

          // Mouse hotspot layer (additive)
          if (mousePos.active && !isNarrow) {
            const mouseDist = Math.hypot(dotX - mousePos.x, dotY - mousePos.y);
            const maxGlowDist = 80;

            if (mouseDist < maxGlowDist) {
              const intensity = 1 - (mouseDist / maxGlowDist);
              brightness += intensity * 0.8;
            }
          }
        }

        // Cap at 1.0
        brightness = Math.min(1.0, brightness);
        dot.style.opacity = brightness.toFixed(3);
      }

      animationFrameId = requestAnimationFrame(updateLoop);
    };

    animationFrameId = requestAnimationFrame(updateLoop);

    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseleave", handleMouseLeave);
      if (animationFrameId) cancelAnimationFrame(animationFrameId);
    };
  }, [shouldReduceMotion]);

  return (
    <div
      ref={containerRef}
      style={{
        display: "grid",
        gridTemplateColumns: `repeat(${COLS}, ${DOT_SIZE}px)`,
        gap: `${GAP}px`,
        padding: "1.5rem",
        margin: "0 auto",
        width: "fit-content",
        pointerEvents: "auto", // Ensure mouse events are captured properly
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
