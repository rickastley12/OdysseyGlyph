"use client";

import { useEffect, useRef } from "react";

interface Flicker {
  col: number;
  row: number;
  startTime: number;
  duration: number; // 150ms - 300ms
  peakAlpha: number; // 0.7 - 1.0
}

export default function AnimatedBackground() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    const SPACING = 28;
    const DOT_RADIUS = 1.5;
    const BASE_COLOR = "rgb(13, 13, 13)"; // #0d0d0d
    const RED_R = 234;
    const RED_G = 51;
    const RED_B = 35; // #EA3323

    let animationFrameId: number;
    let cols = 0;
    let rows = 0;
    let totalDots = 0;
    let activeFlickers: Map<string, Flicker> = new Map();

    const handleResize = () => {
      const dpr = window.devicePixelRatio || 1;
      const width = window.innerWidth;
      const height = window.innerHeight;

      canvas.width = width * dpr;
      canvas.height = height * dpr;
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;

      ctx.scale(dpr, dpr);

      cols = Math.ceil(width / SPACING) + 1;
      rows = Math.ceil(height / SPACING) + 1;
      totalDots = cols * rows;

      if (prefersReducedMotion) {
        drawStaticGrid(width, height);
      }
    };

    const drawStaticGrid = (width: number, height: number) => {
      ctx.clearRect(0, 0, width, height);
      ctx.fillStyle = BASE_COLOR;

      for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
          const x = c * SPACING;
          const y = r * SPACING;
          ctx.beginPath();
          ctx.arc(x, y, DOT_RADIUS, 0, Math.PI * 2);
          ctx.fill();
        }
      }
    };

    const render = () => {
      const width = window.innerWidth;
      const height = window.innerHeight;
      const now = performance.now();

      ctx.clearRect(0, 0, width, height);

      // Target ~1.5% of total dots active at any time
      const targetActiveCount = Math.max(3, Math.floor(totalDots * 0.015));

      // Spawn new flickers if needed
      while (activeFlickers.size < targetActiveCount) {
        const randomCol = Math.floor(Math.random() * cols);
        const randomRow = Math.floor(Math.random() * rows);
        const key = `${randomCol},${randomRow}`;

        if (!activeFlickers.has(key)) {
          activeFlickers.set(key, {
            col: randomCol,
            row: randomRow,
            startTime: now + Math.random() * 50, // slight stagger
            duration: 150 + Math.random() * 150, // 150ms to 300ms
            peakAlpha: 0.7 + Math.random() * 0.3,
          });
        }
      }

      // Draw all base dots
      ctx.fillStyle = BASE_COLOR;
      for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
          const key = `${c},${r}`;
          if (!activeFlickers.has(key)) {
            const x = c * SPACING;
            const y = r * SPACING;
            ctx.beginPath();
            ctx.arc(x, y, DOT_RADIUS, 0, Math.PI * 2);
            ctx.fill();
          }
        }
      }

      // Draw active flickering dots
      activeFlickers.forEach((flicker, key) => {
        const elapsed = now - flicker.startTime;

        if (elapsed < 0) {
          // Waiting to start - draw base color
          const x = flicker.col * SPACING;
          const y = flicker.row * SPACING;
          ctx.fillStyle = BASE_COLOR;
          ctx.beginPath();
          ctx.arc(x, y, DOT_RADIUS, 0, Math.PI * 2);
          ctx.fill();
          return;
        }

        const progress = elapsed / flicker.duration;

        if (progress >= 1) {
          activeFlickers.delete(key);
          // Draw base dot on cleanup frame
          const x = flicker.col * SPACING;
          const y = flicker.row * SPACING;
          ctx.fillStyle = BASE_COLOR;
          ctx.beginPath();
          ctx.arc(x, y, DOT_RADIUS, 0, Math.PI * 2);
          ctx.fill();
          return;
        }

        // Fast fade-in (first 25%), slow fade-out (remaining 75%)
        let intensity = 0;
        if (progress <= 0.25) {
          intensity = progress / 0.25; // 0 to 1
        } else {
          intensity = (1 - progress) / 0.75; // 1 to 0
        }

        intensity *= flicker.peakAlpha;

        const x = flicker.col * SPACING;
        const y = flicker.row * SPACING;

        // Interpolate between base #0d0d0d (13,13,13) and red #EA3323 (234,51,35)
        const r = Math.round(13 + (RED_R - 13) * intensity);
        const g = Math.round(13 + (RED_G - 13) * intensity);
        const b = Math.round(13 + (RED_B - 13) * intensity);

        ctx.fillStyle = `rgb(${r}, ${g}, ${b})`;
        ctx.beginPath();
        ctx.arc(x, y, DOT_RADIUS + intensity * 0.5, 0, Math.PI * 2); // slightly expand when lit
        ctx.fill();
      });

      animationFrameId = requestAnimationFrame(render);
    };

    window.addEventListener("resize", handleResize);
    handleResize();

    if (!prefersReducedMotion) {
      animationFrameId = requestAnimationFrame(render);
    }

    return () => {
      window.removeEventListener("resize", handleResize);
      if (animationFrameId) {
        cancelAnimationFrame(animationFrameId);
      }
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        width: "100vw",
        height: "100vh",
        zIndex: 0,
        pointerEvents: "none",
      }}
    />
  );
}
