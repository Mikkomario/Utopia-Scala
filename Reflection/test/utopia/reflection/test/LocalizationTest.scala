package utopia.reflection.test

import utopia.genesis.generic.GenesisDataType
import utopia.reflection.localization.{LocalString, LocalizedString, Localizer}

/**
  * This class tests localization features
  * @author Mikko Hilpinen
  * @since 22.5.2019, v1+
  */
object LocalizationTest extends App
{
	GenesisDataType.setup()
	
	implicit val sourceLanguageCode: String = "EN"
	implicit private val localizer: Localizer = UpperCaseLocalizer
	
	assert(localizer.localize("Hello").localized.get == LocalString("HELLO", "EN"))
	
	val autoLocalized: LocalizedString = "Hello"
	assert(autoLocalized.localized.get == LocalString("HELLO", "EN"))
	
	val localInterpolated = LocalString("Hello %s (%i) %S %d").interpolate("Manu", 2, "mies", 1.3456)
	println(localInterpolated)
	assert(localInterpolated.string == "Hello Manu (2) MIES 1.35")
	
	println("Success!")
}

private object UpperCaseLocalizer extends Localizer
{
	// Simply transforms strings to upper case
	override def localize(string: LocalString) = LocalizedString(string, LocalString(string.string.toUpperCase,
		string.languageCode))
}