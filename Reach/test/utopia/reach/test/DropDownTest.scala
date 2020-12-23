package utopia.reach.test

import utopia.flow.util.FileExtensions._
import utopia.genesis.image.Image
import utopia.reach.component.input.DropDown
import utopia.reach.container.{Framing, ReachCanvas, Stack}
import utopia.reflection.shape.LengthExtensions._

/**
  * A test application with drop down fields
  * @author Mikko Hilpinen
  * @since 23.12.2020, v1
  */
object DropDownTest extends App
{
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val arrowImage = Image.readFrom("Reflection/test-images/arrow-black-48dp.png")
	// val expandIcon = arrowImage.map { _.rota }
	
	/*
	ReachCanvas(cursors) { hierarchy =>
		Framing(hierarchy).buildFilledWithContext(baseContext, colorScheme.gray.light, Stack)
			.apply(margins.medium.any.square) { stackF =>
				stackF.mapContext { _.forTextComponents }.build(DropDown).column(areRelated = true) { ddF =>
					//ddF.simple()
					???
				}
			}
	}*/
}
