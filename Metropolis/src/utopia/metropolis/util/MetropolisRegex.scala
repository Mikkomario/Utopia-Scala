package utopia.metropolis.util

import utopia.flow.parse.string.Regex

/**
  * A collection of regular expressions used in Metropolis -related features
  * @author Mikko Hilpinen
  * @since 21.2.2022, v2.1
  */
object MetropolisRegex
{
	/**
	  * A regular expression matching to an email address (with correct formatting)
	  */
	lazy val email = Regex.nonWhiteSpace.oneOrMoreTimes + Regex.escape('@') + Regex.nonWhiteSpace.oneOrMoreTimes +
		Regex.escape('.') + Regex.letterOrDigit.oneOrMoreTimes
}
