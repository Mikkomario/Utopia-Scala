package utopia.reflection.test

import utopia.firmament.localization.LocalString._
import utopia.firmament.localization.{Language, LocalString, LocalizedString, Localizer}
import utopia.paradigm.generic.ParadigmDataType

/**
  * This class tests localization features
  * @author Mikko Hilpinen
  * @since 22.5.2019, v1+
  */
object LocalizationTest extends App
{
	ParadigmDataType.setup()
	
	implicit val sourceLanguage: Language = Language.english
	implicit private val localizer: Localizer = UpperCaseLocalizer
	
	assert(localizer("Hello") == "HELLO".local)
	
	val autoLocalized: LocalizedString = "Hello"
	assert(autoLocalized == "HELLO".local)
	
	val localInterpolated = LocalString("Hello %s (%i) %S %d").interpolateAll(Vector("Manu", 2, "mies", 1.3456))
	println(localInterpolated)
	assert(localInterpolated.wrapped == "Hello Manu (2) MIES 1.35")
	
	val localTemplate2 = LocalString("Hello ${name} (${age}) %s")
	val localInterpolated2 = localTemplate2.interpolateNamed(Map("name" -> "Mickey", "age" -> 12))
	println(localInterpolated2)
	assert(localInterpolated2.wrapped == "Hello Mickey (12) %s")
	
	println("Success!")
}

private object UpperCaseLocalizer extends Localizer
{
	// Simply transforms strings to upper case
	override def apply(string: LocalString) = string.map { _.toUpperCase }.skipLocalization
}