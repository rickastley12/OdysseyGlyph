"use client";

import { motion } from "framer-motion";
import { useMemo } from "react";

// 5x7 dot matrix definitions
const GLYPHS: Record<string, number[][]> = {
  G: [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 0],
    [1, 0, 1, 1, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0],
  ],
  L: [
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 1, 1, 1, 1],
  ],
  Y: [
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 0, 1, 0],
    [0, 0, 1, 0, 0],
    [0, 0, 1, 0, 0],
    [0, 0, 1, 0, 0],
    [0, 0, 1, 0, 0],
  ],
  P: [
    [1, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 1, 1, 1, 0],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
  ],
  H: [
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 1, 1, 1, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
  ],
  O: [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0],
  ],
  D: [
    [1, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 1, 1, 1, 0],
  ],
  S: [
    [0, 1, 1, 1, 1],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [0, 1, 1, 1, 0],
    [0, 0, 0, 0, 1],
    [0, 0, 0, 0, 1],
    [1, 1, 1, 1, 0],
  ],
  E: [
    [1, 1, 1, 1, 1],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 1, 1, 1, 0],
    [1, 0, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 1, 1, 1, 1],
  ],
  " ": [
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
  ]
};

interface DotMatrixTextProps {
  text: string;
  color?: string;
  delayOffset?: number;
}

export default function DotMatrixText({ text, color = "var(--foreground)", delayOffset = 0 }: DotMatrixTextProps) {
  const letters = useMemo(() => text.toUpperCase().split(""), [text]);

  return (
    <div style={{ display: "flex", gap: "clamp(0.3rem, 2vw, 1rem)", flexWrap: "nowrap", justifyContent: "center" }}>
      {letters.map((char, charIdx) => {
        const matrix = GLYPHS[char] || GLYPHS[" "];
        return (
          <div key={charIdx} style={{ display: "grid", gridTemplateColumns: "repeat(5, 1fr)", gap: "clamp(2px, 0.6vw, 6px)" }}>
            {matrix.map((row, r) =>
              row.map((val, c) => {
                const isActive = val === 1;
                // Draw animation delay: letters left to right, dots top to bottom left to right
                const dotDelay = delayOffset + (charIdx * 0.15) + ((r * 5 + c) * 0.005);
                
                return (
                  <motion.div
                    key={`${r}-${c}`}
                    initial={{ opacity: 0, scale: 0 }}
                    animate={{ opacity: isActive ? 1 : 0.08, scale: 1 }}
                    transition={{
                      delay: dotDelay,
                      type: "spring",
                      stiffness: 200,
                      damping: 10
                    }}
                    whileHover={isActive ? { scale: 1.5, filter: "brightness(1.5)", zIndex: 10 } : {}}
                    style={{
                      width: "clamp(4px, 1.2vw, 12px)",
                      height: "clamp(4px, 1.2vw, 12px)",
                      borderRadius: "50%",
                      backgroundColor: color,
                      opacity: isActive ? 1 : 0.08,
                      transformOrigin: "center"
                    }}
                  />
                );
              })
            )}
          </div>
        );
      })}
    </div>
  );
}
