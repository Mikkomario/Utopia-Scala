package utopia.logos.model.stored.url

import utopia.flow.parse.string.Regex
import utopia.logos.database.access.single.url.link.DbSingleLink
import utopia.logos.model.partial.url.LinkData
import utopia.vault.model.template.StoredModelConvertible

object Link
{
	private lazy val questionMarkRegex = Regex.escape('?')
	private lazy val pathCharacterRegex = (Regex.letterOrDigit || Regex.anyOf("-._~:/#[]@!$&'()*+,;%="))
		.withinParenthesis
	private lazy val urlCharacterRegex = (pathCharacterRegex || questionMarkRegex).withinParenthesis
	
	/**
	 * A regular expression that matches to the parameters -part of a link
	 */
	lazy val paramPartRegex = (questionMarkRegex + urlCharacterRegex.oneOrMoreTimes).withinParenthesis
	
	/**
	 * A regular expression that matches to links
	 */
	lazy val regex = Domain.regex + pathCharacterRegex.anyTimes + paramPartRegex.noneOrOnce
}

/**
  * Represents a link that has already been stored in the database
  * @param id id of this link in the database
  * @param data Wrapped link data
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class Link(id: Int, data: LinkData) extends StoredModelConvertible[LinkData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this link in the database
	  */
	def access = DbSingleLink(id)
}

