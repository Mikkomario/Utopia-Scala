package utopia.metropolis.model.combined.description

import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper

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
	def descriptions: Set[LinkedDescription]
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Descriptions of this instance in a map format where the keys are description role ids and values
	 *         are description texts
	 */
	def descriptionsMap =
		descriptions.map { link => link.description.roleId -> link.description.text }.toMap
	
	
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
	def textOfRoleWithId(roleId: Int) = descriptionWithRoleId(roleId).map { _.text }
	
	/**
	  * @param role A description role
	  * @return This item's description of that role. None if not found.
	  */
	def description(role: DescriptionRoleIdWrapper) = descriptionWithRoleId(role.id)
	/**
	  * @param descriptionRole A description role
	  * @return This item's description of that role as text. None if not found.
	  */
	def apply(descriptionRole: DescriptionRoleIdWrapper) = textOfRoleWithId(descriptionRole.id)
	/**
	 * @param primaryRole Primary searched description role
	 * @param secondaryRole Role used as backup
	 * @param moreRoles More backup roles
	 * @return The first available description text from those roles
	 */
	def apply(primaryRole: DescriptionRoleIdWrapper, secondaryRole: DescriptionRoleIdWrapper,
	          moreRoles: DescriptionRoleIdWrapper*): Option[String] =
		(Vector(primaryRole, secondaryRole) ++ moreRoles).findMap(apply)
	
	/**
	 * @param role A description role
	 * @return Whether this item has that role described
	 */
	def has(role: DescriptionRoleIdWrapper) = descriptions.exists { _.description.roleId == role.id }
}
