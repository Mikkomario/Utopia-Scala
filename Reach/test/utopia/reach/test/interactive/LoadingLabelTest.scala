package utopia.reach.test.interactive

import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Delay
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.genesis.image.Image
import utopia.paradigm.angular.{DirectionalRotation, Rotation}
import utopia.paradigm.animation.Animation
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.label.image.{LoadingLabelConstructor, ViewImageAndTextLabel}
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
 * Tests loading ImageAndTextLabel
 * @author Mikko Hilpinen
 * @since 15.04.2026, v1.7.2
 */
object LoadingLabelTest extends App
{
	// ATTRIBUTES   -----------------------
	
	private val loadingImage = Image.readFrom("Reach/test-images/close.png").get
	private val completionIcon = SingleColorIcon(Image.readFrom("Reach/test-images/check-box-selected.png").get)
	private implicit val createLoadingLabel: LoadingLabelConstructor =
		_.withCustomBackgroundDrawer(BackgroundDrawer(Color.red)).center.withoutInsets
			.rotating(loadingImage, Animation(DirectionalRotation.clockwise.circles).over(2.seconds),
				centerOrigin = true)
	private val completionFlag = SettableFlag()
	
	private val window = ReachWindow.contentContextual.borderless.using(Framing) { (_, framingF) =>
		framingF.small.build(ViewImageAndTextLabel) { labelF =>
			labelF.withBackground(Secondary).withLoadingFlag(!completionFlag).icon(Fixed("Test"), Fixed(completionIcon))
		}
	}
	
	
	// APP CODE ---------------------------
	
	println("Starting")
	start()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	
	Delay(10.seconds) { completionFlag.set() }
	
	window.closeFuture.waitFor()
	println("Done")
}
