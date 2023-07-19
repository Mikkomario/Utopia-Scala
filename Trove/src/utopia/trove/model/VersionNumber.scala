package utopia.trove.model

import utopia.flow.util.StringExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.SelfComparable
import utopia.flow.collection.CollectionExtensions._

@deprecated("Replaced with Version (Flow)", "v1.1")
object VersionNumber
{
	/**
	  * @param firstNumber First version number
	  * @param moreNumbers Consequent version numbers
	  * @return A version number with specified numbers in order
	  */
	def apply(firstNumber: Int, moreNumbers: Int*): VersionNumber = VersionNumber(Vector(firstNumber) ++ moreNumbers)
	
	/**
	  * @param versionString A string representing a version number (Eg. "v1.2.3-alpha")
	  * @return A version number based on the string
	  */
	def parse(versionString: String) = {
		val (front, back) = versionString.splitAtFirst("-").toTuple
		VersionNumber(front.split('.').flatMap { s => s.digits.int }.toVector, back)
	}
}

/**
  * Represents a version number
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  * @param numbers Numbers that form this version number
  * @param suffix Suffix added to the end of this version number (Eg. "beta") (Optional)
  */
@deprecated("Replaced with Version (Flow)", "v1.1")
case class VersionNumber(numbers: Vector[Int], suffix: String = "") extends SelfComparable[VersionNumber]
{
	// COMPUTED	------------------------
	
	/**
	  * @return Whether this version number contains a suffix
	  */
	def hasSuffix = suffix.nonEmpty
	
	/**
	  * @return A copy of this version number without any suffix included
	  */
	def withoutSuffix = if (hasSuffix) copy(suffix = "") else this
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override def compareTo(o: VersionNumber) = numbers.zip(o.numbers).find { case (a, b) => a != b } match
	{
		case Some((a, b)) => a.compareTo(b)
		case None =>
			val sizeCompare = numbers.dropRightWhile { _ == 0 }.size.compareTo(o.numbers.dropRightWhile { _ == 0 }.size)
			if (sizeCompare == 0)
			{
				if (hasSuffix)
				{
					if (o.hasSuffix)
						suffix.compareTo(o.suffix)
					else
						-1
				}
				else if (o.hasSuffix)
					1
				else
					0
			}
			else
				sizeCompare
	}
	
	override def toString = s"v${numbers.dropRightWhile { _ == 0 }.mkString(".")}${if (hasSuffix) s"-$suffix" else ""}"
	
	
	// OTHER	-----------------------
	
	/**
	  * @param suffix A new suffix
	  * @return A copy of this version number with specified suffix
	  */
	def withSuffix(suffix: String) = copy(suffix = suffix)
}
