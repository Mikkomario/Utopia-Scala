package utopia.citadel.database.factory.description

/**
  * Provides access to linked description factories specific to the Citadel module
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
object CitadelLinkedDescriptionFactory
{
	lazy val descriptionRole = LinkedDescriptionFactory(CitadelDescriptionLinkFactory.descriptionRole)
	lazy val language = LinkedDescriptionFactory(CitadelDescriptionLinkFactory.language)
	lazy val languageFamiliarity = LinkedDescriptionFactory(CitadelDescriptionLinkFactory.languageFamiliarity)
	lazy val organization = LinkedDescriptionFactory(CitadelDescriptionLinkFactory.organization)
	lazy val task = LinkedDescriptionFactory(CitadelDescriptionLinkFactory.task)
	lazy val userRole = LinkedDescriptionFactory(CitadelDescriptionLinkFactory.userRole)
}
