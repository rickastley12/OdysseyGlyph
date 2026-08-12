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
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0, filter: "blur(10px)" }}
          transition={{ duration: 0.5 }}
          style={{
            position: "absolute",
            inset: 0,
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            alignItems: "center",
            backgroundColor: "var(--background)",
            color: "var(--foreground)",
            fontFamily: "var(--font-mono)",
            fontSize: "1.2rem",
            zIndex: 50,
          }}
        >
          <div style={{ textAlign: "left", width: "320px" }}>
            {visibleLines.map((line, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, y: 5 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.1 }}
              >
                &gt; {line}
              </motion.div>
            ))}
            <motion.div
              animate={{ opacity: [1, 0] }}
              transition={{ repeat: Infinity, duration: 0.8, ease: "linear" }}
              style={{ display: "inline-block", width: "10px", height: "1.2rem", backgroundColor: "var(--foreground)", marginTop: "4px" }}
            />
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
