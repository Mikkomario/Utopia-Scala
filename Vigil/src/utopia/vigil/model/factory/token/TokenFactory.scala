package utopia.vigil.model.factory.token

import java.time.Instant

/**
  * Common trait for token-related factories which allow construction with individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait TokenFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param expires New expires to assign
	  * @return Copy of this item with the specified expires
	  */
	def withExpires(expires: Instant): A
	
	/**
	  * @param hash New hash to assign
	  * @return Copy of this item with the specified hash
	  */
	def withHash(hash: String): A
	
	/**
	  * @param name New name to assign
	  * @return Copy of this item with the specified name
	  */
	def withName(name: String): A
	
	/**
	  * @param parentId New parent id to assign
	  * @return Copy of this item with the specified parent id
	  */
	def withParentId(parentId: Int): A
	
	/**
	  * @param revoked New revoked to assign
	  * @return Copy of this item with the specified revoked
	  */
	def withRevoked(revoked: Instant): A
	
	/**
	  * @param templateId New template id to assign
	  * @return Copy of this item with the specified template id
	  */
	def withTemplateId(templateId: Int): A
}

