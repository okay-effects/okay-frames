# AGENTS.md — okay-frames

Read `specs/frames.md` first: the spec is written before the code and kept true after it.

- A pure library with no dependency: `Space`, `Laws`, `Reading`, `Measure` in `okay.frames`. Nothing here weighs
  options, computes an answer, trains or asks a person — those live with their owners (okay-llm, each space).
- A change must serve every place that uses it (specs/frames.md §6); a need of one place alone stays in that place.
- Cross-built JVM / Scala.js / Native; `sbt test` runs all three. No recursion on the size of a reading.
- Scala 3.3 (usable from 3.9), lines at most 120 characters, English in code and docs, `-Werror`.
- Releases: RELEASING.md (republished under 0.1.0 while unused). Commits: plain messages, no attribution trailers.
