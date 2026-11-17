# okay-frames

**A meaning read by choices.** A text is understood by filling the holes of a term, each from a finite set of
options computed from the data and from the text itself; nothing outside the options can be chosen. This library
is the small shared part of that: the space of meanings, its laws, a reading as a value, a measure. Pure Scala 3,
no dependency; the JVM, Scala.js and Scala Native.

```scala
resolvers += "okay-frames" at "https://okay-effects.github.io/okay-frames/maven"
libraryDependencies += "io.github.okay-effects" %%% "okay-frames" % "0.1.0"
```

```scala
import okay.frames.*

object Counter extends Space[Menu, Order]:        // what a text can mean over a menu
  def next(menu, text, chosen)   = …             // the next Hole(name, options), or None
  def assemble(menu, chosen)     = …             // the Order of the choices
  def path(menu, order)          = …             // the Order as its choices
  def say(menu, order, language) = …             // "Order: coffee, large, 2 of them."
  def parse(menu, words, language) = …           // …and back

var reading = Reading(Counter, menu, "2 large coffees please")
while !reading.done && !reading.stuck do
  val hole = reading.hole.get
  reading = reading.weighed(scoresFromYourModel(reading, hole))   // or .chose(option): a person's word
reading.meaning                                   // Some(Order.Of("coffee", "large", 2))
reading.doubtful(bar)                             // the holes to ask a person about
```

A space is right when it obeys `Laws` (`reach`, `roundTrip`, `distinct`); `Measure.of` counts a weigher's right,
asked, unread and wrong on texts with their meant terms. Who weighs — a model, a rule, a person — is the host's:
see [specs/frames.md](specs/frames.md).
