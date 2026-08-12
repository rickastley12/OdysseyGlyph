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
  
  // Red spikes are handled independently of grayscale noise
  isRed: boolean;
  redDuration: number;
  redElapsed: number;
  
  // Spring state for cursor multiplier
  mult: number;
  multVel: number;
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
      isRed: false,
      redDuration: 0,
      redElapsed: 0,
      mult: 1.0,
      multVel: 0,
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

      const dtSec = Math.min(dt / 1000, 0.1); // Clamp dt to prevent explosion on tab resume
      const rect = canvas.getBoundingClientRect();
      const isNarrow = window.innerWidth <= 768;

      ctx.fillStyle = "#000000";
      ctx.fillRect(0, 0, TOTAL_WIDTH, TOTAL_HEIGHT);

      // Critically damped spring configuration for the hover multiplier
      // Response time ~0.4s
      const omega = (2 * Math.PI) / 0.4;
      const k = omega * omega;
      const c = 2 * omega;

      for (let i = 0; i < TOTAL_CELLS; i++) {
        const row = Math.floor(i / COLS);
        const col = i % COLS;
        const rectX = col * (DOT_SIZE + GAP);
        const rectY = row * (DOT_SIZE + GAP);
        const dotCenterX = rect.left + rectX + DOT_SIZE / 2;
        const dotCenterY = rect.top + rectY + DOT_SIZE / 2;

        const ds = dotsState[i];

        if (!shouldReduceMotion) {
          // 1. Process base grayscale noise
          ds.elapsed += dt;
          if (ds.elapsed >= ds.duration) {
            ds.elapsed = 0;
            ds.val = ds.target;
            
            // Re-roll noise target (mostly blacks and whites, bit of grey)
            const rand = Math.random();
            if (rand < 0.45) {
              ds.target = 0.0 + Math.random() * 0.15; // 45% near-black
            } else if (rand < 0.85) {
              ds.target = 0.7 + Math.random() * 0.3; // 40% near-white
            } else {
              ds.target = 0.3 + Math.random() * 0.3; // 15% mid-gray
            }
            
            ds.duration = 300 + Math.random() * 500;
          }

          // 2. Process independent red spikes (2-3% of dots red at any given time)
          if (!ds.isRed) {
            // To maintain ~2.5% red coverage with ~225ms average duration:
            // Rate = 0.025 / 0.225s = ~0.111 spikes per second per dot
            if (Math.random() < 0.111 * dtSec) {
              ds.isRed = true;
              ds.redElapsed = 0;
              ds.redDuration = 150 + Math.random() * 150; // 150-300ms
            }
          } else {
            ds.redElapsed += dt;
            if (ds.redElapsed >= ds.redDuration) {
              ds.isRed = false;
            }
          }
        }

        const progress = ds.elapsed / ds.duration;
        let currentVal = ds.val + (ds.target - ds.val) * progress;

        if (shouldReduceMotion) {
          currentVal = 0.15;
          ds.isRed = false;
        }

        // 3. Process Mouse Interaction Spring
        let targetMult = 1.0;
        if (mousePos.active && !isNarrow && !shouldReduceMotion) {
          const mouseDist = Math.hypot(dotCenterX - mousePos.x, dotCenterY - mousePos.y);
          // Pure boolean radius check, no smooth falloff gradient
          if (mouseDist < 80) {
            targetMult = 2.5; 
          }
        }

        // Apply critically damped spring physics to the multiplier
        const force = -k * (ds.mult - targetMult) - c * ds.multVel;
        ds.multVel += force * dtSec;
        ds.mult += ds.multVel * dtSec;
        
        // Safety clamp on spring
        ds.mult = Math.max(1.0, ds.mult);

        // Apply multiplier and clamp brightness
        const finalVal = Math.min(1.0, currentVal * ds.mult);

        // 4. Draw Dot
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
