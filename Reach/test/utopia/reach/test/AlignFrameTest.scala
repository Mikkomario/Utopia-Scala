package utopia.reach.test

import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.label.image.ViewImageAndTextLabel
import utopia.reach.container.wrapper.AlignFrame
import utopia.reach.window.ReachWindow

import java.nio.file.Paths

/**
  * Used for testing fixed align frames
  * @author Mikko Hilpinen
  * @since 26.6.2023, v1.1
  */
object AlignFrameTest extends App
{
	import ReachTestContext._
	
	val contentPointer = new PointerWithEvents(1)
	val imgPointer = Fixed(SingleColorIcon(
		Image.readOrEmpty(Paths.get("Reach/test-images/check-box-selected.png"))
	))
	
	val window = ReachWindow.contentContextual.withResizeLogic(User).using(AlignFrame) { (_, frameF) =>
		frameF.center.build(ViewImageAndTextLabel) {
			_.withBackground(Secondary).icon(contentPointer, imgPointer,
				DisplayFunction.noLocalization { i: Any => s"Label $i" })
		}
	}
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	window.display(centerOnParent = true)
	start()
	
	Loop.regularly(0.05.seconds) {
		contentPointer.update { i =>
			if (i < 199)
				i + 1
			else
				1
		}
	}
}
