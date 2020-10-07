package utopia.reflection.test

import utopia.reflection.color.ColorRole.{Gray, Primary}
import utopia.reflection.color.ColorShade.Light
import utopia.reflection.component.context.{BaseContext, ColorContext, TextContext}
import utopia.reflection.component.reach.label.MutableTextLabel
import utopia.reflection.container.reach.Framing
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.shape.LengthExtensions._

/**
  * A simple test for the reach component implementation
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
object ReachComponentTest extends App
{
	import TestContext._
	
	val result = ReachCanvas { canvasHierarchy =>
		// [MutableTextLabel, Unit, ColorContext, BaseContext]
		// FIXME: Not yet working with implicit context types
		/*
		val (framing, label) = Framing.withBackground(canvasHierarchy, colorScheme.gray.light,
			StackInsets.symmetric(margins.medium.any))
		{
			(framingH, c: ColorContext) =>
				implicit val context: TextContext = c.forTextComponents(Alignment.Center)
				MutableTextLabel.contextualWithBackground(framingH, "Hello!", Primary)
		}(baseContext).toTuple
		
		framing -> label*/
		???
	}
}
