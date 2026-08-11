"use client";

import { motion, useScroll, useTransform } from 'framer-motion';
import { useRef } from 'react';
import { ArrowDown, Github, Smartphone, Code, PlaySquare, Music } from 'lucide-react';
import styles from './page.module.css';

export default function Home() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ["start start", "end end"]
  });

  // Parallax for Hero
  const heroY = useTransform(scrollYProgress, [0, 0.2], [0, 150]);
  const heroOpacity = useTransform(scrollYProgress, [0, 0.15], [1, 0]);

  return (
    <main className={styles.main} ref={containerRef}>
      
      {/* 1. Hero Section */}
      <section className={styles.hero}>
        <motion.div 
          className={styles.heroContent}
          style={{ y: heroY, opacity: heroOpacity }}
        >
          <motion.h1 
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: "easeOut" }}
            className={styles.glitchText}
            data-text="ODYSSEY GLYPH"
          >
            ODYSSEY GLYPH
          </motion.h1>
          <motion.p 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.3 }}
            className={styles.heroSub}
          >
            A CUSTOM FIRMWARE-LEVEL ANIMATION ENGINE FOR THE <span className="text-accent">NOTHING PHONE</span> GLYPH INTERFACE.
          </motion.p>

          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.6 }}
            className={styles.heroActions}
          >
            <a href="https://github.com/rickastley12/OdysseyGlyph" target="_blank" rel="noreferrer" className="brutalist-btn primary">
              <Github size={24} /> VIEW ON GITHUB
            </a>
            <a href="#vision" className="brutalist-btn">
              EXPLORE
            </a>
          </motion.div>
        </motion.div>

        <motion.div 
          animate={{ y: [0, 10, 0] }} 
          transition={{ repeat: Infinity, duration: 2 }}
          className={styles.scrollIndicator}
        >
          <ArrowDown size={32} />
        </motion.div>
      </section>

      {/* 2. The Vision */}
      <section id="vision" className={styles.section}>
        <div className={styles.container}>
          <div className={styles.grid2}>
            <div>
              <h2 className={styles.sectionTitle}>THE VISION.</h2>
              <p className={styles.sectionBody}>
                Most apps utilize the Nothing Phone's Glyph interface as a glorified notification light. 
                Odyssey changes the paradigm. By reverse-engineering the native <code>Toy Service</code>, 
                we've unlocked the ability to render 25x25 custom frame matrices at 12fps directly to the back of your device.
              </p>
              <br/>
              <p className={styles.sectionBody}>
                It's not just lights. It's a low-res, brutalist canvas.
              </p>
            </div>
            <div className={styles.visionCardWrapper}>
              <div className={`glass-panel ${styles.visionCard}`}>
                <Smartphone size={48} className="text-accent mb-4" />
                <h3>DIRECT MEMORY MAPPING</h3>
                <p>We bypass standard APIs, reading and writing to the firmware buffer to achieve zero-latency strobe syncing.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. Feature Showcases */}
      <section className={styles.featuresSection}>
        <div className={styles.container}>
          
          <motion.div 
            initial={{ opacity: 0, x: -50 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            className={styles.featureRow}
          >
            <div className={styles.featureIcon}><PlaySquare size={48} /></div>
            <div className={styles.featureText}>
              <h2>MEDIA TO MATRIX</h2>
              <p>Pan, crop, and threshold any video or image into a 1-bit dot matrix animation. Save directly to your native Nothing Settings.</p>
            </div>
          </motion.div>

          <motion.div 
            initial={{ opacity: 0, x: 50 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            className={`${styles.featureRow} ${styles.featureRowReverse}`}
          >
            <div className={styles.featureIcon}><Music size={48} /></div>
            <div className={styles.featureText}>
              <h2>LIVE LYRICS</h2>
              <p>Intercepts your local Spotify/YouTube Music playback, downloads synchronized LRC files, and streams the words across your phone's back instantly.</p>
            </div>
          </motion.div>

        </div>
      </section>

      {/* 4. Under the Hood */}
      <section className={styles.technicalSection}>
        <div className={styles.container}>
          <div className="text-center mb-12">
            <h2 className={styles.sectionTitle}>UNDER THE HOOD.</h2>
            <p className={styles.sectionBody}>Open source. No fluff. Just raw byte manipulation.</p>
          </div>
          
          <div className={`glass-panel ${styles.codeBlock}`}>
            <div className={styles.codeHeader}>
              <Code size={18} /> <span>LiveLyricsToyService.kt</span>
            </div>
            <pre>
              <code>
{`override fun onProcessFrame(frameMap: IntArray) {
    if (!MusicPlaybackState.isPlaying) {
        renderSleepState(frameMap)
        return
    }
    
    val currentPosition = MusicPlaybackState.position + syncOffset
    val currentLine = activeLyrics.find { it.timeMs <= currentPosition }
    
    // Map text to 25x25 matrix
    GlyphFontEngine.renderToBuffer(currentLine.text, frameMap)
}`}
              </code>
            </pre>
          </div>
        </div>
      </section>

      <footer className={styles.footer}>
        <p>BUILT FOR THE NOTHING COMMUNITY.</p>
        <p className={styles.muted}>Odyssey Glyph is an open-source project and is not affiliated with Nothing Technology Limited.</p>
      </footer>
    </main>
  );
}
