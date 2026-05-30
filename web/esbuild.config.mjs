import * as esbuild from "esbuild";
import { rmSync, mkdirSync, copyFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const outdir = resolve(__dirname, "../app/src/main/assets/web");

// Clean output so stale chunks never ship in the APK.
rmSync(outdir, { recursive: true, force: true });
mkdirSync(outdir, { recursive: true });

await esbuild.build({
  entryPoints: [resolve(__dirname, "src/scrambler.js")],
  bundle: true,
  format: "esm",
  // cubing.js requires ES2022 module compatibility.
  target: "es2022",
  // splitting lets esbuild emit cubing.js's Web Worker entry points as
  // separate chunks (scramble/search run off the main thread).
  splitting: true,
  outdir,
  minify: true,
  sourcemap: false,
  // Emit third-party license headers next to the bundle (for attribution).
  legalComments: "external",
  // cubing.js pulls WebAssembly for its solver/scramble search.
  loader: { ".wasm": "copy" },
  assetNames: "[name]-[hash]",
  chunkNames: "[name]-[hash]",
  logLevel: "info",
});

// Static files copied verbatim.
copyFileSync(resolve(__dirname, "src/index.html"), resolve(outdir, "index.html"));
copyFileSync(resolve(__dirname, "src/styles.css"), resolve(outdir, "styles.css"));

console.log("Built web assets ->", outdir);
