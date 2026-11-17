package okay.frames

/**
 * A READING IN PROGRESS, AS A VALUE: the choices made so far and how sure each was. The host drives it — it takes
 * the `hole`, gets a score for each option from whatever weighs (a model, a rule, a person), and gives the scores
 * back with `weighed`; so the same reading runs in place, over futures in a browser, or inside any effect system,
 * and a reading stopped at a hole is resumed by going on from its value.
 *
 * A margin is the best score less the second; a choice with no rival (one option, a person's word) has an
 * infinite margin. A margin is a number to calibrate against a set of texts, never a probability.
 */
final case class Reading[D, T](space: Space[D, T], data: D, text: String, chosen: Vector[Choice] = Vector.empty,
                               margins: Vector[Double] = Vector.empty):
  /** the decision to make now; none when the reading is complete */
  lazy val hole: Option[Hole] = space.next(data, text, chosen)
  def done: Boolean = hole.isEmpty
  /** a hole with no option at all: the text cannot be read in this space */
  def stuck: Boolean = hole.exists(_.options.isEmpty)
  /** the only option of the hole, when there is one: nothing to weigh */
  def forced: Option[String] = hole.collect { case Hole(_, Vector(only)) => only }

  /** the hole filled by the best of the scores, one a option, in the options' order */
  def weighed(scores: Vector[Double]): Reading[D, T] = hole match
    case Some(h) if h.options.nonEmpty && scores.size == h.options.size =>
      // the first of the best: the options' order breaks a tie
      val best = scores.indices.foldLeft(0)((b, i) => if scores(i) > scores(b) then i else b)
      val second = scores.indices.filter(_ != best).map(scores).maxOption
      val margin = second.fold(Double.PositiveInfinity)(scores(best) - _)
      copy(chosen = chosen :+ Choice(h.name, h.options(best)), margins = margins :+ margin)
    case _ => this

  /** the hole filled by a word with authority (a person, a rule, a test): one of its options, or nothing changes */
  def chose(option: String): Reading[D, T] = hole match
    case Some(h) if h.options.contains(option) =>
      copy(chosen = chosen :+ Choice(h.name, option), margins = margins :+ Double.PositiveInfinity)
    case _ => this

  /** the term read, once complete */
  def meaning: Option[T] = if done then space.close(data, text, chosen) else None
  /** the least sure choice's margin */
  def margin: Double = margins.minOption.getOrElse(Double.PositiveInfinity)
  /** the holes chosen with a margin under the bar: what to ask a person about */
  def doubtful(bar: Double): Vector[String] = chosen.zip(margins).collect { case (c, m) if m < bar => c.hole }

object Reading:
  /** a reading driven in place to its end by a function that scores a hole's options; a forced hole is not
   * weighed; a stuck one ends the reading with no meaning */
  def run[D, T](space: Space[D, T])(data: D, text: String)(weigh: (Reading[D, T], Hole) => Vector[Double])
      : Reading[D, T] =
    var r = Reading(space, data, text)
    while !r.done && !r.stuck do
      r = r.forced.fold(r.weighed(weigh(r, r.hole.get)))(r.chose)
    r

/**
 * THE MEASURE OF A WEIGHER on texts with their meant terms: right, asked (a choice under the bar), unread (no term
 * came out), wrong (answered, and not the meant term — the number every promise rests on), and by hole how often
 * the meant option was chosen.
 */
final case class Measure(texts: Int, right: Int, asked: Int, unread: Int, wrong: Int,
                         byHole: Map[String, (Int, Int)]):
  def said: String =
    val holes = byHole.toVector.sortBy(_._1).map { case (h, (r, n)) => s"$h $r/$n" }.mkString(", ")
    s"$texts texts: right $right, asked $asked, unread $unread, wrong $wrong; by hole: $holes"

object Measure:
  def of[D, T](space: Space[D, T])(data: D, pairs: Seq[(String, T)], bar: Double)(
      weigh: (Reading[D, T], Hole) => Vector[Double]): Measure =
    pairs.foldLeft(Measure(0, 0, 0, 0, 0, Map.empty)) { case (m, (text, meant)) =>
      val r = Reading.run(space)(data, text)(weigh)
      val gold = space.path(data, meant)
      // a hole is counted where the reading and the meant path are still at the same decision
      val same = r.chosen.zip(gold).takeWhile((c, g) => c.hole == g.hole)
      val agreed = same.takeWhile((c, g) => c == g).size
      val counted = same.take(agreed + 1)
      val byHole = counted.foldLeft(m.byHole) { case (acc, (c, g)) =>
        val (right, n) = acc.getOrElse(g.hole, (0, 0))
        acc.updated(g.hole, (right + (if c == g then 1 else 0), n + 1))
      }
      val (right, asked, unread, wrong) =
        if r.margin < bar then (0, 1, 0, 0)
        else r.meaning match
          case None          => (0, 0, 1, 0)
          case Some(`meant`) => (1, 0, 0, 0)
          case _             => (0, 0, 0, 1)
      Measure(m.texts + 1, m.right + right, m.asked + asked, m.unread + unread, m.wrong + wrong, byHole)
    }
