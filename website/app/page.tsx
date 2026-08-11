"use client";

import { motion } from "framer-motion";
import styles from "./page.module.css";
import { useEffect, useState } from "react";

// Apple-style spring configs
const springDefault = { type: "spring" as const, bounce: 0, duration: 0.8 };
const springBouncy = { type: "spring" as const, bounce: 0.2, duration: 0.6 };

const titleText = "GLYPH ODYSSEY";

export default function Home() {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) return null;

  return (
    <main className={styles.main}>
      <nav className={styles.nav}>
        <div className={styles.navLogo}>GLYPH ODYSSEY</div>
        <div>v1.0</div>
      </nav>

      <section className={styles.hero}>
        <h1 className={styles.title}>
          {titleText.split(" ").map((word, wordIndex) => (
            <span key={wordIndex} className={styles.word}>
              {word.split("").map((letter, letterIndex) => (
                <motion.span
                  key={letterIndex}
                  className={styles.letter}
                  initial={{ y: "100%" }}
                  animate={{ y: 0 }}
                  transition={{
                    ...springDefault,
                    delay: wordIndex * 0.1 + letterIndex * 0.05,
                  }}
                >
                  {letter}
                </motion.span>
              ))}
              &nbsp;
            </span>
          ))}
        </h1>

        <motion.p
          className={styles.subtitle}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springDefault, delay: 1 }}
        >
          Unleash the Glyph. True audio-visual synergy powered by a custom animation engine.
        </motion.p>

        <motion.button
          className={styles.downloadBtn}
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ ...springDefault, delay: 1.2 }}
          whileTap={{ scale: 0.95 }}
          whileHover={{ scale: 1.05 }}
        >
          Download APK
        </motion.button>
      </section>

      <section className={styles.features}>
        {/* Feature 1 */}
        <div className={styles.featureCard}>
          <div className={styles.featureContent}>
            <motion.h2
              className={styles.featureTitle}
              initial={{ opacity: 0, x: -50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={springDefault}
            >
              LYRIC STUDIO
            </motion.h2>
            <motion.p
              className={styles.featureDesc}
              initial={{ opacity: 0, x: -50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ ...springDefault, delay: 0.1 }}
            >
              Author precise, frame-by-frame glyph animations synced perfectly to your favorite .lrc files. Export standalone .ogg toys directly to the Nothing ecosystem.
            </motion.p>
          </div>
          <motion.div
            className={styles.visualPlaceholder}
            initial={{ opacity: 0, scale: 0.9 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={springBouncy}
          >
            <div className={styles.glyphGrid}>
              {Array.from({ length: 25 }).map((_, i) => (
                <motion.div
                  key={i}
                  className={`${styles.glyphDot} ${i % 3 === 0 ? styles.active : ""}`}
                  animate={{ opacity: [0.3, 1, 0.3] }}
                  transition={{
                    duration: 2,
                    repeat: Infinity,
                    delay: i * 0.1,
                  }}
                />
              ))}
            </div>
          </motion.div>
        </div>

        {/* Feature 2 */}
        <div className={`${styles.featureCard} ${styles.reverse}`}>
          <div className={styles.featureContent}>
            <motion.h2
              className={styles.featureTitle}
              initial={{ opacity: 0, x: 50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={springDefault}
            >
              LIVE LYRICS
            </motion.h2>
            <motion.p
              className={styles.featureDesc}
              initial={{ opacity: 0, x: 50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ ...springDefault, delay: 0.1 }}
            >
              An intelligent background service that intercepts playing media and renders real-time scrolling typography across the Glyph Matrix on your phone's back glass.
            </motion.p>
          </div>
          <motion.div
            className={styles.visualPlaceholder}
            initial={{ opacity: 0, scale: 0.9 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={springBouncy}
          >
            {/* Visual mock for live lyrics */}
            <motion.div
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: "4rem",
                color: "var(--foreground)",
                whiteSpace: "nowrap",
              }}
              animate={{ x: ["100%", "-100%"] }}
              transition={{ duration: 5, repeat: Infinity, ease: "linear" }}
            >
              NEVER GONNA GIVE YOU UP
            </motion.div>
          </motion.div>
        </div>

        {/* Feature 3 */}
        <div className={styles.featureCard}>
          <div className={styles.featureContent}>
            <motion.h2
              className={styles.featureTitle}
              initial={{ opacity: 0, x: -50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={springDefault}
            >
              TOY MANAGER
            </motion.h2>
            <motion.p
              className={styles.featureDesc}
              initial={{ opacity: 0, x: -50 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ ...springDefault, delay: 0.1 }}
            >
              Seamless integration with the Nothing OS third-party ecosystem. Manage, preview, and apply your custom glyph compositions natively.
            </motion.p>
          </div>
          <motion.div
            className={styles.visualPlaceholder}
            initial={{ opacity: 0, scale: 0.9 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={springBouncy}
            whileHover={{ scale: 1.05, rotate: 2 }}
          >
            <div style={{ fontFamily: "var(--font-mono)", fontSize: "2rem" }}>
              {"{ OGG }"}
            </div>
          </motion.div>
        </div>
      </section>

      <footer className={styles.footer}>
        <p>Built for the Nothing ecosystem. Not affiliated with Nothing Technology Limited.</p>
      </footer>
    </main>
  );
}
