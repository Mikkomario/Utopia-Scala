package utopia.metropolis.model.partial.language

import java.time.Instant
import utopia.flow.datastructure.immutable.ModelDeclaration
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.StringType
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

object LanguageData extends FromModelFactoryWithSchema[LanguageData]
{
	override val schema = ModelDeclaration("iso_code" -> StringType)
	
	override protected def fromValidatedModel(model: Model) =
		LanguageData(model("iso_code"), model("created"))
}

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

