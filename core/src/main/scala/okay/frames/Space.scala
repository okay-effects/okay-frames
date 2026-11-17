package okay.frames

/** ONE DECISION: a hole's name and its options — finite, computed by the space from the data and the text.
 * Nothing outside the options can be chosen */
final case class Hole(name: String, options: Vector[String])

/** a hole filled: one of its options */
final case class Choice(hole: String, option: String)

/**
 * A SPACE OF MEANINGS READ BY CHOICES (specs/frames.md): what a text can mean over some data `D`, as terms `T`
 * reached by filling holes one after another, each from a finite set of options. A space declares four things —
 * the next hole, the term of the choices, the term's canonical words and their parser — and is right when it obeys
 * `Laws`. Whoever chooses (a model's weights, a rule, a person, a test's oracle) is not here: see `Reading`.
 *
 * The options of a hole may depend on the choices already made (a column's values on the column chosen) and on
 * the text (a name or a number the text itself writes is an option; a value it does not write is not).
 */
trait Space[D, T]:
  def name: String

  /** the next hole after the choices made, in order; none when nothing is left to decide */
  def next(data: D, text: String, chosen: Vector[Choice]): Option[Hole]

  /** the term the choices make; none when they make no well-formed one. Called only on choices that `next`
   * offered, in its order — `close` checks that */
  def assemble(data: D, chosen: Vector[Choice]): Option[T]

  /** a term as the choices that make it, in the order `next` asks for them */
  def path(data: D, t: T): Vector[Choice]

  /** the canonical words of a term in a language (`en`, `pl`, …): what a person is shown and confirms */
  def say(data: D, t: T, language: String): String

  /** the term of canonical words; none for any other words */
  def parse(data: D, words: String, language: String): Option[T]

  /** THE TERM OF CHOICES, CHECKED: every choice is of the hole `next` asks for and one of its options, and
   * nothing is left to decide — otherwise none. The only way from choices to a term */
  final def close(data: D, text: String, chosen: Vector[Choice]): Option[T] =
    var i = 0
    var ok = true
    var open = true
    while ok && open do
      next(data, text, chosen.take(i)) match
        case None => open = false; ok = i == chosen.size
        case Some(h) =>
          ok = i < chosen.size && chosen(i).hole == h.name && h.options.contains(chosen(i).option)
          i += 1
    if ok then assemble(data, chosen) else None

/**
 * THE LAWS OF A SPACE, as checks a test calls: each answers none when the law holds, or why it does not.
 * (okay-desk specs/theory.md §3: L1 finite typed options and L2 reachability are `reach`; L5 the round trip is
 * `roundTrip`.)
 */
object Laws:
  /** a term is reachable for a text: its path is offered step by step, and closes to it again */
  def reach[D, T](space: Space[D, T])(data: D, text: String, t: T): Option[String] =
    val path = space.path(data, t)
    val refused = path.indices.iterator.flatMap { i =>
      space.next(data, text, path.take(i)) match
        case None => Some(s"step $i: nothing left to decide, but the path goes on with ${path(i)}")
        case Some(h) if h.name != path(i).hole => Some(s"step $i: the hole is ${h.name}, the path says ${path(i)}")
        case Some(h) if !h.options.contains(path(i).option) => Some(s"step $i: ${path(i)} is not offered")
        case _ => None
    }.nextOption()
    refused.orElse(space.close(data, text, path) match
      case Some(`t`) => None
      case other     => Some(s"the path closes to $other, not to $t"))

  /** the words of a term parse to it, in every language given */
  def roundTrip[D, T](space: Space[D, T])(data: D, t: T, languages: Seq[String]): Option[String] =
    languages.iterator.flatMap { l =>
      val words = space.say(data, t, l)
      space.parse(data, words, l) match
        case Some(`t`) => None
        case other     => Some(s"$l: «$words» parses to $other, not to $t")
    }.nextOption()

  /** no two of the terms say the same in a language */
  def distinct[D, T](space: Space[D, T])(data: D, terms: Seq[T], language: String): Option[String] =
    terms.distinct.groupBy(space.say(data, _, language)).collectFirst {
      case (words, ts) if ts.size > 1 => s"$language: «$words» is said by ${ts.mkString(" and ")}"
    }
