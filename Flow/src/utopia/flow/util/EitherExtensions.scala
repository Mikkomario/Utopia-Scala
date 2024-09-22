package utopia.flow.util

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}

import scala.collection.immutable.VectorBuilder

/**
  * Adds new functions for [[Either]]s
  * @author Mikko Hilpinen
  * @since 22.09.2024, v2.5
  */
object EitherExtensions
{
	// TYPES    -----------------------
	
	/**
	  * Type where the item exists either on the Left or the Right side
	  */
	type Sided[+A] = Either[A, A]
	
	
	// EXTENSIONS   ------------------
	
	implicit class RichEither[L, R](val e: Either[L, R]) extends AnyVal
	{
		/**
		  * @return This either's left value or None if this either is right
		  */
		def leftOption = e match {
			case Left(l) => Some(l)
			case Right(_) => None
		}
		/**
		  * @return This either's right value or None if this either is left (same as toOption)
		  */
		def rightOption = e.toOption
		
		/**
		  * If this either is left, maps it
		  * @param f A mapping function for left side
		  * @tparam B New type for left side
		  * @return A mapped version of this either
		  */
		def mapLeft[B](f: L => B) = e match {
			case Right(r) => Right(r)
			case Left(l) => Left(f(l))
		}
		/**
		  * If this either is right, maps it
		  * @param f A mapping function for left side
		  * @tparam B New type for right side
		  * @return A mapped version of this either
		  */
		def mapRight[B](f: R => B) = e match {
			case Right(r) => Right(f(r))
			case Left(l) => Left(l)
		}
		
		/**
		  * @param f A mapping function applied if this is left.
		  *          Returns a new either.
		  * @tparam L2 Type of left in the mapping result.
		  * @tparam R2 Type of the resulting right type.
		  * @return This if right, mapping result if left
		  */
		def divergeMapLeft[L2, R2 >: R](f: L => Either[L2, R2]) = e match {
			case Right(r) => Right(r)
			case Left(l) => f(l)
		}
		/**
		  * @param f A mapping function applied if this is right.
		  *          Returns a new either.
		  * @tparam L2 Type of the resulting left type.
		  * @tparam R2 Type of right in the mapping result.
		  * @return This if left, mapping result if right
		  */
		def divergeMapRight[L2 >: L, R2](f: R => Either[L2, R2]) = e match {
			case Right(r) => f(r)
			case Left(l) => Left(l)
		}
		
		/**
		  * @param f A mapping function for left values
		  * @tparam B Type of map result
		  * @return Right value or the mapped left value
		  */
		def rightOrMap[B >: R](f: L => B) = e match {
			case Right(r) => r
			case Left(l) => f(l)
		}
		/**
		  * @param f A mapping function for right values
		  * @tparam B Type of map result
		  * @return Left value or the mapped right value
		  */
		def leftOrMap[B >: L](f: R => B) = e match {
			case Right(r) => f(r)
			case Left(l) => l
		}
		
		/**
		  * Maps the value of this either to a single value, whichever side this is
		  * @param leftMap  Mapping function used when left value is present
		  * @param rightMap Mapping function used when right value is present
		  * @tparam B Resulting item type
		  * @return Mapped left or mapped right
		  */
		def mapToSingle[B](leftMap: L => B)(rightMap: R => B) = e match {
			case Right(r) => rightMap(r)
			case Left(l) => leftMap(l)
		}
		/**
		  * Maps this either, no matter which side it is
		  * @param leftMap  Mapping function used when this either is left
		  * @param rightMap Mapping function used when this either is right
		  * @tparam L2 New left type
		  * @tparam R2 New right type
		  * @return A mapped version of this either (will have same side)
		  */
		def mapBoth[L2, R2](leftMap: L => L2)(rightMap: R => R2) = e match {
			case Right(r) => Right(rightMap(r))
			case Left(l) => Left(leftMap(l))
		}
	}
	
	implicit class RichSingleTypeEither[A](val e: Either[A, A]) extends AnyVal
	{
		/**
		  * @return Left or right side value, whichever is defined
		  */
		def either = e match {
			case Left(l) => l
			case Right(r) => r
		}
		/**
		  * @return The left or the right side value, plus the side from which the item was found.
		  *         First represents the left side and Last represents the right side.
		  */
		def eitherAndSide: (A, End) = e match {
			case Left(l) => l -> First
			case Right(r) => r -> Last
		}
		
		/**
		  * @return A pair based on this either, where the non-occupied side receives None and the occupied side
		  *         receives Some
		  */
		def toPair = e match {
			case Left(l) => Pair(Some(l), None)
			case Right(r) => Pair(None, Some(r))
		}
		
		/**
		  * @param f A mapping function
		  * @tparam B Mapping result type
		  * @return Mapping result, keeping the same side
		  */
		def mapEither[B](f: A => B) = e match {
			case Left(l) => Left(f(l))
			case Right(r) => Right(f(r))
		}
		/**
		  * Maps the value in this either, but only if the the value resides on the specified side
		  * @param side The side to map (if applicable), where First represents left and Last represents Right
		  * @param f A mapping function to use, if applicable
		  * @tparam B Type of mapping result
		  * @return Either this either, if the value resided on the opposite side, or a mapped copy of this either
		  */
		def mapSide[B >: A](side: End)(f: A => B) = e match {
			case Left(l) => if (side == First) Left(f(l)) else e
			case Right(r) => if (side == Last) Right(f(r)) else e
		}
		/**
		  * @param f A mapping function
		  * @tparam B Mapping result type
		  * @return Mapping function result, whether from left or from right
		  */
		def mapEitherToSingle[B](f: A => B) = e match {
			case Left(l) => f(l)
			case Right(r) => f(r)
		}
	}
	
	implicit class RichIterableOnceEithers[L, R](val i: IterableOnce[Either[L, R]]) extends AnyVal
	{
		/**
		  * Divides this collection to two separate collections, one for left items and one for right items
		  * @return The Left items (1) and then the Right items (2)
		  */
		def divided = {
			val lBuilder = new VectorBuilder[L]
			val rBuilder = new VectorBuilder[R]
			i.iterator.foreach {
				case Left(l) => lBuilder += l
				case Right(r) => rBuilder += r
			}
			lBuilder.result() -> rBuilder.result()
		}
	}
}
