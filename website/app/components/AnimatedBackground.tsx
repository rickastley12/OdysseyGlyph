"use client";

import { useEffect, useRef } from "react";

interface Flicker {
  col: number;
  row: number;
  startTime: number;
  duration: number; // 150ms - 300ms
  peakAlpha: number; // 0.7 - 1.0
}

interface HazePatch {
  cx: number;
  cy: number;
  baseRadius: number;
  baseAlpha: number;
  phase: number;
  speed: number;
}

export default function AnimatedBackground() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const osReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const prefersReducedMotion = process.env.NODE_ENV === "production" ? osReducedMotion : false;

    const SPACING = 28;
    const DOT_RADIUS = 1.5;
    const BASE_GRAY = 13; // #0d0d0d
    const RED_R = 234;
    const RED_G = 51;
    const RED_B = 35; // #EA3323

    let animationFrameId: number;
    let cols = 0;
    let rows = 0;
    let totalDots = 0;
    let activeFlickers: Map<string, Flicker> = new Map();
    let patches: HazePatch[] = [];

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

      // Re-initialize patches relative to new screen size
      patches = [
        { cx: 0.2 + Math.random() * 0.1, cy: 0.3 + Math.random() * 0.2, baseRadius: Math.min(width, height) * 0.4, baseAlpha: 0.05, phase: Math.random() * Math.PI * 2, speed: (Math.PI * 2) / (8 + Math.random() * 4) },
        { cx: 0.7 + Math.random() * 0.1, cy: 0.2 + Math.random() * 0.2, baseRadius: Math.min(width, height) * 0.3, baseAlpha: 0.04, phase: Math.random() * Math.PI * 2, speed: (Math.PI * 2) / (8 + Math.random() * 4) },
        { cx: 0.5 + Math.random() * 0.1, cy: 0.7 + Math.random() * 0.2, baseRadius: Math.min(width, height) * 0.45, baseAlpha: 0.06, phase: Math.random() * Math.PI * 2, speed: (Math.PI * 2) / (8 + Math.random() * 4) },
        { cx: 0.8 + Math.random() * 0.1, cy: 0.8 + Math.random() * 0.1, baseRadius: Math.min(width, height) * 0.35, baseAlpha: 0.05, phase: Math.random() * Math.PI * 2, speed: (Math.PI * 2) / (8 + Math.random() * 4) },
      ];

      if (prefersReducedMotion) {
        drawStaticGrid(width, height);
      }
    };

    const drawStaticGrid = (width: number, height: number) => {
      ctx.clearRect(0, 0, width, height);
      ctx.fillStyle = `rgb(${BASE_GRAY}, ${BASE_GRAY}, ${BASE_GRAY})`;

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

    // Helper to calculate the base color of a dot including the white haze patches
    const getHazeIntensity = (x: number, y: number, timeSec: number, width: number, height: number) => {
      let hazeAlpha = 0;
      for (const p of patches) {
        const px = p.cx * width;
        const py = p.cy * height;
        const dist = Math.hypot(x - px, y - py);
        
        // Breathing radius and intensity
        const breathe = Math.sin(timeSec * p.speed + p.phase); // -1 to 1
        const currentRadius = p.baseRadius + breathe * 30;
        
        if (dist < currentRadius) {
          // Quadratic falloff
          const normalized = dist / currentRadius;
          const falloff = Math.pow(1 - normalized * normalized, 2);
          hazeAlpha += p.baseAlpha * falloff * (1 + breathe * 0.1);
        }
      }
      return Math.min(1, hazeAlpha);
    };

    const render = () => {
      const width = window.innerWidth;
      const height = window.innerHeight;
      const now = performance.now();
      const timeSec = now / 1000;

      ctx.clearRect(0, 0, width, height);

      // Target ~1.5% of total dots active at any time, bounded by total available dots
      const maxPossible = cols * rows;
      const targetActiveCount = Math.min(maxPossible, Math.max(1, Math.floor(totalDots * 0.015)));

      // Spawn new flickers if needed (with failsafe counter to prevent infinite loops)
      let failsafe = 0;
      while (activeFlickers.size < targetActiveCount && failsafe < 50) {
        failsafe++;
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

      // Draw all base dots (with haze)
      for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
          const key = `${c},${r}`;
          if (!activeFlickers.has(key)) {
            const x = c * SPACING;
            const y = r * SPACING;
            
            const haze = getHazeIntensity(x, y, timeSec, width, height);
            const val = Math.round(BASE_GRAY + (255 - BASE_GRAY) * haze);
            
            ctx.fillStyle = `rgb(${val}, ${val}, ${val})`;
            ctx.beginPath();
            ctx.arc(x, y, DOT_RADIUS, 0, Math.PI * 2);
            ctx.fill();
          }
        }
      }

      // Draw active flickering dots
      activeFlickers.forEach((flicker, key) => {
        const x = flicker.col * SPACING;
        const y = flicker.row * SPACING;
        const haze = getHazeIntensity(x, y, timeSec, width, height);
        const baseVal = Math.round(BASE_GRAY + (255 - BASE_GRAY) * haze);

        const elapsed = now - flicker.startTime;

        if (elapsed < 0) {
          ctx.fillStyle = `rgb(${baseVal}, ${baseVal}, ${baseVal})`;
          ctx.beginPath();
          ctx.arc(x, y, DOT_RADIUS, 0, Math.PI * 2);
          ctx.fill();
          return;
        }

        const progress = elapsed / flicker.duration;

        if (progress >= 1) {
          activeFlickers.delete(key);
          ctx.fillStyle = `rgb(${baseVal}, ${baseVal}, ${baseVal})`;
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

        // Mix red over the haze base
        const rVal = Math.round(baseVal + (RED_R - baseVal) * intensity);
        const gVal = Math.round(baseVal + (RED_G - baseVal) * intensity);
        const bVal = Math.round(baseVal + (RED_B - baseVal) * intensity);

        ctx.fillStyle = `rgb(${rVal}, ${gVal}, ${bVal})`;
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
