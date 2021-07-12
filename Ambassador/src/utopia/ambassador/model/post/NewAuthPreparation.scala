package utopia.ambassador.model.post

import utopia.flow.datastructure.immutable.Value

/**
  * Posted when a new authentication attempt needs to be prepared.
  * Contains information concerning the upcoming attempt.
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
case class NewAuthPreparation(state: Value = Value.empty, successRedirectUrl: String)
