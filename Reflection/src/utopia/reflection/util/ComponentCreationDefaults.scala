package utopia.reflection.util

import java.util.concurrent.TimeUnit
import utopia.flow.time.TimeExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.genesis.util.Fps
import utopia.paradigm.shape.shape2d.Size

/**
  * A container for mutable globals that are used as default values in component creation. This object shouldn't be
  * relied on for specific component settings, as those are usually handled either in component creation context
  * or by specific component creation parameters. These defaults are generally used only when no other value has
  * been provided and no changes in defaults are taken into account after components have already been created.
  * @author Mikko Hilpinen
  * @since 22.8.2020, v1.2
  */
object ComponentCreationDefaults
{
	/**
	  * The maximum refresh rate for animations by default. This limits the amount of component hierarchy revalidations
	  * and repaint calls from animated components inside the action loop. Default value is 90 Hz (times per second).
	  */
	var maxAnimationRefreshRate = Fps(90)
	
	/**
	  * The default transition duration for animated transitions. Default value is 0.25 seconds.
	  */
	var transitionDuration = 0.25.seconds
	
	/**
	  * The scrolling friction that should be used by default. Friction in this context means how fast a scrolling
	  * stops by itself, the greater the friction, the faster the scrolling stops. Default value is 2000 pixels/s&#94;2
	  */
	var scrollFriction = LinearAcceleration(2000)(TimeUnit.SECONDS)
	
	/**
	  * The width of scroll bars by default (in pixels). The default value is 24 px. It is recommended to overwrite
	  * this value with one relative to display resolution.
	  */
	var scrollBarWidth = 24
	
	/**
	  * The amount of scrolling applied by default for every mouse wheel "click". Default value is 32 px.
	  * It is recommended to overwrite this value with one relative to display resolution.
	  */
	var scrollAmountPerWheelClick = 32.0
	
	/**
	  * Icon used in frames by default. Default = standard java icon.
	  */
	var windowIcon = Image.empty
	
	/**
	  * How often should a text field caret change its visibility when idle
	  */
	var caretBlinkFrequency = 0.5.seconds
	
	/**
	  * A scaling modifier applied to radio button sizes when they're created
	  */
	var radioButtonScalingFactor = 1.0
	
	/**
	  * Whether a filled style should be used in input fields (true) or an outline style should be used (false)
	  */
	var useFillStyleFields = true
}
