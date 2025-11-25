package utopia.logos.database.access.many.url.domain

import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple domains at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with DomainDb and AccessDomains.", "v0.7")
object DbDomains extends ManyDomainsAccess with UnconditionalView with ViewManyByIntIds[ManyDomainsAccess]
