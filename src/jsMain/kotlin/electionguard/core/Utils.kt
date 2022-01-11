package electionguard.core

// complicated browser-vs-node detection logic from here:
// https://github.com/flexdinesh/browser-or-node/blob/master/src/index.js

// const isBrowser =
//  typeof window !== "undefined" && typeof window.document !== "undefined";
//
//const isNode =
//  typeof process !== "undefined" &&
//  process.versions != null &&
//  process.versions.node != null;

/** Distinguish if we're in Node.js (true) or maybe in a browser (false). */
fun isNodeJs(): Boolean =
    jsTypeOf(js("process")) != "undefined" &&
            js("process").versions != null &&
            js("process").versions.node != null

/** Distinguish if we're in a browser with crypto built-in (true) or maybe somewhere else (false). */
fun isBrowser(): Boolean =
    jsTypeOf(js("window")) != "undefined" &&
            jsTypeOf(js("window.crypto")) != "undefined"