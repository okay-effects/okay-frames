# okay-frames — a meaning read by choices (spec, 2026-11-17)

One small library for an interface written four times: okay-inlay's `Domain` (slots, each a choice or a copy of
the text), okay-desk's `Frame` (lines whose options depend on the lines before), okay-invest's `GoalReader`
(slots weighed by a model), okay-shop's commands. The theory is okay-desk's `specs/theory.md` — reading is
refining a term with typed holes; the wider picture okay-inlay's `specs/concept.md`. This repository holds only
what every one of the four needs, pure, with no dependency.

## 1. What a space declares (`Space[D, T]`)

Over some data `D` (a shop's desk, a table, a calendar), terms `T` reached by filling holes in order:

- `next(data, text, chosen)`: the next `Hole` — its name and its FINITE options — or none when nothing is left.
  The options may depend on the choices made and on the text: a name or a number the text itself writes is an
  option, a value it does not write is not. A flat set of independent slots is the case where they depend on
  neither.
- `assemble(data, chosen)`: the term of the choices, or none.
- `path(data, term)`: the term as its choices, in `next`'s order.
- `say(data, term, language)` and `parse(data, words, language)`: the canonical words a person confirms, and
  their inverse.

Derived, final: `close(data, text, chosen)` — the term of choices CHECKED against `next` step by step (the right
hole, an offered option, nothing missing, nothing more). The only way from choices to a term.

## 2. The laws (`Laws`)

Each answers none when the law holds, or why it does not — so any test framework can call them.

- `reach(space)(data, text, term)`: the term's path is offered step by step and closes to it (theory.md L1, L2).
- `roundTrip(space)(data, term, languages)`: `parse(say(t)) == t` (L5).
- `distinct(space)(data, terms, language)`: no two terms say the same.

L3 (the exact engine) and L4 (refutation) are the space's own: this library never computes an answer.

## 3. A reading is a value (`Reading`)

The choices so far and each one's margin. The host takes `hole`, gets a score an option from whatever weighs — a
model, a rule, a person, a test's oracle — and gives them back: `weighed(scores)` takes the best (the first on a
tie) and keeps the margin over the second; `chose(option)` is a word with authority. `forced`: a hole with one
option needs no weighing; `stuck`: a hole with none — the text cannot be read here. `meaning`: the closed term.
`doubtful(bar)`: the holes to ask a person about.

Why a value and not an effect: the one thing that differs between hosts is HOW the scores come — in place on the
JVM, by futures in a browser, as an effect of okay-scala or okay-simple. A value driven step by step serves all
of them with no dependency, and a reading stopped at a hole is resumed from its value (no replay).
`Reading.run` is the loop for a host that scores in place.

## 4. The measure (`Measure`)

Texts with their meant terms and a bar: right, asked (a choice under the bar), unread (no term came out), wrong
(answered and not the meant term), and by hole how often the meant option was chosen — counted only while the
reading is still on the meant path, since after a wrong choice the later holes are other decisions.

## 5. Not here

Weighing by a model (okay-llm's `Read.score`), calibration of the bar, training data from a space, an engine's
answer, asking a person as an effect. Each has a home already; they meet a space through `Reading`.

## 6. The four places

| place | before | on okay-frames |
| --- | --- | --- |
| okay-shop `okay.shop.commands` | `Frames`, `Words` | `Commands extends Space[Desk, Command]` — done |
| okay-inlay `okay.inlay.domain` | `Domain`, `Kit` | a `Space` of a `Domain`'s slots; `Kit` → `Reading`, `Measure` |
| okay-invest `GoalReader` | slots over `Read.score` | a `Space[Unit, Goal]`; its reader drives a `Reading` |
| okay-desk `Frame`, `FrameSay`, `FrameReading` | lines by `Frame.next` | a `Space[Tables, Query]` under a `Reading` |

State 2026-11-17: the library released (0.1.0, https://okay-effects.github.io/okay-frames/maven) and
okay-shop on it. The other three have other sessions' work in progress; each moves
when its tree is free, with its own tests unchanged as the proof.
