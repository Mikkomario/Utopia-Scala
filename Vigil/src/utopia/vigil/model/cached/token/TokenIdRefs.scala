package utopia.vigil.model.cached.token

import utopia.vigil.database.access.token.AccessToken

/**
 * Combines an auth token's referenced IDs, including its own
 * @param id ID of this token
 * @param templateId ID of the template this token is based on
 * @param parentId ID of the token used when creating this token, if applicable.
 * @author Mikko Hilpinen
 * @since 03.05.2026, v0.1
 */
case class TokenIdRefs(id: Int, templateId: Int, parentId: Option[Int] = None)
{
	/**
	 * Access to this token's data in the DB
	 */
	lazy val access = AccessToken(id)
}