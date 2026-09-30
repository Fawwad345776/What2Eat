/**
 * CallWaiter Promotional Video — Procedural Web Audio Engine
 * Generates an energetic yet sophisticated B2B tech commercial background score
 * and crystal-clear cinematic sound effects (chimes, scans, clicks, whooshes).
 */

class AudioEngine {
  constructor() {
    this.ctx = null;
    this.isMuted = false;
    this.isPlaying = false;
    this.tempo = 116; // BPM
    this.masterGain = null;
    this.musicGain = null;
    this.sfxGain = null;
    this.musicInterval = null;
    this.stepIndex = 0;

    // Harmonic chord progression: Fm9 -> Dbmaj7 -> Bbm9 -> Eb7sus (Sophisticated tech commercial feel)
    this.chords = [
      [174.61, 220.00, 261.63, 329.63, 392.00], // F3, A3, C4, E4, G4
      [138.59, 207.65, 261.63, 311.13, 392.00], // Db3, Ab3, C4, Eb4, G4
      [116.54, 174.61, 220.00, 277.18, 349.23], // Bb2, F3, A3, C#4, F4
      [155.56, 233.08, 277.18, 349.23, 415.30]  // Eb3, Bb3, Db4, F4, Ab4
    ];
    this.bassNotes = [87.31, 69.30, 58.27, 77.78]; // F2, Db2, Bb1, Eb2
  }

  init() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      this.ctx = new AudioCtx();

      this.masterGain = this.ctx.createGain();
      this.masterGain.gain.setValueAtTime(0.7, this.ctx.currentTime);
      this.masterGain.connect(this.ctx.destination);

      this.musicGain = this.ctx.createGain();
      this.musicGain.gain.setValueAtTime(0.5, this.ctx.currentTime);
      this.musicGain.connect(this.masterGain);

      this.sfxGain = this.ctx.createGain();
      this.sfxGain.gain.setValueAtTime(0.8, this.ctx.currentTime);
      this.sfxGain.connect(this.masterGain);
    }

    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  toggleMute() {
    this.isMuted = !this.isMuted;
    if (this.masterGain && this.ctx) {
      this.masterGain.gain.setTargetAtTime(this.isMuted ? 0 : 0.7, this.ctx.currentTime, 0.05);
    }
    return this.isMuted;
  }

  startMusic() {
    this.init();
    if (this.isPlaying) return;
    this.isPlaying = true;
    this.stepIndex = 0;

    const secondsPerBeat = 60 / this.tempo;
    const stepDuration = (secondsPerBeat / 2) * 1000; // 8th note steps

    this.musicInterval = setInterval(() => {
      if (!this.isPlaying) return;
      this.tickStep();
    }, stepDuration);
  }

  stopMusic() {
    this.isPlaying = false;
    if (this.musicInterval) {
      clearInterval(this.musicInterval);
      this.musicInterval = null;
    }
  }

  tickStep() {
    if (!this.ctx || this.isMuted) return;

    const now = this.ctx.currentTime;
    const bar = Math.floor(this.stepIndex / 16) % this.chords.length;
    const stepInBar = this.stepIndex % 16;

    // 1. Kick on beats 0, 4, 8, 12 (4-on-the-floor commercial tech groove)
    if (stepInBar % 4 === 0) {
      this.playKick(now);
    }

    // 2. Hi-hat on off-beats
    if (stepInBar % 2 === 1) {
      this.playHiHat(now, stepInBar % 4 === 2 ? 0.35 : 0.2);
    }

    // 3. Sidechained Pad chord on bar start
    if (stepInBar === 0) {
      this.playWarmChord(now, this.chords[bar]);
    }

    // 4. Bassline
    if (stepInBar % 4 === 0 || stepInBar % 4 === 2) {
      this.playBassNote(now, this.bassNotes[bar]);
    }

    // 5. Melodic pluck accent on certain steps
    if (stepInBar === 6 || stepInBar === 10 || stepInBar === 14) {
      const notes = this.chords[bar];
      const note = notes[Math.floor(Math.random() * notes.length)] * 1.5;
      this.playMelodicPluck(now, note);
    }

    this.stepIndex++;
  }

  playKick(time) {
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.frequency.setValueAtTime(140, time);
    osc.frequency.exponentialRampToValueAtTime(38, time + 0.12);

    gain.gain.setValueAtTime(0.7, time);
    gain.gain.exponentialRampToValueAtTime(0.001, time + 0.14);

    osc.connect(gain);
    gain.connect(this.musicGain);

    osc.start(time);
    osc.stop(time + 0.15);
  }

  playHiHat(time, vol = 0.2) {
    const bufferSize = this.ctx.sampleRate * 0.04;
    const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = (Math.random() * 2 - 1) * 0.5;
    }

    const noise = this.ctx.createBufferSource();
    noise.buffer = buffer;

    const filter = this.ctx.createBiquadFilter();
    filter.type = 'highpass';
    filter.frequency.setValueAtTime(7000, time);

    const gain = this.ctx.createGain();
    gain.gain.setValueAtTime(vol, time);
    gain.gain.exponentialRampToValueAtTime(0.001, time + 0.04);

    noise.connect(filter);
    filter.connect(gain);
    gain.connect(this.musicGain);

    noise.start(time);
  }

  playWarmChord(time, freqs) {
    freqs.forEach(freq => {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      const filter = this.ctx.createBiquadFilter();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, time);

      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(1200, time);

      gain.gain.setValueAtTime(0.01, time);
      gain.gain.linearRampToValueAtTime(0.12, time + 0.15);
      gain.gain.exponentialRampToValueAtTime(0.001, time + 1.8);

      osc.connect(filter);
      filter.connect(gain);
      gain.connect(this.musicGain);

      osc.start(time);
      osc.stop(time + 2.0);
    });
  }

  playBassNote(time, freq) {
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, time);

    gain.gain.setValueAtTime(0.45, time);
    gain.gain.exponentialRampToValueAtTime(0.01, time + 0.35);

    osc.connect(gain);
    gain.connect(this.musicGain);

    osc.start(time);
    osc.stop(time + 0.4);
  }

  playMelodicPluck(time, freq) {
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, time);

    gain.gain.setValueAtTime(0.18, time);
    gain.gain.exponentialRampToValueAtTime(0.001, time + 0.4);

    osc.connect(gain);
    gain.connect(this.musicGain);

    osc.start(time);
    osc.stop(time + 0.45);
  }

  // ================= SPECIFIC CINEMATIC SFX =================

  playScannerBeep() {
    this.init();
    if (this.isMuted) return;
    const now = this.ctx.currentTime;

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(1200, now);
    osc.frequency.exponentialRampToValueAtTime(2400, now + 0.08);

    gain.gain.setValueAtTime(0.4, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.2);

    osc.connect(gain);
    gain.connect(this.sfxGain);

    osc.start(now);
    osc.stop(now + 0.22);
  }

  playWhoosh() {
    this.init();
    if (this.isMuted) return;
    const now = this.ctx.currentTime;

    const bufferSize = this.ctx.sampleRate * 0.3;
    const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = (Math.random() * 2 - 1) * 0.3;
    }

    const noise = this.ctx.createBufferSource();
    noise.buffer = buffer;

    const filter = this.ctx.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.setValueAtTime(400, now);
    filter.frequency.exponentialRampToValueAtTime(2200, now + 0.25);

    const gain = this.ctx.createGain();
    gain.gain.setValueAtTime(0.01, now);
    gain.gain.linearRampToValueAtTime(0.3, now + 0.12);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.3);

    noise.connect(filter);
    filter.connect(gain);
    gain.connect(this.sfxGain);

    noise.start(now);
  }

  playItemAddPop() {
    this.init();
    if (this.isMuted) return;
    const now = this.ctx.currentTime;

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(600, now);
    osc.frequency.exponentialRampToValueAtTime(1100, now + 0.06);

    gain.gain.setValueAtTime(0.3, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.12);

    osc.connect(gain);
    gain.connect(this.sfxGain);

    osc.start(now);
    osc.stop(now + 0.14);
  }

  playOrderSentChime() {
    this.init();
    if (this.isMuted) return;
    const now = this.ctx.currentTime;

    [880, 1318.5].forEach((freq, idx) => {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, now + idx * 0.08);

      gain.gain.setValueAtTime(0.3, now + idx * 0.08);
      gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.08 + 0.35);

      osc.connect(gain);
      gain.connect(this.sfxGain);

      osc.start(now + idx * 0.08);
      osc.stop(now + idx * 0.08 + 0.4);
    });
  }

  playStatusTick() {
    this.init();
    if (this.isMuted) return;
    const now = this.ctx.currentTime;

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(987.77, now); // B5

    gain.gain.setValueAtTime(0.25, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.15);

    osc.connect(gain);
    gain.connect(this.sfxGain);

    osc.start(now);
    osc.stop(now + 0.18);
  }

  playDashboardAlert() {
    this.init();
    if (this.isMuted) return;
    const now = this.ctx.currentTime;

    [523.25, 659.25, 1046.5].forEach((freq, idx) => {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, now + idx * 0.05);

      gain.gain.setValueAtTime(0.2, now + idx * 0.05);
      gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.05 + 0.3);

      osc.connect(gain);
      gain.connect(this.sfxGain);

      osc.start(now + idx * 0.05);
      osc.stop(now + idx * 0.05 + 0.35);
    });
  }

  playTableTurnSound() {
    this.init();
    if (this.isMuted) return;
    const now = this.ctx.currentTime;

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'triangle';
    osc.frequency.setValueAtTime(783.99, now); // G5
    osc.frequency.linearRampToValueAtTime(1046.5, now + 0.1); // C6

    gain.gain.setValueAtTime(0.25, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.3);

    osc.connect(gain);
    gain.connect(this.sfxGain);

    osc.start(now);
    osc.stop(now + 0.32);
  }

  playClosingChord() {
    this.init();
    if (this.isMuted) return;
    const now = this.ctx.currentTime;

    // Resonant triumphant Ab major chord swell
    [207.65, 261.63, 311.13, 415.30, 523.25, 622.25].forEach(freq => {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, now);

      gain.gain.setValueAtTime(0.01, now);
      gain.gain.linearRampToValueAtTime(0.1, now + 0.3);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 3.0);

      osc.connect(gain);
      gain.connect(this.sfxGain);

      osc.start(now);
      osc.stop(now + 3.2);
    });
  }
}

// Global Singleton
window.audioEngine = new AudioEngine();
