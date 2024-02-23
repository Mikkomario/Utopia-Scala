package utopia.reach.test

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.image.ButtonImageEffect.ChangeSize
import utopia.firmament.image.SingleColorIcon
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.{Flag, ResettableFlag}
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorRole
import utopia.paradigm.transform.Adjustment
import utopia.reach.component.button.image.ViewImageButton
import utopia.reach.container.multi.Stack
import utopia.reach.window.ReachWindow

import java.nio.file.Path

/**
  * Tests image buttons with custom color overlay, highlighting and size-changes
  * @author Mikko Hilpinen
  * @since 23/01/2024, v1.2.1
  */
object ColoredImageButtonTest extends App
{
	import ReachTestContext._
	
	ComponentCreationDefaults.asButtonImageEffects :+= ChangeSize()(Adjustment(0.1))
	
	private val onFlag = ResettableFlag()
	private val colorP = onFlag.map[ColorRole] { if (_) ColorRole.Secondary else ColorRole.Primary }
	private val pressedFlag = Flag()
	
	private val iconsDir: Path = "Reach/test-images"
	private val offIcon = SingleColorIcon(Image.readFrom(iconsDir/"check-box-empty.png").get)
	private val onIcon = SingleColorIcon(Image.readFrom(iconsDir/"check-box-selected.png").get)
	
	private val window = ReachWindow.contentContextual.using(Stack) { (_, stackF) =>
		stackF.centeredRow.build(ViewImageButton) { buttonF =>
			val closeButton = buttonF.coloredIcon(
				Fixed(SingleColorIcon(Image.readFrom(iconsDir/"close.png").get)),
				colorP) {
				println("Pressed")
				pressedFlag.set()
			}
			val onButton = buttonF.icon(onFlag.lightSwitch(offIcon, onIcon)) {
				println("On button pressed")
				onFlag.switch()
			}
			
			Vector(closeButton, onButton)
		}
	}
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	
	pressedFlag.onceSet { window.close() }
	
	start()
	window.display(centerOnParent = true)
}
