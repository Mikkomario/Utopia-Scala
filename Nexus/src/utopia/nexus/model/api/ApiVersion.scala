package utopia.nexus.model.api

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.ScopeUsable
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.view.immutable.View

import scala.language.implicitConversions
import scala.util.Failure

object ApiVersion
{
	// ATTRIBUTES   --------------------
	
	/**
	 * The first API version
	 */
	lazy val v1 = apply(1)
	
	
	// IMPLICIT ------------------------
	
	/**
	 * @param version The major API version number to wrap
	 * @return An API version based on that version number
	 */
	implicit def apply(version: Int): ApiVersion = new ApiVersion(version)
	
	
	// OTHER    ------------------------
	
	/**
	 * Parses a string into a (major) API version
	 * @param pathPart String that may appear on a request path. E.g. "v1".
	 * @return API version matching the specified string. Failure if the string didn't represent an API version.
	 */
	def parse(pathPart: String) = {
		if (pathPart.isEmpty)
			Failure(new IllegalArgumentException("Version can't be empty"))
		else {
			val firstChar = pathPart.head
			val numbersPart = {
				if (firstChar == 'v' || firstChar == 'V')
					pathPart.tail
				else
					pathPart
			}
			numbersPart.takeWhile { _ != '.' }.tryInt.map(apply)
		}
	}
}

/**
 * A model representing a major API version (v1, v2, etc.)
 * @author Mikko Hilpinen
 * @since 07.11.2025, v2.0
 */
case class ApiVersion(value: Int) extends View[Int] with SelfComparable[ApiVersion] with ScopeUsable[ApiVersion]
{
	// COMPUTED ----------------------
	
	/**
	 * @return The API version previous to this one
	 */
	def previous = ApiVersion(value - 1)
	
	/**
	 * @return An iterator that yields this version, as well as all the previous API versions
	 */
	def decreasing = Iterator.iterate(this) { _.previous }.takeWhile { _.value >= 1 }
	
	
	// IMPLEMENTED  ------------------
	
	override def self: ApiVersion = this
	override def toString: String = s"v$value"
	
	override def compareTo(o: ApiVersion): Int = value.compareTo(o.value)
}
