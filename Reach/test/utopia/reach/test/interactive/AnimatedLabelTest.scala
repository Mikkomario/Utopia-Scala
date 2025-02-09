package utopia.reach.test.interactive

import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.angular.DirectionalRotation
import utopia.paradigm.animation.Animation
import utopia.reach.component.label.image.AnimatedImageLabel
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
  * Tests animated image label
  * @author Mikko Hilpinen
  * @since 08.02.2025, v1.6
  */
object AnimatedLabelTest extends App
{
	private val window = ReachWindow.contentContextual.borderless.using(AnimatedImageLabel) { (_, labelF) =>
		labelF.once.rotating(Image.readFrom("Reach/test-images/check-box-selected.png").get,
			Animation(DirectionalRotation.clockwise.circles).sPathCurved.over(2.seconds), centerOrigin = true)
	}
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	start()
	
	window.display(centerOnParent = true)
}
