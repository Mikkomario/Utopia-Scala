package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.generic.model.template.HasPropertiesLike
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.view.immutable.caching.Lazy

import java.nio.file.Path
import scala.jdk.CollectionConverters.IteratorHasAsScala

/**
 * An interface for reading environment variables
 * @author Mikko Hilpinen
 * @since 01.03.2026, v2.8
 */
object Env extends HasPropertiesLike[Constant]
{
	// ATTRIBUTES   ---------------------
	
	private val lazyProperties = Lazy { newPropertiesIterator.caching }
	private val lazyPropMap = lazyProperties.map { _.iterator.map { p => p.name.toLowerCase -> p }.toMap }
	
	/**
	 * Path to the current user's home directory
	 */
	lazy val home = apply("HOME").string.map { s => s: Path }
	
	
	// COMPUTED -------------------------
	
	private def newPropertiesIterator = {
		val env = System.getenv()
		env.keySet().iterator().asScala.map { key => Constant(key, env.get(key)) }
	}
	
	
	// IMPLEMENTED  ---------------------
	
	override def propertiesIterator: Iterator[Constant] = lazyProperties.current match {
		case Some(props) => props.iterator
		case None => newPropertiesIterator
	}
	override def properties: Seq[Constant] = lazyProperties.value
	
	override def existingProperty(propName: String): Option[Constant] = {
		if (lazyProperties.isInitialized)
			lazyPropMap.value.get(propName.toLowerCase)
		else
			Option(System.getenv(propName)).map { Constant(propName, _) }
	}
	override def property(propName: String): Constant =
		existingProperty(propName).getOrElse { Constant(propName, Value.empty) }
	
	override protected def simulateValueFor(propName: String): Value = Value.empty
	
	override def contains(propName: String): Boolean = {
		if (lazyProperties.isInitialized)
			lazyPropMap.value.contains(propName.toLowerCase)
		else
			System.getenv(propName) != null
	}
	override def containsNonEmpty(propName: String): Boolean = {
		if (lazyProperties.isInitialized)
			lazyPropMap.value.get(propName).exists { _.nonEmpty }
		else
			Option(System.getenv(propName)).exists { _.nonEmpty }
	}
}
