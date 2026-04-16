package utopia.reach.test.interactive

import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Delay
import utopia.flow.collection.immutable.Single
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.{ResettableFlag, SettableFlag}
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.genesis.image.Image
import utopia.paradigm.angular.DirectionalRotation
import utopia.paradigm.animation.Animation
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.label.image.{LoadingLabelConstructor, ViewImageAndTextLabel}
import utopia.reach.container.multi.ViewStack
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

import scala.util.Random

/**
 * Tests loading ImageAndTextLabel
 * @author Mikko Hilpinen
 * @since 15.04.2026, v1.7.2
 */
object LoadingLabelsTest extends App
{
	// ATTRIBUTES   -----------------------
	
	private val loadingImage = Image.readFrom("Reach/test-images/close.png").get
	private val completionIcon = SingleColorIcon(Image.readFrom("Reach/test-images/check-box-selected.png").get)
	private implicit val createLoadingLabel: LoadingLabelConstructor =
		_.withCustomBackgroundDrawer(BackgroundDrawer(Color.red)).center.withoutInsets
			.rotating(loadingImage, Animation(DirectionalRotation.clockwise.circles).over(2.seconds),
				centerOrigin = true)
				
	private val processesP = Pointer.eventful.seq[Int](Single(1))
	
	private val completionFlag = SettableFlag()
	
	private val window = ReachWindow.contentContextual.borderless.revalidatingAsync
		.using(Framing) { (_, framingF) =>
			framingF.small.build(ViewStack) { stackF =>
				stackF.related.trailing.mapPointer(processesP, ViewImageAndTextLabel) { (labelF, contentP, index) =>
					val completionFlag = ResettableFlag()
					val label = labelF.withBackground(Secondary).withLoadingFlag(!completionFlag).withoutInsets
						.icon(contentP, Fixed(completionIcon))
					
					label.addHierarchyListener { attached => println(s"Label #$index attached: $attached") }
					contentP.addContinuousListenerAndSimulateEvent(-1) { _ =>
						completionFlag.reset()
						Delay(5.seconds) { completionFlag.set() }
					}
					
					label
				}
			}
		}
	
	
	// APP CODE ---------------------------
	
	processesP.addListener { e => println(s"[${ e.newValue.mkString(", ") }]") }
	KeyboardEvents += KeyStateListener.pressed.anyDigit { e =>
		e.digit.foreach { count =>
			println(s"Displaying $count labels")
			processesP.update { processes =>
				if (processes.size >= count)
					processes.take(count)
				else
					processes :++ Vector.fill(count - processes.size) { Random.nextInt() }
			}
		}
	}
	
	println("Starting")
	start()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	
	Delay(10.seconds) { completionFlag.set() }
	
	window.closeFuture.waitFor()
	println("Done")
}
