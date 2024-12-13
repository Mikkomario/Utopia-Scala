package utopia.firmament.factory

import utopia.firmament.context.HasContext
import utopia.firmament.context.base.StaticBaseContext
import utopia.firmament.model.enumeration.SizeCategory

/**
  * Common trait for (component) factories that apply static insets
  * and have access to a static component-creation context.
  * @author Mikko Hilpinen
  * @since 12.12.2024, v1.4
  */
trait StaticContextualFramedFactory[+A] extends StaticFramedFactory[A] with HasContext[StaticBaseContext]
{
	/**
	  * @param size General size of the insets to apply
	  * @return An item with symmetric insets of the specific general size applied on all sides
	  */
	def withInsets(size: SizeCategory): A = withInsets(context.scaledStackMargin(size))
}
