package utopia.vigil.database.value

import utopia.vault.database.value.LazyDbValue
import utopia.vigil.database.VigilContext._
import utopia.vigil.database.access.scope.AccessScope

import scala.collection.mutable

object ScopeTarget
{
	// ATTRIBUTES   -----------------------
	
	private val cachedById = mutable.Map[Int, ScopeTarget]()
	private val cachedByKey = mutable.Map[String, ScopeTarget]()
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param scopeId ID of the targeted scope
	 * @return A scope target matching that ID
	 */
	def id(scopeId: Int): ScopeTarget = {
		if (scopeId > 0)
			cachedById.getOrElseUpdate(scopeId, new ScopeId(scopeId))
		else
			InvalidScope
	}
	/**
	 * @param key Key of the targeted scope (case-insensitive)
	 * @return A scope target matching that key
	 */
	def apply(key: String): ScopeTarget = {
		if (key.isEmpty)
			InvalidScope
		else {
			val lowerKey = key.toLowerCase
			cachedByKey.getOrElseUpdate(lowerKey, new ScopeKey(lowerKey))
		}
	}
	
	
	// NESTED   ---------------------------
	
	/**
	 * Represents an invalid scope target
	 */
	object InvalidScope extends ScopeTarget
	{
		override val isValid: LazyDbValue[Boolean] = LazyDbValue.initialized(false)
		override val id: LazyDbValue[Int] = LazyDbValue.initialized(-1)
		override val key: LazyDbValue[String] = LazyDbValue.initialized("")
	}
	
	private class ScopeId(_id: Int) extends ScopeTarget
	{
		override val id: LazyDbValue[Int] = LazyDbValue.initialized(_id)
		override val key: LazyDbValue[String] = LazyDbValue.lookUp { implicit c =>
			val key = AccessScope(_id).key.pull
			if (key.nonEmpty)
				cachedByKey += (key.toLowerCase -> this)
			key
		}
		
		override val isValid: LazyDbValue[Boolean] = key.lightMap { _.nonEmpty }
	}
	
	private class ScopeKey(_key: String) extends ScopeTarget
	{
		override val key: LazyDbValue[String] = LazyDbValue.initialized(_key)
		override val id: LazyDbValue[Int] = LazyDbValue.lookUp { implicit c =>
			AccessScope.forKey(_key).id.pull match {
				case Some(id) =>
					cachedById += (id -> this)
					id
				case None => -1
			}
		}
		
		override val isValid: LazyDbValue[Boolean] = id.lightMap { _ > 0 }
	}
}

/**
 * Used for targeting specific scopes. Data may be acquired lazily.
 * @author Mikko Hilpinen
 * @since 01.05.2026, v0.1
 */
// TODO: Add contains / handling of child scopes
trait ScopeTarget
{
	// ABSTRACT --------------------------
	
	/**
	 * @return A pointer that contains whether this is a valid scope
	 */
	def isValid: LazyDbValue[Boolean]
	/**
	 * @return A pointer that contains the ID of this scope
	 */
	def id: LazyDbValue[Int]
	/**
	 * @return A pointer that contains the key / identifier of this scope
	 */
	def key: LazyDbValue[String]
	
	
	// IMPLEMENTED  ----------------------
	
	override def toString: String = key.value
}
