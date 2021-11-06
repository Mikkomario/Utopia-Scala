package utopia.vault.coder.model.scala

import utopia.flow.time.Today
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.Version
import utopia.vault.coder.model.data.ProjectSetup
import utopia.vault.coder.model.scala.DeclarationDate.primaryFormat

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

object DeclarationDate
{
	/**
	  * Date that represents today
	  */
	val today = apply(Today)
	
	private val primaryFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
	private val alternativeFormats = Vector(
		DateTimeFormatter.ISO_DATE,
		DateTimeFormatter.ofPattern("d.M.uuuu")
	)
	
	/**
	  * @param setup Implicit project setup
	  * @return Declaration date set to today and including a version number if one is available
	  */
	def versionedToday(implicit setup: ProjectSetup) = apply(Today, setup.version)
	
	/**
	  * Parses a declaration date from a string
	  * @param string String to parse
	  * @return A declaration date from that string. Failure if string couldn't be parsed.
	  */
	def apply(string: String): Try[DeclarationDate] =
	{
		if (string.contains(',')) {
			val (datePart, versionPart) = string.splitAtLast(",")
			parseDate(datePart.trim).map { date => apply(date, Version.findFrom(versionPart)) }
		}
		else
			parseDate(string.trim).map { apply(_) }
	}
	
	private def parseDate(string: String) =
	{
		val primaryResult = Try { LocalDate.parse(string, primaryFormat) }
		if (primaryResult.isSuccess)
			primaryResult
		else
			alternativeFormats.view.map { format => Try { LocalDate.parse(string, format) } }.find { _.isSuccess }
				.getOrElse(primaryResult)
	}
}

/**
  * Represents a value which can be put to a since scaladoc
  * @author Mikko Hilpinen
  * @since 6.11.2021, v1.4
  */
case class DeclarationDate(date: LocalDate = Today, version: Option[Version] = None)
{
	// IMPLEMENTED  -------------------------------
	
	override def toString = {
		val base = primaryFormat.format(date)
		version match {
			case Some(version) => s"$base, $version"
			case None => base
		}
	}
	
	
	// OTHER    -----------------------------------
	
	/**
	  * @param other Another date
	  * @return Minimum between these two dates
	  */
	def min(other: DeclarationDate) = {
		val newVersion = version match {
			case Some(myVersion) =>
				other.version match {
					case Some(otherVersion) => Some(myVersion min otherVersion)
					case None => Some(myVersion)
				}
			case None => other.version
		}
		DeclarationDate(date min other.date, newVersion)
	}
}