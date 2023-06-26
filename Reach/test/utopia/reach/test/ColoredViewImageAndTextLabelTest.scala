package utopia.reach.test

import utopia.firmament.image.SingleColorIcon
import utopia.flow.async.process.Loop
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.image.Image
import utopia.paradigm.color.Color
import utopia.reach.component.label.image.ViewImageAndTextLabel
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow

import java.nio.file.Paths

/**
  * Tests background drawing and view image-and-text label
  * @author Mikko Hilpinen
  * @since 26.6.2023, v1.1
  */
object ColoredViewImageAndTextLabelTest extends App
{
	import ReachTestContext._
	
	val images = Paths.get("Reach/test-images")
	val options = Vector(
		SingleColorIcon(Image.readFrom(images/"check-box-empty.png").get) -> "Empty",
		SingleColorIcon(Image.readFrom(images/"check-box-selected.png").get) -> "Selected",
		SingleColorIcon.empty -> "Nothing"
	)
	val indexPointer = new PointerWithEvents(0)
	val itemPointer = indexPointer.map { options(_)._2 }
	val imagePointer = indexPointer.map { options(_)._1 }
	
	val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.build(ViewImageAndTextLabel) { labelF =>
			val f = labelF.withBackground(Color.yellow)
			println(f.customDrawers)
			f.icon(itemPointer, imagePointer)
		}
	}
	
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
	
	Loop.regularly(2.seconds, waitFirst = true) { indexPointer.update { i => if (i >= options.size - 1) 0 else i + 1 } }
}
