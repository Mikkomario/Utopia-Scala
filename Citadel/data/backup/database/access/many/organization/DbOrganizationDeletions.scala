package utopia.citadel.database.access.many.organization

/**
  * Used for accessing all organization deletions
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.0
  */
object DbOrganizationDeletions extends OrganizationDeletionsAccess
{
	override protected def defaultOrdering = None
	
	override def globalCondition = None
}
