package utopia.reflection.localization

/**
  * This is a simple localizer that skips all localization. This can be used as a localizer in programs where no
  * localization is required or available (no pun intended), like test projects.
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
  */
object NoLocalization extends Localizer
{
	override def localize(string: LocalString) = string.localizationSkipped
}