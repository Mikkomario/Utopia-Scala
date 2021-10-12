package utopia.metropolis.model.combined.description

import utopia.metropolis.model.stored.description.DescriptionLink

/**
  * A common trait for items with descriptions
  * @author Mikko Hilpinen
  * @since 30.6.2021, v1.1
  */
trait Described
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Descriptions linked with the wrapped item
	  */
	def descriptions: Set[DescriptionLink]
	
	
	// OTHER    -------------------------
	
	/**
	 * @param roleId Id of the targeted description role
	 * @return Description for that role, if available
	 */
	def descriptionWithRoleId(roleId: Int) =
		descriptions.find { _.description.roleId == roleId }.map { _.description }
	/**
	 * @param roleId If of the targeted description role
	 * @return Text associated with that description role, if available
	 */
	def valueOfRoleWithId(roleId: Int) = descriptionWithRoleId(roleId).map { _.text }
}
