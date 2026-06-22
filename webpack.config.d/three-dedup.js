// Dedupe three.js: our @JsModule("three") binding compiles to require("three") → resolves to the
// CJS build, while three/examples (GLTFLoader) import the ESM build — two copies of three, hence the
// "Multiple instances of Three.js" warning + ~1.2MB of duplicate code. Alias bare `three` imports to
// the absolute path of the single ESM build (bypasses three's exports map, which blocks the subpath).
config.resolve = config.resolve || {};
config.resolve.alias = config.resolve.alias || {};
config.resolve.alias["three$"] = require.resolve("three").replace(/three\.cjs$/, "three.module.js");
