"use client";

import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";

const lines = [
  "INITIALIZING GLYPH MATRIX...",
  "LOADING FRAME PIPELINE...",
  "BOX_FILTER: ENABLED",
  "S_CURVE_CONTRAST: ACTIVE",
  "READY."
];

interface BootSequenceProps {
  onComplete: () => void;
}

export default function BootSequence({ onComplete }: BootSequenceProps) {
  const [visibleLines, setVisibleLines] = useState<string[]>([]);
  const [isExiting, setIsExiting] = useState(false);

  useEffect(() => {
    let timeoutIds: NodeJS.Timeout[] = [];

    lines.forEach((line, index) => {
      const id = setTimeout(() => {
        setVisibleLines((prev) => [...prev, line]);
      }, 400 + index * 300);
      timeoutIds.push(id);
    });

    const finishId = setTimeout(() => {
      setIsExiting(true);
      setTimeout(onComplete, 500); // Wait for fade out
    }, 400 + lines.length * 300 + 400);
    timeoutIds.push(finishId);

    return () => timeoutIds.forEach(clearTimeout);
  }, [onComplete]);

  return (
    <AnimatePresence>
      {!isExiting && (
        <motion.div
          initial={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.5 }}
          style={{
            position: "fixed",
            inset: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            backgroundColor: "var(--background)",
            zIndex: 9999,
          }}
        >
          <div style={{ width: "100%", maxWidth: "600px", padding: "2rem", fontFamily: "var(--font-mono)", color: "var(--foreground)" }}>
            {visibleLines.map((line, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                style={{ marginBottom: "0.5rem", fontSize: "0.9rem" }}
              >
                <span style={{ color: "var(--accent)", marginRight: "0.5rem" }}>&gt;</span>
                {line}
              </motion.div>
            ))}
            <motion.div
              animate={{ opacity: [1, 0, 1] }}
              transition={{ repeat: Infinity, duration: 0.8 }}
              style={{ display: "inline-block", width: "8px", height: "1rem", backgroundColor: "var(--accent)", marginTop: "0.5rem" }}
            />
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
