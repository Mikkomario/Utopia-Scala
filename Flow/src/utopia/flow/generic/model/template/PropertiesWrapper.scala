package utopia.flow.generic.model.template

import utopia.flow.generic.model.immutable.Value

/**
 * Common trait for classes which implement [[HasPropertiesLike]] by wrapping another such instance.
 * @author Mikko Hilpinen
 * @since 02.01.2026, v2.8
 */
trait PropertiesWrapper[+P <: Property] extends HasPropertiesLike[P]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return The wrapped instance which contains the exposed properties
	 */
	protected def wrapped: HasPropertiesLike[P]
	
	
	// IMPLEMENTED  ------------------------
	
	override def propertiesIterator: Iterator[P] = wrapped.propertiesIterator
	override def properties: Seq[P] = wrapped.properties
	
	override def existingProperty(propName: String): Option[P] = wrapped.existingProperty(propName)
	override def property(propName: String): P = wrapped.property(propName)
	
	override protected def simulateValueFor(propName: String): Value = Value.empty
	
	override def contains(propName: String): Boolean = wrapped.contains(propName)
	override def containsNonEmpty(propName: String): Boolean = wrapped.containsNonEmpty(propName)
	
	override def apply(propName: String): Value = wrapped.apply(propName)
	override def apply(propNames: IterableOnce[String]): Value = wrapped.apply(propNames)
}
