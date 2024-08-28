package utopia.logos.model.stored.url

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.flow.parse.string.Regex
import utopia.logos.database.access.single.url.link.DbSingleLink
import utopia.logos.model.factory.url.LinkFactoryWrapper
import utopia.logos.model.partial.url.LinkData
import utopia.vault.model.template.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object Link extends StoredFromModelFactory[LinkData, Link]
{
	// ATTRIBUTES	--------------------
	
	private lazy val questionMarkRegex = Regex.escape('?')
	private lazy val pathCharacterRegex = 
		(Regex.letterOrDigit || Regex.anyOf("-._~:/#[]@!$&'()*+,;%=")).withinParenthesis
	private lazy val urlCharacterRegex = (pathCharacterRegex || questionMarkRegex).withinParenthesis
	/**
	  * A regular expression that matches to the parameters -part of a link
	  */
	lazy val paramPartRegex = (questionMarkRegex + urlCharacterRegex.oneOrMoreTimes).withinParenthesis
	/**
	  * A regular expression that matches to links
	  */
	lazy val regex = Domain.regex + pathCharacterRegex.anyTimes + paramPartRegex.noneOrOnce
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = LinkData
	
	override protected def complete(model: AnyModel, data: LinkData) = model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a link that has already been stored in the database
  * @param id id of this link in the database
  * @param data Wrapped link data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class Link(id: Int, data: LinkData) 
	extends StoredModelConvertible[LinkData] with FromIdFactory[Int, Link] with LinkFactoryWrapper[LinkData, Link]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this link in the database
	  */
	def access = DbSingleLink(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: LinkData) = copy(data = data)
}

