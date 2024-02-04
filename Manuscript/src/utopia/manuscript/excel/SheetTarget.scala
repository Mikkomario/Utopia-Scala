package utopia.manuscript.excel

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.StringExtensions._
import utopia.manuscript.excel.SheetTarget.OrTarget

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
  * Common trait for different ways of locating the targeted spread-sheet
  * @author Mikko Hilpinen
  * @since 31/01/2024, v1.0
  */
trait SheetTarget
{
	// ABSTRACT ------------------------
	
	/**
	  * Finds the targeted spread-sheet from the specified list
	  * @param sheetNames An ordered list of sheet names
	  * @return 0-based index of the sheet to target
	  */
	def apply(sheetNames: Seq[String]): Try[Int]
	
	
	// OTHER    -----------------------
	
	/**
	  * @param other Another target used as a backup
	  * @return A target that primarily uses this target, but also uses the second target as a backup
	  */
	def ||(other: SheetTarget): SheetTarget = new OrTarget(this, other)
}

object SheetTarget
{
	// COMPUTED -----------------------------
	
	/**
	  * @return Target that always targets the first available spread-sheet
	  */
	def first = FirstSheet
	/**
	  * @return Target that always targets the last available spread-sheet
	  */
	def last = LastSheet
	
	
	// IMPLICIT    --------------------------
	
	/**
	  * @param f A function that accepts a list of available sheet names
	  *          and returns the index of the sheet to read / target.
	  *          May return a failure.
	  * @return A new sheet target which wraps the specified function
	  */
	implicit def apply(f: Seq[String] => Try[Int]): SheetTarget = new FunctionalSheetTarget(f)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param i Targeted index (0-based)
	  * @return Target that reads the sheet at that index
	  */
	def index(i: Int) = SheetAtIndex(i)
	/**
	  * @param name Targeted sheet's name (case-insensitive)
	  * @return Target that reads a sheet with that name
	  */
	def name(name: String) = SheetWithName(name)
	/**
	  * @param string String to search from spread-sheet names
	  * @return A target that selects a sheet which contains the specified string
	  */
	def containing(string: String) = SheetContaining(string)
	
	
	// NESTED   --------------------------
	
	/**
	  * Target that always reads the first spread-sheet
	  */
	case object FirstSheet extends SheetTarget
	{
		def apply(sheetNames: Seq[String]): Try[Int] =
			if (sheetNames.isEmpty) Failure(new IllegalArgumentException("No sheets available")) else Success(0)
	}
	/**
	  * Target that always reads the last spread-sheet
	  */
	case object LastSheet extends SheetTarget
	{
		def apply(sheetNames: Seq[String]): Try[Int] = {
			if (sheetNames.isEmpty)
				Failure(new IllegalArgumentException("No sheets available"))
			else
				Success(sheetNames.size - 1)
		}
	}
	
	/**
	  * Target that reads a spread-sheet at a specific index
	  * @param index Targeted spread-sheet index
	  */
	case class SheetAtIndex(index: Int) extends SheetTarget
	{
		// INITIAL CODE ------------------
		
		// Specified index must be non-negative
		if (index < 0)
			throw new IllegalArgumentException(s"The specified sheet index $index is negative!")
		
		
		// IMPLEMENTED  ------------------
		
		def apply(sheetNames: Seq[String]): Try[Int] = {
			if (sheetNames.hasSize > index)
				Success(index)
			else
				Failure(new IndexOutOfBoundsException(
					s"The specified sheet index $index doesn't fall into the accepted range (< ${sheetNames.size})"))
		}
	}
	
	object SheetWithName
	{
		/**
		  * @param name Targeted spread-sheet name
		  * @param maxVariance Maximum differences (missing, additional or different characters) allowed (default = 1)
		  * @return A target that searches for the specified name, allowing for some difference
		  */
		def resembling(name: String, maxVariance: Int = 1) =
			SheetWithNameResembling(name, maxVariance)
	}
	/**
	  * Target which reads a spread-sheet with a specific name
	  * @param name Targeted spread-sheet name (case-insensitive)
	  */
	case class SheetWithName(name: String) extends SheetTarget
	{
		// COMPUTED --------------------------
		
		/**
		  * @return Copy of this target, which allows for small differences in sheet names
		  */
		def orSimilar = SheetWithNameResembling(name)
		
		
		// IMPLEMENTED  ----------------------
		
		def apply(sheetNames: Seq[String]): Try[Int] = sheetNames.findIndexWhere { _ ~== name }
			.toTry { new NoSuchElementException(s"No sheet name matches target '$name'. Available options are: [${
				sheetNames.map { n => s"'$n'" }.mkString(", ") }]") }
		
		
		// OTHER    --------------------------
		
		/**
		  * @param maxDifferences Maximum number of differences (missing or additional characters, or wrong characters)
		  *                       allowed
		  * @return Copy of this target which allows for some difference
		  */
		def allowingDifferenceOf(maxDifferences: Int): SheetTarget = {
			if (maxDifferences <= 0) this else SheetWithNameResembling(name, maxDifferences)
		}
	}
	
	/**
	  * A target which searches for the specified sheet name, but allows for some difference (typos etc.).
	  * Prefers exact matches, of course.
	  * @param name Targeted spread-sheet name
	  * @param allowedVariance Maximum number of differences allowed.
	  *                        A difference may be:
	  *                             1. A missing character,
	  *                             1. An additional chracter,
	  *                             1. A wrong character
	  *
  *                            Default maximum is 1.
	  */
	case class SheetWithNameResembling(name: String, allowedVariance: Int = 1) extends SheetTarget
	{
		def apply(sheetNames: Seq[String]): Try[Int] = {
			findSimilar(sheetNames.zipWithIndex, allowedVariance).headOption match {
				case Some((_, index)) => Success(index)
				case None =>
					Failure(new NoSuchElementException(
						s"None of the specified sheet names resembles '$name'. Tested options: [${
							sheetNames.map { n => s"'$n'" }.mkString(", ") }]"))
			}
		}
		
		@tailrec
		private def findSimilar(options: Seq[(String, Int)], maxVariance: Int): Seq[(String, Int)] = {
			// Performs the next level of filtering using the specified max variance setting
			val filtered = {
				if (maxVariance <= 0)
					options.filter { _._1 ~== name  }
				else
					options.filter { _._1.isSimilarTo(name, allowedVariance) }
			}
			// Case: Filtered too much => Returns the unfiltered version (which still yields results)
			if (filtered.isEmpty)
				options
			// Case: Can't filter further => Returns the fully filtered list
			else if (maxVariance <= 0 || filtered.hasSize(1))
				filtered
			// Case: May filter further => Performs the next round of filtering
			else
				findSimilar(filtered, maxVariance - 1)
		}
	}
	
	case class SheetContaining(string: String) extends SheetTarget
	{
		override def apply(sheetNames: Seq[String]): Try[Int] =
			sheetNames.findIndexWhere { _.containsIgnoreCase(string) }
				.toTry { new NoSuchElementException(
					s"None of the specified sheet names contained the searched string '$string'. Searched from: ${
						sheetNames.map { n => s"'$n'" }.mkString(", ") }") }
	}
	
	private class OrTarget(primary: SheetTarget, secondary: SheetTarget) extends SheetTarget
	{
		override def apply(sheetNames: Seq[String]): Try[Int] = primary(sheetNames).orElse(secondary(sheetNames))
	}
	
	private class FunctionalSheetTarget(f: Seq[String] => Try[Int]) extends SheetTarget
	{
		override def apply(sheetNames: Seq[String]): Try[Int] = f(sheetNames)
	}
}