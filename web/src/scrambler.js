// AndRubik scrambler UI.
// - cubing/twisty provides the <twisty-player> 3D cube web component.
// - cubing/scramble generates WCA random-state 3x3 scrambles (in a Web Worker).
import "cubing/twisty";
import { randomScrambleForEvent } from "cubing/scramble";

const player = document.getElementById("player");
const scrambleEl = document.getElementById("scramble");
const regenBtn = document.getElementById("regen");
const playBtn = document.getElementById("play");
const speedButtons = Array.from(document.querySelectorAll(".speed"));

const DEFAULT_SPEED = 1;

// anchor=start means the player begins on a solved cube and the scramble alg
// animates forward to the scrambled state.
player.experimentalSetupAnchor = "start";
setSpeed(DEFAULT_SPEED);

/** Reset to the solved state and play the current scramble from the beginning. */
function playFromStart() {
  player.pause();        // stop any in-progress animation immediately
  player.jumpToStart();  // reset the view to solved
  player.play();         // animate the scramble
}

function setSpeed(speed) {
  player.tempoScale = speed;
  for (const b of speedButtons) {
    b.classList.toggle("active", Number(b.dataset.speed) === speed);
  }
}

async function newScramble() {
  regenBtn.disabled = true;
  scrambleEl.textContent = "Generating…";
  try {
    const scramble = await randomScrambleForEvent("333");
    // Interrupt whatever is playing, swap in the new scramble, reset and replay.
    player.pause();
    player.alg = scramble;
    const text = scramble.toString();
    scrambleEl.textContent = text;
    player.jumpToStart();
    player.play();
    // Hand the scramble to the native side so the Timer can attach it to a solve.
    if (window.AndRubikNative && typeof window.AndRubikNative.onScramble === "function") {
      window.AndRubikNative.onScramble(text);
    }
  } catch (err) {
    scrambleEl.textContent = "Scramble error: " + (err && err.message ? err.message : err);
  } finally {
    regenBtn.disabled = false;
  }
}

playBtn.addEventListener("click", playFromStart);
for (const b of speedButtons) {
  b.addEventListener("click", () => setSpeed(Number(b.dataset.speed)));
}
regenBtn.addEventListener("click", newScramble);

newScramble();
