package utopia.metropolis.model.partial.language

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

/**
  * Represents a language
  * @param isoCode 2 letter ISO-standard code for this language
  * @param created Time when this Language was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class LanguageData(isoCode: String, created: Instant = Now) extends StyledModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("iso_code" -> isoCode, "created" -> created))
	
	override def toSimpleModel = Model(Vector("code" -> isoCode))
}

