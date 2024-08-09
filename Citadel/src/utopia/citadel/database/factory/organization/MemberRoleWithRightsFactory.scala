package utopia.citadel.database.factory.organization

import utopia.metropolis.model.combined.organization.MemberRoleWithRights
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.Result
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.template.Deprecatable
import utopia.vault.sql.JoinType
import utopia.vault.util.ErrorHandling

import scala.util.{Failure, Success}

/**
  * Used for reading member role data, including the allowed task ids
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
object MemberRoleWithRightsFactory extends FromResultFactory[MemberRoleWithRights] with Deprecatable
{
	// ATTRIBUTES   -----------------------------
	
	override lazy val selectTarget: SelectTarget = linkFactory.selectTarget + rightLinkFactory.selectTarget
	
	
	// COMPUTED ---------------------------------
	
	private def linkFactory = MemberRoleLinkFactory
	private def rightLinkFactory = UserRoleRightFactory
	
	
	// IMPLEMENTED  -----------------------------
	
	override def table = linkFactory.table
	override def joinedTables = rightLinkFactory.tables
	override def joinType = JoinType.Left
	
	override def defaultOrdering = None
	
	override def nonDeprecatedCondition = linkFactory.nonDeprecatedCondition
	
	override def apply(result: Result) = {
		// Starts by grouping the rows based on the member role id
		result.split(table).flatMap { result =>
			linkFactory(result.rows.head).map { memberRole =>
				// Next parses role right links for each sub-result
				val taskLinks = rightLinkFactory(result)
				MemberRoleWithRights(memberRole, taskLinks.map { _.taskId }.toSet)
			} match {
				case Success(memberRole) => Some(memberRole)
				case Failure(exception) =>
					ErrorHandling.modelParsePrinciple.handle(exception)
					None
			}
		}
	}
}
