/**
 * CallWaiter Promotional Video — Master Timeline & Animation Engine
 * Controls millisecond-accurate sequencing, device UI animations, and video export.
 */

class VideoTimeline {
  constructor() {
    this.totalDuration = 28.0; // 28 seconds
    this.currentTime = 0.0;
    this.isPlaying = false;
    this.lastTimestamp = null;
    this.animationFrameId = null;

    // Track triggered SFX cues to avoid re-triggering during regular playback
    this.triggeredCues = new Set();

    // DOM Elements
    this.dom = {
      viewport: document.getElementById('video-viewport'),
      scenes: [
        document.getElementById('scene-1'),
        document.getElementById('scene-2'),
        document.getElementById('scene-3'),
        document.getElementById('scene-4'),
        document.getElementById('scene-5')
      ],
      hudScene: document.getElementById('hud-scene-indicator'),
      hudTimecode: document.getElementById('hud-timecode'),
      playBtn: document.getElementById('play-pause-btn'),
      playIcon: document.getElementById('play-icon'),
      replayBtn: document.getElementById('replay-btn'),
      currentTimeDisplay: document.getElementById('ctrl-current-time'),
      totalTimeDisplay: document.getElementById('ctrl-total-time'),
      scrubberContainer: document.getElementById('scrubber-container'),
      scrubberProgress: document.getElementById('scrubber-progress'),
      scrubberHandle: document.getElementById('scrubber-handle'),
      scenePills: document.querySelectorAll('.scene-pill-btn'),
      audioBtn: document.getElementById('toggle-audio-btn'),
      audioIcon: document.getElementById('audio-icon'),
      audioLabel: document.getElementById('audio-label'),
      exportBtn: document.getElementById('export-video-btn'),
      exportLabel: document.getElementById('export-label'),
      fullscreenBtn: document.getElementById('fullscreen-btn')
    };

    // Specific Scene UI elements for animated progression
    this.ui = {
      // Scene 2
      s2Reticle: document.getElementById('qr-scanner-reticle'),
      s2Pill: document.getElementById('scan-pill'),
      // Scene 3
      s3Phone: document.getElementById('phone-mockup'),
      foodCard1: document.getElementById('food-card-1'),
      btnAddFood: document.getElementById('btn-add-food'),
      stepReceived: document.getElementById('step-received'),
      line1: document.getElementById('line-1'),
      stepPreparing: document.getElementById('step-preparing'),
      line2: document.getElementById('line-2'),
      stepReady: document.getElementById('step-ready'),
      line3: document.getElementById('line-3'),
      stepServed: document.getElementById('step-served'),
      pillMenu: document.getElementById('pill-menu'),
      pillOrder: document.getElementById('pill-order'),
      pillTrack: document.getElementById('pill-track'),
      pillWaiter: document.getElementById('pill-waiter'),
      // Scene 4
      s4Console: document.getElementById('dashboard-console'),
      t14: document.getElementById('t-table-14'),
      t7: document.getElementById('t-table-7'),
      t9: document.getElementById('t-table-9'),
      t9Badge: document.getElementById('t9-badge'),
      t9Sub: document.getElementById('t9-sub'),
      streamOrder: document.getElementById('stream-order-card'),
      streamWaiter: document.getElementById('stream-waiter-card'),
      streamBill: document.getElementById('stream-bill-card'),
      tagTurned: document.getElementById('tag-turned'),
      btnRespond: document.getElementById('btn-respond'),
      // Scene 5
      s5Hero: document.getElementById('s5-hero'),
      s5MainTitle: document.getElementById('s5-main-title'),
      s5FeatureStrip: document.getElementById('s5-feature-strip'),
      s5CtaBox: document.getElementById('s5-cta-box')
    };

    this.isRecording = false;
    this.mediaRecorder = null;
    this.recordedChunks = [];
  }

  init() {
    this.bindEvents();
    this.seekTo(0);
  }

  bindEvents() {
    this.dom.playBtn.addEventListener('click', () => this.togglePlay());
    this.dom.replayBtn.addEventListener('click', () => {
      this.seekTo(0);
      if (!this.isPlaying) this.play();
    });

    // Scrubber click & drag
    let isDragging = false;
    const handleScrub = (e) => {
      const rect = this.dom.scrubberContainer.getBoundingClientRect();
      const clickX = Math.max(0, Math.min(e.clientX - rect.left, rect.width));
      const percentage = clickX / rect.width;
      this.seekTo(percentage * this.totalDuration);
    };

    this.dom.scrubberContainer.addEventListener('mousedown', (e) => {
      isDragging = true;
      handleScrub(e);
    });

    window.addEventListener('mousemove', (e) => {
      if (isDragging) handleScrub(e);
    });

    window.addEventListener('mouseup', () => {
      isDragging = false;
    });

    // Scene marker jumps
    document.querySelectorAll('.scene-marker').forEach(marker => {
      marker.addEventListener('click', (e) => {
        e.stopPropagation();
        const time = parseFloat(marker.getAttribute('data-time'));
        this.seekTo(time);
      });
    });

    // Scene pill buttons
    this.dom.scenePills.forEach(pill => {
      pill.addEventListener('click', () => {
        const time = parseFloat(pill.getAttribute('data-time'));
        this.seekTo(time);
      });
    });

    // Audio toggle
    this.dom.audioBtn.addEventListener('click', () => {
      const isMuted = window.audioEngine.toggleMute();
      this.dom.audioIcon.textContent = isMuted ? '🔇' : '🔊';
      this.dom.audioLabel.textContent = isMuted ? 'Sound Off' : 'Sound On';
    });

    // Fullscreen
    this.dom.fullscreenBtn.addEventListener('click', () => {
      if (!document.fullscreenElement) {
        document.getElementById('video-container').requestFullscreen().catch(() => {});
      } else {
        document.exitFullscreen().catch(() => {});
      }
    });

    // Video recording export
    this.dom.exportBtn.addEventListener('click', () => this.toggleExport());
  }

  togglePlay() {
    if (this.isPlaying) {
      this.pause();
    } else {
      this.play();
    }
  }

  play() {
    if (this.currentTime >= this.totalDuration) {
      this.seekTo(0);
    }
    this.isPlaying = true;
    this.dom.playIcon.textContent = '❚❚';
    this.lastTimestamp = performance.now();
    window.audioEngine.startMusic();
    this.loop();
  }

  pause() {
    this.isPlaying = false;
    this.dom.playIcon.textContent = '▶';
    window.audioEngine.stopMusic();
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
  }

  loop(timestamp = performance.now()) {
    if (!this.isPlaying) return;

    const delta = (timestamp - this.lastTimestamp) / 1000;
    this.lastTimestamp = timestamp;

    this.currentTime += delta;

    if (this.currentTime >= this.totalDuration) {
      this.currentTime = this.totalDuration;
      this.renderFrame(this.currentTime);
      this.pause();
      if (this.isRecording) {
        this.stopRecording();
      }
      return;
    }

    this.renderFrame(this.currentTime);
    this.animationFrameId = requestAnimationFrame((ts) => this.loop(ts));
  }

  seekTo(seconds) {
    this.currentTime = Math.max(0, Math.min(seconds, this.totalDuration));
    // Clear SFX cues that occur after the sought time
    this.triggeredCues.forEach(cue => {
      if (cue > this.currentTime) {
        this.triggeredCues.delete(cue);
      }
    });
    this.renderFrame(this.currentTime);
  }

  triggerCue(cueId, action) {
    if (!this.triggeredCues.has(cueId)) {
      this.triggeredCues.add(cueId);
      action();
    }
  }

  renderFrame(t) {
    // 1. Update Scrubber & HUD
    const progressPct = (t / this.totalDuration) * 100;
    this.dom.scrubberProgress.style.width = `${progressPct}%`;
    this.dom.scrubberHandle.style.left = `${progressPct}%`;

    const mins = Math.floor(t / 60);
    const secs = Math.floor(t % 60);
    const ms = Math.floor((t % 1) * 100);
    const timeFormatted = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
    this.dom.currentTimeDisplay.textContent = timeFormatted;
    this.dom.hudTimecode.textContent = `${timeFormatted}:${String(ms).padStart(2, '0')} / 00:28:00`;

    // 2. Determine Active Scene
    let activeSceneIndex = 0;
    if (t < 4.0) {
      activeSceneIndex = 0;
      this.dom.hudScene.textContent = 'SCENE 1: THE PROBLEM (0-4s)';
    } else if (t < 8.0) {
      activeSceneIndex = 1;
      this.dom.hudScene.textContent = 'SCENE 2: THE SOLUTION (4-8s)';
    } else if (t < 14.0) {
      activeSceneIndex = 2;
      this.dom.hudScene.textContent = 'SCENE 3: GUEST EXPERIENCE (8-14s)';
    } else if (t < 20.0) {
      activeSceneIndex = 3;
      this.dom.hudScene.textContent = 'SCENE 4: STAFF CONSOLE (14-20s)';
    } else {
      activeSceneIndex = 4;
      this.dom.hudScene.textContent = 'SCENE 5: SMARTER TABLE CTA (20-28s)';
    }

    this.dom.scenes.forEach((sc, idx) => {
      sc.classList.toggle('active', idx === activeSceneIndex);
    });

    this.dom.scenePills.forEach((pill, idx) => {
      pill.classList.toggle('active', idx === activeSceneIndex);
    });

    // 3. Scene 1 Animation & SFX (0 - 4s)
    if (t < 4.0) {
      const bg1 = document.getElementById('bg-scene-1');
      if (bg1) {
        const p = t / 4.0;
        bg1.style.transform = `scale(${1 + p * 0.08}) translate(${p * -1}%, ${p * -0.5}%)`;
      }
      if (t >= 3.6) {
        this.triggerCue('whoosh_1', () => window.audioEngine.playWhoosh());
      }
    }

    // 4. Scene 2 Animation & SFX (4 - 8s)
    if (t >= 4.0 && t < 8.0) {
      const p = (t - 4.0) / 4.0;
      const bg2 = document.getElementById('bg-scene-2');
      if (bg2) {
        bg2.style.transform = `scale(${1.02 + p * 0.06}) translateY(${p * -1}%)`;
      }

      // Scanner lock-on sound & detection pulse
      if (t >= 5.2) {
        this.triggerCue('scan_beep', () => window.audioEngine.playScannerBeep());
        if (this.ui.s2Pill) this.ui.s2Pill.style.opacity = '1';
      } else {
        if (this.ui.s2Pill) this.ui.s2Pill.style.opacity = '0';
      }

      if (t >= 7.6) {
        this.triggerCue('whoosh_2', () => window.audioEngine.playWhoosh());
      }
    }

    // 5. Scene 3 Animation & SFX (8 - 14s)
    if (t >= 8.0 && t < 14.0) {
      const p = (t - 8.0) / 6.0;
      const bg3 = document.getElementById('bg-scene-3');
      if (bg3) {
        bg3.style.transform = `scale(${1.05 + p * 0.05})`;
      }

      // 9.2s: Add Food Item to order
      if (t >= 9.2) {
        this.triggerCue('item_added', () => window.audioEngine.playItemAddPop());
        if (this.ui.btnAddFood) {
          this.ui.btnAddFood.style.background = '#10b981';
          this.ui.btnAddFood.innerHTML = '✓ Added to Cart';
        }
        if (this.ui.pillOrder) this.ui.pillOrder.classList.add('active');
      }

      // 10.0s: Order placed & sent
      if (t >= 10.0) {
        this.triggerCue('order_sent', () => window.audioEngine.playOrderSentChime());
        if (this.ui.pillTrack) this.ui.pillTrack.classList.add('active');
      }

      // 10.4s: Status Step 1 "Received"
      const isReceived = t >= 10.2;
      this.ui.stepReceived.classList.toggle('active', isReceived);

      // 11.2s: Status Step 2 "Preparing"
      const isPreparing = t >= 11.2;
      if (isPreparing) {
        this.triggerCue('step_prep', () => window.audioEngine.playStatusTick());
      }
      this.ui.line1.classList.toggle('active', isPreparing);
      this.ui.stepPreparing.classList.toggle('active', isPreparing);
      this.ui.stepPreparing.classList.toggle('current', isPreparing && t < 12.4);

      // 12.4s: Status Step 3 "Ready"
      const isReady = t >= 12.4;
      if (isReady) {
        this.triggerCue('step_ready', () => window.audioEngine.playStatusTick());
      }
      this.ui.line2.classList.toggle('active', isReady);
      this.ui.stepReady.classList.toggle('active', isReady);
      this.ui.stepReady.classList.toggle('current', isReady && t < 13.5);

      // 13.5s: Status Step 4 "Served"
      const isServed = t >= 13.5;
      if (isServed) {
        this.triggerCue('step_served', () => window.audioEngine.playStatusTick());
      }
      this.ui.line3.classList.toggle('active', isServed);
      this.ui.stepServed.classList.toggle('active', isServed);

      if (t >= 13.7) {
        this.triggerCue('whoosh_3', () => window.audioEngine.playWhoosh());
      }
    }

    // 6. Scene 4 Animation & SFX (14 - 20s)
    if (t >= 14.0 && t < 20.0) {
      const p = (t - 14.0) / 6.0;
      const bg4 = document.getElementById('bg-scene-4');
      if (bg4) {
        bg4.style.transform = `scale(${1.02 + p * 0.06}) translateX(${p * 1}%)`;
      }

      // 15.0s: Incoming Order Alert
      if (t >= 15.0) {
        this.triggerCue('dash_order_alert', () => window.audioEngine.playDashboardAlert());
        if (this.ui.streamOrder) {
          this.ui.streamOrder.style.borderColor = 'rgba(255, 90, 54, 0.7)';
          this.ui.streamOrder.style.boxShadow = '0 0 20px rgba(255, 90, 54, 0.3)';
        }
      }

      // 16.5s: Waiter Request Alert & Response
      if (t >= 16.5) {
        this.triggerCue('waiter_call_alert', () => window.audioEngine.playDashboardAlert());
        if (this.ui.btnRespond) {
          this.ui.btnRespond.style.background = '#10b981';
          this.ui.btnRespond.style.borderColor = '#10b981';
          this.ui.btnRespond.style.color = '#fff';
          this.ui.btnRespond.textContent = '✓ Responding (Marco)';
        }
      }

      // 18.2s: Table 09 changes from Occupied -> Available
      if (t >= 18.2) {
        this.triggerCue('table_turned', () => window.audioEngine.playTableTurnSound());
        if (this.ui.t9Badge) {
          this.ui.t9Badge.textContent = 'Available';
          this.ui.t9Badge.style.color = '#10b981';
        }
        if (this.ui.t9Sub) {
          this.ui.t9Sub.textContent = 'Turned in 42s • Clean & Ready';
          this.ui.t9Sub.style.color = '#34d399';
        }
        if (this.ui.tagTurned) {
          this.ui.tagTurned.style.background = 'rgba(16, 185, 129, 0.3)';
          this.ui.tagTurned.style.boxShadow = '0 0 16px rgba(16, 185, 129, 0.4)';
        }
      }

      if (t >= 19.6) {
        this.triggerCue('whoosh_4', () => window.audioEngine.playWhoosh());
      }
    }

    // 7. Scene 5 Animation & SFX (20 - 28s)
    if (t >= 20.0) {
      const p = (t - 20.0) / 8.0;
      const bg5 = document.getElementById('bg-scene-5');
      if (bg5) {
        bg5.style.transform = `scale(${1.0 + p * 0.08})`;
      }

      // 21.0s: Resonant Final Chord
      if (t >= 21.0) {
        this.triggerCue('final_chord', () => window.audioEngine.playClosingChord());
      }

      // Sequential entrance
      if (this.ui.s5Hero) {
        this.ui.s5Hero.style.opacity = Math.min(1, (t - 20.0) / 1.0);
        this.ui.s5Hero.style.transform = `translateY(${Math.max(0, (1 - (t - 20.0) / 1.0) * 20)}px)`;
      }
    }
  }

  // ================= VIDEO EXPORT / RECORDING =================
  async toggleExport() {
    if (this.isRecording) {
      this.stopRecording();
      return;
    }
    this.startRecording();
  }

  async startRecording() {
    this.isRecording = true;
    this.dom.exportLabel.textContent = 'Recording (0-28s)...';
    this.dom.exportBtn.classList.add('recording');

    // Seek to beginning and play
    this.seekTo(0);
    this.play();

    try {
      const stream = this.dom.viewport.captureStream ? this.dom.viewport.captureStream(60) : null;
      if (!stream) {
        alert('Browser does not support direct DOM stream capture. You can record using the browser subagent!');
        this.isRecording = false;
        this.dom.exportLabel.textContent = 'Export Video (HD)';
        return;
      }

      // If audio engine has destination, capture audio track
      if (window.audioEngine.masterGain && window.audioEngine.ctx) {
        const dest = window.audioEngine.ctx.createMediaStreamDestination();
        window.audioEngine.masterGain.connect(dest);
        dest.stream.getAudioTracks().forEach(track => stream.addTrack(track));
      }

      this.recordedChunks = [];
      const mime = MediaRecorder.isTypeSupported('video/webm;codecs=vp9,opus') 
        ? 'video/webm;codecs=vp9,opus' 
        : 'video/webm';

      this.mediaRecorder = new MediaRecorder(stream, { mimeType: mime, videoBitsPerSecond: 6000000 });
      this.mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) this.recordedChunks.push(e.data);
      };

      this.mediaRecorder.onstop = () => {
        const blob = new Blob(this.recordedChunks, { type: 'video/webm' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'CallWaiter_Promotional_Video_HD.webm';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);

        this.dom.exportLabel.textContent = 'Export Video (HD)';
        this.dom.exportBtn.classList.remove('recording');
        this.isRecording = false;
      };

      this.mediaRecorder.start();
    } catch (err) {
      console.warn('Recording error or permissions:', err);
      this.dom.exportLabel.textContent = 'Export Video (HD)';
      this.isRecording = false;
    }
  }

  stopRecording() {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop();
    }
    this.isRecording = false;
    this.dom.exportLabel.textContent = 'Export Video (HD)';
    this.dom.exportBtn.classList.remove('recording');
  }
}

// Instantiate and initialize when DOM is ready
window.addEventListener('DOMContentLoaded', () => {
  window.videoTimeline = new VideoTimeline();
  window.videoTimeline.init();
});
