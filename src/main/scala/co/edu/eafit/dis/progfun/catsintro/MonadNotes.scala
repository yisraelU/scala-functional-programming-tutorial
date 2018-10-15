package co.edu.eafit.dis.progfun.catsintro

import cats.Eval // Import the Eval Monad data type.
import cats.Id // Import the Id Monad data type.
import cats.Monad // Import the Monad type class.
import cats.MonadError // Import the MonadErorr type class.
import cats.instances.either._ // Brings the implicit MonadError[Either[E, _], E] instance to scope.
import cats.instances.int._ // Brings the implicit Eq[Int] instance to scope.
import cats.instances.list._ // Brings the implicit Monad[List[_]] instance to scope.
import cats.syntax.eq.catsSyntaxEq // Provides the === operator for type safe equality checks.
import cats.syntax.functor.toFunctorOps // Provides the map operator for mapping a Monad.
import cats.syntax.flatMap.toFlatMapOps // Provides the flatMap operator for mapping a Monad.
import scala.language.higherKinds // Enable the use of higher-kinded types, like F[_].

object MonadNotes extends App {
  // FlatMap allow us to sequence computations inside a context...
  // However, unlike map, they are aware of inner contexts.
  val flatMapped1 = List(1, 2, 3).flatMap(x => List.fill(x)(x)) // Returns a list of x elements, each of value x.
  println(s"List(1, 2, 3).flatMap(x => List.fill(x)(x)) = ${flatMapped1}")

  // For comprehension can be used to simplify complex flatMap computations.
  val flatMapped2 = for {
    x <- List(1, 2, 3)
    y <- List.fill(x)(x)
  } yield y
  println(s"for (x <- List(1, 2, 3); y <- List.fill(x)(x)) yield y = ${flatMapped2}")

  // This common behavior is encapsulated in the Monad[F[_]] type class.
  val assertion1 = List(1, 2, 3).flatMap(x => List.fill(x)(x)) === Monad[List].flatMap(List(1, 2, 3))(x => List.fill(x)(x))
  println(s"List(1, 2, 3).flatMap(x => List.fill(x)(x)) === Monad[List].flatMap(List(1, 2, 3))(x => List.fill(x)(x)) is ${assertion1}")

  // Monad laws!
  // A correct implementation of a Functor for some type F[_] must satisfy three laws:
  // Left identity: Calling pure on x and transforming the result with f is the same as calling f(x).
  // Right identity: Passing pure to flatMap is the same as doing nothing.
  // Associativity: FlatMapping over two functions f and g is the same as flatMapping over f and then flatMapping over g.
  val x: Int = 3
  val xPure: List[Int] = Monad[List].pure(x)
  val f: Int => List[Int] = _ => List(1, 2, 3)
  val g: Int => List[Int] = x => List.fill(x)(x)
  val assertion2 = xPure.flatMap(f) === f(x)
  val assertion3 = xPure.flatMap(Monad[List].pure) === xPure
  val assertion4 = xPure.flatMap(f).flatMap(g) === xPure.flatMap(x => f(x).flatMap(g))
  println(s"Given f = _ => List(1, 2, 3)\t->\tMonad[List].pure(3).flatMap(f) === f(3) is ${assertion2}")
  println(s"List(3).flatMap(Monad[List].pure) === List(3) is ${assertion3}")
  println(s"Given f = _ => List(1, 2, 3) & g = x => List.fill(x)(x)\t->\t List(3).flatMap(f).flatMap(g) === List(3).flatMap(x => f(x).flatMap(g)) is ${assertion4}")

  // Monad for options.
  // Lets define our own Monad for Options.
  // BTW, every monad is also a Functor,
  // lest implement the abstract method map too.
  implicit val OptionMonad: Monad[Option] = new Monad[Option] {
    override def pure[A](a: A): Option[A] = Some(a)
    override def flatMap[A, B](fa: Option[A])(f: A => Option[B]): Option[B] = fa match {
      case Some(a) => f(a)
      case None    => None
    }
    override def map[A, B](fa: Option[A])(f: A => B): Option[B] = flatMap(fa)(x => pure(f(x)))
    override def tailRecM[A, B](a: A)(f: A => Option[Either[A, B]]): Option[B] = ???
  }
  val tenDividedSafe: Int => Option[Int] = x => if (x == 0) None else Some(10 / x)
  val flatMapped3 = OptionMonad.flatMap(Some(3))(tenDividedSafe)
  val flatMapped4 = OptionMonad.flatMap(Some(0))(tenDividedSafe)
  println(s"Given tenDividedSafe = x => if (x == 0) None else Some(10 / x)\t->\tMonad[Option]flatMap(Some(3))(tenDividedSafe) = ${flatMapped3}")
  println(s"Given tenDividedSafe = x => if (x == 0) None else Some(10 / x)\t->\tMonad[Option]flatMap(Some(0))(tenDividedSafe) = ${flatMapped4}")

  // We can use the Monad type class to write a generic method
  // That computes the square sum of two values
  // inside two instances of the same context.
  def sumSquare[F[_]: Monad](fa: F[Int], fb: F[Int]): F[Int] =
    for {
      x <- fa
      y <- fb
    } yield ((x * x) + (y * y))
  val flatMapped5 = sumSquare[Option](Some(3), Some(5))
  val flatMapped6 = sumSquare[Option](Some(3), None)
  val flatMapped7 = sumSquare(List(1, 2, 3), List(0, 5, 10))
  println(s"Given sumSquare[F[_]: Monad](fa: F[Int], fb: F[Int]): F[Int] = for (x <- fa; y <- fb) yield ((x * x) + (y * y))\t->\t sumSquare(Some(3), Some(5)) = ${flatMapped5}")
  println(s"Given sumSquare[F[_]: Monad](fa: F[Int], fb: F[Int]): F[Int] = for (x <- fa; y <- fb) yield ((x * x) + (y * y))\t->\t sumSquare(Some(3), None) = ${flatMapped6}")
  println(s"Given sumSquare[F[_]: Monad](fa: F[Int], fb: F[Int]): F[Int] = for (x <- fa; y <- fb) yield ((x * x) + (y * y))\t->\t sumSquare(List(1, 2, 3), List(0, 5, 10)) = ${flatMapped7}")

  // Identity Monad!
  // The Id[_] cats' data type, is a convenient wrapper of a plain type T
  // To use pure values in monadic computations.
  val a = Monad[Id].pure(3)
  val b = Monad[Id].pure(5)
  val flatMapped8 = sumSquare(a, b)
  println(s"Given sumSquare[F[_]: Monad](fa: F[Int], fb: F[Int]): F[Int] = for (x <- fa; y <- fb) yield ((x * x) + (y * y))\t->\t sumSquare(3, 5) = ${flatMapped8}")

  // Monad Error!
  // The Monad Error provides a better abstraction for Either-like  data types,
  // which are used for error handling.
  type ErrorOr[A] = Either[String, A]
  val EitherMonadError = MonadError[ErrorOr, String]
  val success = EitherMonadError.pure(42)
  println(s"MonadError[Either[String, _], String].pure(42) = ${success}")
  val failure = EitherMonadError.raiseError[Int]("Kabum!")
  println(s"MonadError[Either[String, _], String].raiseError('Kabum!') = ${failure}")
  val recovered = EitherMonadError.handleErrorWith(failure) { case "Kabum!" => EitherMonadError.pure(0); case _ => EitherMonadError.raiseError("Unexpected error") }
  println(s"EitherMonadError.handleErrorWith(failure) { case 'Kabum!' => EitherMonadError.pure(0); case _ => EitherMonadError.raiseError('Unexpected error') } = ${recovered}")
  val ensured = EitherMonadError.ensure(success)("Number too low")(_ >= 50)
  println(s"EitherMonadError.ensure(success)('Number too low')(_ >= 50) = ${ensured}")

  // Eval Monad!
  // The eval monad abstracts over different models of evaluation.
  // Eager - val (now), Lazy - def (always) & Memoized - lazy val (latter).
  // Note: Eval's map & flatMap methods store the chain as a list of functions (in the heap),
  // and aren't run until we ask for the Eval's value - also those two methods
  // are trampolined, that means, they are stack safe.
  // Additionally, the defer method allow us to defer the evaluation of an Eval.
  val it = List(1, 2, 3, 4, 5).toIterator
  val now = Eval.now(it.next())
  println(s"Given it = 1 to 5\t->\tNow = ${now}, value1 = ${now.value}, value2 = ${now.value}")
  val latter = Eval.later(it.next())
  println(s"Given it = 1 to 5\t->\tLatter = ${latter}, value1 = ${latter.value}, value2 = ${latter.value}")
  val always = Eval.always(it.next())
  println(s"Given it = 1 to 5\t->\tAlways = ${always}, value1 = ${always.value}, value2 = ${always.value}")
  def safeFactorial(n: BigInt): Eval[BigInt] = if (n == 1) Eval.now(1) else Eval.defer(safeFactorial(n - 1).map(_ * n))
  val safeComputed = safeFactorial(50000).value.toString.substring(0, 5) // Cap the string, the real number is very long to print
  println(s"Given safeFactorial = n => if (n == 1) Eval.now(1) else Eval.defer(safeFactorial(n - 1).map(_ * n))\t->\tsafeFactorial(50000) = ${safeComputed}...")
}