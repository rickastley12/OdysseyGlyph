"use client";

import { useEffect, useRef } from "react";
import { useReducedMotion } from "framer-motion";

const COLS = 48;
const ROWS = 12;
const DOT_SIZE = 8;
const GAP = 4;
const TOTAL_WIDTH = COLS * DOT_SIZE + (COLS - 1) * GAP;
const TOTAL_HEIGHT = ROWS * DOT_SIZE + (ROWS - 1) * GAP;

export default function LedGrid() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const shouldReduceMotion = useReducedMotion();

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d", { alpha: false }); // optimize for opaque background if possible, but here we just draw rects
    if (!ctx) return;

    // Handle high-DPI displays for crisp rendering
    const dpr = window.devicePixelRatio || 1;
    canvas.width = TOTAL_WIDTH * dpr;
    canvas.height = TOTAL_HEIGHT * dpr;
    canvas.style.width = `${TOTAL_WIDTH}px`;
    canvas.style.height = `${TOTAL_HEIGHT}px`;
    ctx.scale(dpr, dpr);

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
      const rect = canvas.getBoundingClientRect();
      const time = (Date.now() - startTime) / 1000;
      const isNarrow = window.innerWidth <= 768;

      // Scan line configuration
      const scanSpeed = 15;
      const scanCol = (time * scanSpeed) % (COLS + 20) - 10;

      // Clear the canvas with the background color (matches hero background roughly)
      ctx.fillStyle = "#000000";
      ctx.fillRect(0, 0, TOTAL_WIDTH, TOTAL_HEIGHT);

      for (let row = 0; row < ROWS; row++) {
        for (let col = 0; col < COLS; col++) {
          const rectX = col * (DOT_SIZE + GAP);
          const rectY = row * (DOT_SIZE + GAP);
          const dotCenterX = rect.left + rectX + DOT_SIZE / 2;
          const dotCenterY = rect.top + rectY + DOT_SIZE / 2;

          let brightness = 0.08; // Base idle brightness (8%)

          if (shouldReduceMotion) {
            brightness = 0.08;
          } else {
            // Scan line layer
            const distToScan = scanCol - col;
            if (distToScan >= 0 && distToScan < 12) {
              const wakeIntensity = 1 - (distToScan / 12);
              // Base is 0.08, wake peaks at ~0.20 (+0.12)
              brightness += (0.12 * wakeIntensity);
            }

            // Mouse hotspot layer (additive)
            if (mousePos.active && !isNarrow) {
              const mouseDist = Math.hypot(dotCenterX - mousePos.x, dotCenterY - mousePos.y);
              const maxGlowDist = 100;

              if (mouseDist < maxGlowDist) {
                const intensity = 1 - (mouseDist / maxGlowDist);
                // Add radial brightness boost
                brightness += intensity * 0.8;
              }
            }
          }

          brightness = Math.min(1.0, brightness);
          const colorValue = Math.round(255 * brightness);
          
          ctx.fillStyle = `rgb(${colorValue}, ${colorValue}, ${colorValue})`;
          ctx.fillRect(rectX, rectY, DOT_SIZE, DOT_SIZE);
        }
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
    <div style={{ padding: "1.5rem", margin: "0 auto", width: "fit-content", pointerEvents: "auto" }}>
      <canvas ref={canvasRef} style={{ display: "block" }} />
    </div>
  );
}
