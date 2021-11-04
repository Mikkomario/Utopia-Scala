package utopia.citadel.database.access.many.organization

import utopia.metropolis.model.combined.organization.MemberRoleWithRights
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing multiple member roles with rights at a time
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
object DbMemberRolesWithRights extends ManyMemberRolesWithRightsAccess with NonDeprecatedView[MemberRoleWithRights]
