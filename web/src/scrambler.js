// AndRubik scrambler UI.
// - cubing/twisty provides the <twisty-player> 3D cube and the <twisty-alg-viewer>
//   clickable move list (both auto-registered as custom elements on import).
// - cubing/scramble generates WCA random-state 3x3 scrambles (in a Web Worker).
import "cubing/twisty";
import { randomScrambleForEvent } from "cubing/scramble";

const player = document.getElementById("player");
const statusEl = document.getElementById("status");
const regenBtn = document.getElementById("regen");
const playBtn = document.getElementById("play");
const pauseBtn = document.getElementById("pause");
const speedButtons = Array.from(document.querySelectorAll(".speed"));

const DEFAULT_SPEED = 1;

// anchor=start means the player begins on a solved cube and the scramble alg
// animates forward to the scrambled state.
player.experimentalSetupAnchor = "start";
setSpeed(DEFAULT_SPEED);

// Keep the Pause/Resume button icon in sync with the real playing state. The
// animation also stops on its own at the end of the scramble (and when a move in
// the list is clicked), so we mirror the model rather than tracking it ourselves.
// Icon-only (no label) to keep the speed buttons on-screen.
player.experimentalModel.playingInfo.addFreshListener((info) => {
  pauseBtn.textContent = info.playing ? "⏸" : "▶";
});

/** Reset to the solved state and play the current scramble from the beginning. */
function playFromStart() {
  player.pause();        // stop any in-progress animation immediately
  player.jumpToStart();  // reset the view to solved
  player.play();         // animate the scramble
}

/** Stop the animation immediately (or resume from the current position). */
async function togglePause() {
  const info = await player.experimentalModel.playingInfo.get();
  if (info.playing) {
    player.pause();
  } else {
    player.play();
  }
}

function setSpeed(speed) {
  player.tempoScale = speed;
  for (const b of speedButtons) {
    b.classList.toggle("active", Number(b.dataset.speed) === speed);
  }
}

async function newScramble() {
  regenBtn.disabled = true;
  statusEl.textContent = "Generating…";
  try {
    const scramble = await randomScrambleForEvent("333");
    // Interrupt whatever is playing, swap in the new scramble, reset and replay.
    // The <twisty-alg-viewer> re-renders its clickable move list from player.alg.
    player.pause();
    player.alg = scramble;
    const text = scramble.toString();
    statusEl.textContent = "";
    player.jumpToStart();
    player.play();
    // Hand the scramble to the native side so the Timer can attach it to a solve.
    if (window.AndRubikNative && typeof window.AndRubikNative.onScramble === "function") {
      window.AndRubikNative.onScramble(text);
    }
  } catch (err) {
    statusEl.textContent = "Scramble error: " + (err && err.message ? err.message : err);
  } finally {
    regenBtn.disabled = false;
  }
}

playBtn.addEventListener("click", playFromStart);
pauseBtn.addEventListener("click", togglePause);
for (const b of speedButtons) {
  b.addEventListener("click", () => setSpeed(Number(b.dataset.speed)));
}
regenBtn.addEventListener("click", newScramble);

newScramble();
