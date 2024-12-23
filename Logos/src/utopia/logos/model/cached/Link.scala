package utopia.logos.model.cached

import utopia.flow.parse.string.Regex
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
	lazy val regex = Domain.regex + pathCharacterRegex.anyTimes + paramPartRegex.noneOrOnce
}
