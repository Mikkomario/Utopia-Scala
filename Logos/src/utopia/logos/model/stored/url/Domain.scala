package utopia.logos.model.stored.url

import utopia.flow.parse.string.Regex
import utopia.logos.database.access.single.url.domain.DbSingleDomain
import utopia.logos.model.factory.url.DomainFactory
import utopia.logos.model.partial.url.DomainData
import utopia.vault.model.template.{FromIdFactory, StoredModelConvertible}

import java.time.Instant

/**
  * Represents a domain that has already been stored in the database
  * @param id id of this domain in the database
  * @param data Wrapped domain data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class Domain(id: Int, data: DomainData) 
	extends StoredModelConvertible[DomainData] with DomainFactory[Domain] with FromIdFactory[Int, Domain]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this domain in the database
	  */
	def access = DbSingleDomain(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def toString = data.url
	
	override def withCreated(created: Instant) = copy(data = data.withCreated(created))
	
	override def withId(id: Int) = copy(id = id)
	
	override def withUrl(url: String) = copy(data = data.withUrl(url))
}

object Domain
{
	// ATTRIBUTES	--------------------
	
	/**
	  * A regular expression that matches a forward slash (/)
	  */
	lazy val forwardSlashRegex = Regex.escape('/')
	
	private lazy val colonRegex = Regex.escape(':')
	
	private lazy val domainCharacterRegex = (Regex.letterOrDigit || Regex.anyOf("-.")).withinParenthesis
	
	private lazy val httpRegex = 
		(Regex("http") + Regex("s").noneOrOnce + colonRegex + forwardSlashRegex.times(2)).withinParenthesis
	
	private lazy val wwwRegex = Regex("w").times(3)
	
	private lazy val portNumberRegex = (colonRegex + Regex.digit.times(1 to 6)).withinParenthesis
	
	/**
	  * A regular expression that matches a domain part of a link.
	  * Includes the initial forward slash, if present.
	  * For example, matches: "https://api.example.com/", "http://128.0.0.1:8080/" and "www.palvelu.fi"
	  */
	lazy val regex = 
		(httpRegex || wwwRegex).withinParenthesis +domainCharacterRegex.oneOrMoreTimes + 
			Regex.escape('.') +domainCharacterRegex.oneOrMoreTimes + portNumberRegex.noneOrOnce + 
			forwardSlashRegex.noneOrOnce
}

