package utopia.exodus.database.access.many.auth

import utopia.exodus.model.combined.auth.TypedToken
import utopia.vault.nosql.view.{NonDeprecatedView, UnconditionalView}

/**
  * A root access point to tokens, including their type information
  * @author Mikko Hilpinen
  * @since 20.2.2022, v4.0
  */
object DbTypedTokens extends ManyTypedTokensAccess with NonDeprecatedView[TypedToken]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return A copy of this access point where historical information is included
	  */
	def includingHistory = DbAllTypedTokens
	
	
	// NESTED   ----------------------------
	
	/**
	  * A root access point to tokens, whether they be active or not, where type information is included
	  */
	object DbAllTypedTokens extends ManyTypedTokensAccess with UnconditionalView
}