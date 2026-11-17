package okay.frames

/** A TOY SPACE for the tests: an order at a counter. The sizes depend on the drink chosen; the count is one of the
 * text's own numbers; `nothing` is the meaning of any other text */
enum Order:
  case Of(drink: String, size: String, count: Int)
  case Nothing

final case class Menu(sizes: Vector[(String, Vector[String])])

object Counter extends Space[Menu, Order]:
  def name = "counter"
  private def numbers(text: String): Vector[String] = text.split("\\D+").toVector.filter(_.nonEmpty).distinct

  def next(menu: Menu, text: String, chosen: Vector[Choice]): Option[Hole] = chosen.map(_.option) match
    case Vector()                 => Some(Hole("drink", menu.sizes.map(_._1) :+ "nothing"))
    case Vector("nothing")        => None
    case Vector(drink)            => Some(Hole("size", menu.sizes.toMap.getOrElse(drink, Vector.empty)))
    case Vector(_, _)             => Some(Hole("count", numbers(text)))
    case _                        => None

  def assemble(menu: Menu, chosen: Vector[Choice]): Option[Order] = chosen.map(_.option) match
    case Vector("nothing")           => Some(Order.Nothing)
    case Vector(drink, size, count)  => count.toIntOption.map(Order.Of(drink, size, _))
    case _                           => None

  def path(menu: Menu, t: Order): Vector[Choice] = t match
    case Order.Nothing => Vector(Choice("drink", "nothing"))
    case Order.Of(d, s, n) => Vector(Choice("drink", d), Choice("size", s), Choice("count", n.toString))

  def say(menu: Menu, t: Order, language: String): String = (t, language) match
    case (Order.Nothing, "pl")      => "Nic."
    case (Order.Nothing, _)         => "Nothing."
    case (Order.Of(d, s, n), "pl")  => s"Zamówienie: $d, $s, sztuk $n."
    case (Order.Of(d, s, n), _)     => s"Order: $d, $s, $n of them."

  def parse(menu: Menu, words: String, language: String): Option[Order] =
    val all = Order.Nothing +: (for (d, sizes) <- menu.sizes; s <- sizes; n <- 1 to 9 yield Order.Of(d, s, n))
    all.find(say(menu, _, language) == words)

class FramesSuite extends munit.FunSuite:
  private val menu = Menu(Vector("tea" -> Vector("cup", "pot"), "coffee" -> Vector("small", "large")))
  private val text = "2 large coffees please"
  private val meant = Order.Of("coffee", "large", 2)
  private val terms = Order.Nothing +: (for (d, ss) <- menu.sizes; s <- ss; n <- 1 to 9 yield Order.Of(d, s, n))

  /** a weigher that knows the meant term: the meant option 1, the others 0 */
  private def oracle(t: Order)(r: Reading[Menu, Order], h: Hole): Vector[Double] =
    val wanted = Counter.path(menu, t).lift(r.chosen.size).map(_.option)
    h.options.map(o => if wanted.contains(o) then 1.0 else 0.0)

  test("close: the path of a term closes to it; the laws hold"):
    assertEquals(Counter.close(menu, text, Counter.path(menu, meant)), Some(meant))
    assertEquals(Laws.reach(Counter)(menu, text, meant), None)
    assertEquals(Laws.reach(Counter)(menu, "anything", Order.Nothing), None)
    terms.foreach(t => assertEquals(Laws.roundTrip(Counter)(menu, t, Seq("en", "pl")), None))
    Seq("en", "pl").foreach(l => assertEquals(Laws.distinct(Counter)(menu, terms, l), None))

  test("close refuses what was not offered, what is missing, what is too much, a wrong order"):
    val path = Counter.path(menu, meant)
    assertEquals(Counter.close(menu, text, path.updated(1, Choice("size", "pot"))), None, "tea's size for coffee")
    assertEquals(Counter.close(menu, text, path.updated(2, Choice("count", "3"))), None, "a number not in the text")
    assertEquals(Counter.close(menu, text, path.dropRight(1)), None)
    assertEquals(Counter.close(menu, text, path :+ Choice("count", "2")), None)
    assertEquals(Counter.close(menu, text, Vector(path(1), path(0), path(2))), None)
    assertEquals(Counter.close(menu, text, Vector(Choice("drink", "juice"))), None)

  test("the laws say why when they fail"):
    assert(Laws.reach(Counter)(menu, "no number here", meant).exists(_.contains("not offered")))
    assert(Laws.reach(Counter)(menu, text, Order.Of("coffee", "pot", 2)).exists(_.contains("not offered")))
    object Mute extends Space[Menu, Order]:
      export Counter.{name, next, assemble, path}
      def say(m: Menu, t: Order, l: String) = "the same"
      def parse(m: Menu, w: String, l: String) = None
    assert(Laws.roundTrip(Mute)(menu, meant, Seq("en")).nonEmpty)
    assert(Laws.distinct(Mute)(menu, terms, "en").nonEmpty)

  test("a reading is a value driven by its host: a hole, the scores, the next reading"):
    val r0 = Reading(Counter, menu, text)
    assertEquals(r0.hole, Some(Hole("drink", Vector("tea", "coffee", "nothing"))))
    val r1 = r0.weighed(Vector(0.1, 0.7, 0.2))
    assertEquals(r1.chosen, Vector(Choice("drink", "coffee")))
    assertEqualsDouble(r1.margin, 0.5, 1e-9)
    assertEquals(r1.hole, Some(Hole("size", Vector("small", "large"))))
    val r2 = r1.chose("large")
    assertEquals(r2.forced, Some("2"), "the text writes one number: nothing to weigh")
    val r3 = r2.chose("2")
    assert(r3.done)
    assertEquals(r3.meaning, Some(meant))
    assertEquals(r3.doubtful(0.6), Vector("drink"))
    assertEquals(r3.doubtful(0.4), Vector.empty)
    assertEquals(r1.meaning, None, "an unfinished reading means nothing yet")

  test("a reading refuses scores of the wrong size and a word that is no option; a tie takes the first"):
    val r0 = Reading(Counter, menu, text)
    assertEquals(r0.weighed(Vector(1.0)), r0)
    assertEquals(r0.chose("juice"), r0)
    val tie = r0.weighed(Vector(0.5, 0.5, 0.1))
    assertEquals(tie.chosen, Vector(Choice("drink", "tea")))
    assertEqualsDouble(tie.margin, 0.0, 1e-9)

  test("run: an oracle reads every term of a text; a text with no number is stuck, with no meaning"):
    for t <- Vector(meant, Order.Of("tea", "pot", 2), Order.Nothing) do
      assertEquals(Reading.run(Counter)(menu, text)(oracle(t)).meaning, Some(t))
    val stuck = Reading.run(Counter)(menu, "a large coffee")(oracle(meant))
    assert(stuck.stuck)
    assertEquals(stuck.meaning, None)

  test("the measure: right, asked under the bar, unread, wrong, and by hole"):
    val pairs = Vector(text -> meant, "3 pots of tea, 3" -> Order.Of("tea", "pot", 3), "hello" -> Order.Nothing,
      "a large coffee" -> meant)
    val exact = Measure.of(Counter)(menu, pairs, 0.5)((r, h) => oracle(pairs.find(_._1 == r.text).get._2)(r, h))
    assertEquals((exact.right, exact.asked, exact.unread, exact.wrong), (3, 0, 1, 0))
    assertEquals(exact.byHole("drink"), (4, 4))
    // a weigher that always takes the first option, sure of itself: tea in a cup — wrong where the text has a
    // number, stuck where it has none
    val first = Measure.of(Counter)(menu, pairs, 0.5)((_, h) => 1.0 +: Vector.fill(h.options.size - 1)(0.0))
    assertEquals((first.right, first.asked, first.unread, first.wrong), (0, 0, 2, 2))
    assertEquals(first.byHole("drink"), (1, 4))
    assertEquals(first.byHole("size"), (0, 1), "counted only where the reading is still on the meant path")
    // the same weigher, unsure of itself: everything is asked
    val unsure = Measure.of(Counter)(menu, pairs, 0.5)((_, h) => 0.3 +: Vector.fill(h.options.size - 1)(0.0))
    assertEquals(unsure.asked, 4)
    assert(exact.said.startsWith("4 texts: right 3, asked 0, unread 1, wrong 0"))
