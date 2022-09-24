package utopia.ambassador.controller.implementation

import utopia.ambassador.controller.template.AuthRedirector
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.ambassador.model.stored.scope.Scope
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.flow.generic.model.immutable.Model
import utopia.vault.database.Connection

/**
  * A redirector implementation that can be used in simple OAuth processes that don't require additional
  * parameters or parameter encoding (E.g. Zoom)
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object DefaultRedirector extends AuthRedirector
{
	override def parameterEncoding = None
	
	override def extraParametersFor(settings: AuthServiceSettings, preparation: AuthPreparation, scopes: Vector[Scope])
	                               (implicit connection: Connection) = Model.empty
}
