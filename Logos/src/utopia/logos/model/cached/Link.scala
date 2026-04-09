package utopia.logos.model.cached

import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._
import utopia.logos.model.cached.Link.{parameterAssignmentRegex, parameterSeparatorRegex}
import utopia.logos.model.stored.url.Domain

/**
 * An interface for dealing with links
 *
 * @author Mikko Hilpinen
 * @since 22.12.2024, v0.3
 */
object Link
{
	// ATTRIBUTES	--------------------
	
	private lazy val questionMarkRegex = Regex.escape('?')
	private lazy val pathCharacterRegex =
		(Regex.letterOrDigit || Regex.anyOf("-._~:/#[]@!$&'()*+,;%=")).withinParentheses
	private lazy val urlCharacterRegex = (pathCharacterRegex || questionMarkRegex).withinParentheses
	/**
	 * A regular expression that matches to the parameters -part of a link
	 */
	lazy val paramPartRegex = (questionMarkRegex + urlCharacterRegex.oneOrMoreTimes).withinParentheses
	/**
	 * A regular expression that matches to links
	 */
	lazy val regex = Domain.regex +
		(Regex.forwardSlash + pathCharacterRegex.anyTimes).withinParentheses.noneOrOnce +
		paramPartRegex.noneOrOnce
	
	private lazy val parameterSeparatorRegex = Regex.escape('&').ignoringQuotations
	private lazy val parameterAssignmentRegex = Regex.escape('=').ignoringQuotations
	
	
	// OTHER    -----------------------
	
	/**
	 * Parses a string into a link where the domain and the parameters have been separated
	 * @param link Link string to parse
	 * @return Parsed link. None if the link didn't contain a domain part.
	 */
	def apply(link: String): Option[Link] = Domain.regex.firstRangeFrom(link).map { domainRange =>
		// Removes the / from domain, if present
		val domainPart = link.slice(domainRange).notEndingWith("/")
		val remainingPart = link.drop(domainRange.last + 2)
		paramPartRegex.firstRangeFrom(remainingPart) match {
			// Case: Parameters are specified => Extracts them from the path
			case Some(paramsRange) =>
				val paramsPart = remainingPart.slice(paramsRange).drop(1)
				val pathPart = remainingPart.take(paramsRange.start)
				apply(domainPart, pathPart, paramsPart)
				
			// Case: No parameters specified
			case None => apply(domainPart, remainingPart)
		}
	}
}

case class Link(domain: String, path: String = "", paramsStr: String = "")
{
	/**
	 * Parameters as a model
	 */
	lazy val params = Model.withConstants(parameterSeparatorRegex.split(paramsStr)
		.filter { _.nonEmpty }.flatMap { assignment =>
			// Splits into parameter name and value
			parameterAssignmentRegex.firstRangeFrom(assignment) match {
				case Some(assignRange) =>
					NotEmpty(assignment.take(assignRange.start)).map { paramName =>
						Constant(paramName, assignment.drop(assignRange.last + 1))
					}
				// Case: No assignment => Treats as null value
				case None => Some(Constant(assignment, Value.empty))
			}
		})
	
	override lazy val toString = s"$domain${ path.prependIfNotEmpty("/") }${ paramsStr.prependIfNotEmpty("?") }"
}
