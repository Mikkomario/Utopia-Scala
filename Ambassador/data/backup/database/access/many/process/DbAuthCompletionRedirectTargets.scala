package utopia.ambassador.database.access.many.process

import utopia.ambassador.database.factory.process.AuthCompletionRedirectTargetFactory
import utopia.ambassador.database.model.process.AuthCompletionRedirectTargetModel
import utopia.ambassador.model.enumeration.GrantLevel.{AccessDenied, AccessFailed, FullAccess, PartialAccess}
import utopia.ambassador.model.enumeration.GrantLevel
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing multiple redirect targets at a time
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object DbAuthCompletionRedirectTargets extends ManyRowModelAccess[AuthCompletionRedirectTarget] with UnconditionalView
{
	// COMPUTED ---------------------------------
	
	private def model = AuthCompletionRedirectTargetModel
	
	
	// IMPLEMENTED  -----------------------------
	
	override protected def defaultOrdering = None
	override def factory = AuthCompletionRedirectTargetFactory
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param preparationId An authentication preparation id
	  * @return An access point to redirect targets specified for that authentication attempt
	  */
	def forPreparationWithId(preparationId: Int) = new DbAuthPreparationRedirectTargets(preparationId)
	
	
	// NESTED   ---------------------------------
	
	class DbAuthPreparationRedirectTargets(val preparationId: Int)
		extends ManyRowModelAccess[AuthCompletionRedirectTarget] with SubView
	{
		// COMPUTED -----------------------------
		
		/**
		  * @param connection Implicit DB Connection
		  * @return Redirect targets available for the full access case
		  */
		def forFullAccess(implicit connection: Connection) =
			forResult(FullAccess)
		
		
		// IMPLEMENTED  -------------------------
		
		override protected def parent = DbAuthCompletionRedirectTargets
		
		override def filterCondition = model.withPreparationId(preparationId).toCondition
		
		override protected def defaultOrdering = parent.defaultOrdering
		override def factory = parent.factory
		
		
		// OTHER    -----------------------------
		
		/**
		  * @param grantLevel Grant level given by the user
		  * @param connection Implicit DB Connection
		  * @return Available redirect targets for that result
		  */
		def forResult(grantLevel: GrantLevel)(implicit connection: Connection): Vector[AuthCompletionRedirectTarget] =
			grantLevel match
			{
				case PartialAccess => forResult(wasSuccess = true, didDenyAccess = true)
				case FullAccess => forResult(wasSuccess = true)
				case AccessDenied => forResult(wasSuccess = false, didDenyAccess = true)
				case AccessFailed => forResult(wasSuccess = false)
			}
		/**
		  * @param wasSuccess A result state: Success (true) or Failure (false)
		  * @param didDenyAccess Whether the user denied access partially or fully (default = false)
		  * @param connection Implicit DB Connection
		  * @return Targets that apply to that state
		  */
		def forResult(wasSuccess: Boolean, didDenyAccess: Boolean = false)
		             (implicit connection: Connection) =
		{
			val resultColumn = model.resultFilterColumn
			val defaultCondition = resultColumn.isNull
			val resultCondition = resultColumn <=> wasSuccess
			
			val finalResultCondition = if (didDenyAccess) resultCondition else
				resultCondition && model.accessAllowedCondition
			
			find(defaultCondition || finalResultCondition)
		}
	}
}
