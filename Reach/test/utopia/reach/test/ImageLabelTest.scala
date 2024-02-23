package utopia.reach.test

import utopia.flow.parse.file.FileExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.angular.Rotation
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.label.image.ImageLabel
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow

/**
  * Tests image-drawing
  * @author Mikko Hilpinen
  * @since 23/02/2024, v1.3
  */
object ImageLabelTest extends App
{
	import ReachTestContext._
	
	private val img = Image.readFrom("Reach/test-images/check-box-selected.png").get.cropped.roundSize.withCenterOrigin
	
	private val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.build(ImageLabel) { labelF =>
			labelF.scaled(2.0).rotated(Rotation.degrees(22.5).clockwise).withBackground(Secondary)(img)
		}
	}
	
	window.display(centerOnParent = true)
	start()
}
