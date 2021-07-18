package utopia.ambassador.model.partial.process

import utopia.ambassador.model.enumeration.AuthCompletionType
import utopia.ambassador.model.enumeration.AuthCompletionType.Default

/**
  * Contains a redirection client that has been prepared for an authentication attempt
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  * @param preparationId Id of the authentication preparation this redirect url is defined for
  * @param url Url where the user should be redirected
  * @param resultFilter A filter that must be met in order for this redirect to activate
  *                     (default = Default = this is the default url to use)
  */
case class AuthCompletionRedirectTargetData(preparationId: Int, url: String,
                                            resultFilter: AuthCompletionType = Default)
