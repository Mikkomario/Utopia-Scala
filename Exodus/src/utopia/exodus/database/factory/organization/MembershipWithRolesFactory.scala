package utopia.exodus.database.factory.organization

import utopia.exodus.database.model.organization.MemberRoleModel
import utopia.metropolis.model.combined.organization.MembershipWithRoles
import utopia.vault.model.immutable.Result
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.template.Deprecatable
import utopia.vault.sql.JoinType
import utopia.vault.util.ErrorHandling

import scala.util.{Failure, Success}

/**
  * Used for reading rich membership data from DB
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
object MembershipWithRolesFactory extends FromResultFactory[MembershipWithRoles] with Deprecatable
{
	// IMPLEMENTED	----------------------------
	
	override def nonDeprecatedCondition = MembershipFactory.nonDeprecatedCondition &&
		MemberRoleModel.nonDeprecatedCondition
	
	override def table = MembershipFactory.table
	
	override def joinType = JoinType.Left
	
	override def joinedTables = Vector(roleLinkTable)
	
	override def apply(result: Result) =
	{
		// Groups rows by membership id
		result.grouped(table, roleLinkTable).flatMap { case (_, membershipData) =>
			val (membershipRow, roleLinkRows) = membershipData
			// Membership must be parseable
			MembershipFactory(membershipRow) match
			{
				case Success(membership) =>
					// Adds role ids (parsed)
					val roleIds = roleLinkRows.flatMap { _(roleLinkTable)(MemberRoleModel.roleIdAttName).int }
					Some(MembershipWithRoles(membership, roleIds.toSet))
				case Failure(error) =>
					ErrorHandling.modelParsePrinciple.handle(error)
					None
			}
		}.toVector
	}
	
	
	// COMPUTED	------------------------------
	
	private def roleLinkTable = MemberRoleModel.table
}
