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

    // Check if mobile/touch device
    const isMobile = window.matchMedia("(max-width: 768px)").matches || ("ontouchstart" in window);

    let animationFrameId: number;
    let startTime = Date.now();

    const updateGrid = (mouseX?: number, mouseY?: number) => {
      const rect = container.getBoundingClientRect();
      const dots = dotsRef.current;

      const time = (Date.now() - startTime) / 1000;

      for (let i = 0; i < TOTAL_CELLS; i++) {
        const dot = dots[i];
        if (!dot) continue;

        const row = Math.floor(i / GRID_SIZE);
        const col = i % GRID_SIZE;

        // Calculate center of the dot relative to the viewport
        const dotX = rect.left + col * (DOT_SIZE + GAP) + DOT_SIZE / 2;
        const dotY = rect.top + row * (DOT_SIZE + GAP) + DOT_SIZE / 2;

        let brightness = 0.1;

        if (shouldReduceMotion) {
           brightness = 0.2;
        } else if (isMobile) {
          // Radial pulse for mobile
          const centerX = rect.left + rect.width / 2;
          const centerY = rect.top + rect.height / 2;
          const dist = Math.hypot(dotX - centerX, dotY - centerY);
          const maxDist = Math.hypot(rect.width / 2, rect.height / 2);
          
          // Sine wave propagating outwards
          const phase = dist / maxDist * Math.PI * 2 - time * 2;
          brightness = 0.1 + Math.max(0, Math.sin(phase)) * 0.4;
        } else if (mouseX !== undefined && mouseY !== undefined) {
          // Flashlight effect for desktop
          const dist = Math.hypot(dotX - mouseX, dotY - mouseY);
          const maxGlowDist = 150; // pixels
          
          if (dist < maxGlowDist) {
            // Falloff
            const intensity = 1 - (dist / maxGlowDist);
            brightness = 0.1 + (intensity * 0.9); 
          }
        }

        dot.style.opacity = brightness.toString();
      }
    };

    if (isMobile || shouldReduceMotion) {
      const loop = () => {
        updateGrid();
        animationFrameId = requestAnimationFrame(loop);
      };
      loop();
    } else {
      const handleMouseMove = (e: MouseEvent) => {
        updateGrid(e.clientX, e.clientY);
      };
      
      const handleMouseLeave = () => {
         // Reset to dim when mouse leaves
         updateGrid(-1000, -1000);
      };

      window.addEventListener("mousemove", handleMouseMove);
      window.addEventListener("mouseleave", handleMouseLeave);
      
      // Initial render
      updateGrid(-1000, -1000);

      return () => {
        window.removeEventListener("mousemove", handleMouseMove);
        window.removeEventListener("mouseleave", handleMouseLeave);
      };
    }

    return () => {
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
        padding: "2rem",
        margin: "0 auto",
        width: "fit-content"
      }}
    >
      {Array.from({ length: TOTAL_CELLS }).map((_, i) => (
        <div
          key={i}
          ref={(el) => { dotsRef.current[i] = el; }}
          style={{
            width: `${DOT_SIZE}px`,
            height: `${DOT_SIZE}px`,
            backgroundColor: "var(--foreground)",
            opacity: 0.1,
            transition: "opacity 0.1s ease-out" // smooth out fast mouse movements slightly
          }}
        />
      ))}
    </div>
  );
}
