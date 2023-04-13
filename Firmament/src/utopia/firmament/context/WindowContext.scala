package utopia.firmament.context

import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.flow.operator.ScopeUsable
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Insets

object WindowContext
{
	// OTHER    ------------------------
	
	/**
	  * Creates a new window-creation context
	  * @param actorHandler        An actor handler that distributes action events for this window mouse event generators.
	  * @param resizeLogic         The logic that should be applied to window resizing.
	  *                            Default = Program = Only the program may resize windows (i.e. user can't)
	  * @param screenBorderMargins Additional margins that are placed on the screen edges, which are not covered by
	  *                            windows if at all possible.
	  *                            Default = no margins
	  * @param icon                Icon displayed on windows.
	  *                            Default = common default (see [[ComponentCreationDefaults]])
	  * @param borderless          Whether windows should be 'undecorated', i.e. have no OS headers or borders.
	  *                            Set to true if you implement your own header,
	  *                            or if you're creating temporary pop-up windows.
	  *                            Notice that without the OS header, the user can't move windows by default.
	  *                            Default = false = use OS header.
	  * @param fullScreen          Whether windows should be set to fill the whole screen whenever possible.
	  *                            Default = false.
	  * @param disableFocus        Whether windows shall not be allowed to gain focus.
	  *                            Default = false.
	  * @param ignoreScreenInsets  Whether windows should ignore the screen insets (such as the OS toolbar)
	  *                            when positioning themselves.
	  *                            Set to true if you want to cover the toolbar (in some full-screen use-cases, for example).
	  *                            Default = false.
	  * @param disableTransparency Whether windows shall not be allowed to become transparent,
	  *                            even when it would be otherwise possible.
	  *                            Default = false = transparency is enabled when possible.
	  * @return A new context
	  */
	def apply(actorHandler: ActorHandler, resizeLogic: WindowResizePolicy = Program,
	          screenBorderMargins: Insets = Insets.zero, icon: Image = ComponentCreationDefaults.windowIcon,
	          borderless: Boolean = false, fullScreen: Boolean = false, disableFocus: Boolean = false,
	          ignoreScreenInsets: Boolean = false, disableTransparency: Boolean = false): WindowContext =
		_WindowContext(actorHandler, resizeLogic, screenBorderMargins, icon, !borderless, fullScreen, !disableFocus,
			!ignoreScreenInsets, !disableTransparency)
	
	
	// NESTED   ------------------------
	
	private case class _WindowContext(actorHandler: ActorHandler, windowResizeLogic: WindowResizePolicy,
	                                  screenBorderMargins: Insets, icon: Image, windowBordersEnabled: Boolean,
	                                  fullScreenEnabled: Boolean, focusEnabled: Boolean, screenInsetsEnabled: Boolean,
	                                  transparencyEnabled: Boolean)
		extends WindowContext
	{
		override def self: WindowContext = this
		
		override def withResizeLogic(logic: WindowResizePolicy): WindowContext = copy(windowResizeLogic = logic)
		override def withScreenBorderMargins(margins: Insets): WindowContext = copy(screenBorderMargins = margins)
		override def withIcon(icon: Image): WindowContext = copy(icon = icon)
		override def withWindowBordersEnabled(enabled: Boolean): WindowContext = copy(windowBordersEnabled = enabled)
		override def withFullScreenEnabled(enabled: Boolean): WindowContext = copy(fullScreenEnabled = enabled)
		override def withFocusEnabled(enabled: Boolean): WindowContext = copy(focusEnabled = enabled)
		override def withScreenInsetsEnabled(enabled: Boolean): WindowContext = copy(screenInsetsEnabled = enabled)
		override def withTransparencyEnabled(enabled: Boolean): WindowContext = copy(transparencyEnabled = enabled)
	}
}

/**
  * Common trait for context items that specify information for window-creation
  * @author Mikko Hilpinen
  * @since 12.4.2023, v1.0
  */
trait WindowContext extends ScopeUsable[WindowContext]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Actor handler used for delivering action events for window event generation
	  */
	def actorHandler: ActorHandler
	/**
	  * @return Resize logic to use for constructed windows
	  */
	def windowResizeLogic: WindowResizePolicy
	/**
	  * @return Empty space to place around the screen borders, where windows should not enter
	  */
	def screenBorderMargins: Insets
	/**
	  * @return Window icon used
	  */
	def icon: Image
	
	/**
	  * @return Whether the created windows should have the OS borders (i.e. headers)
	  */
	def windowBordersEnabled: Boolean
	/**
	  * @return Whether the created windows should fill the whole screen whenever possible
	  */
	def fullScreenEnabled: Boolean
	/**
	  * @return Whether the created windows should be focusable
	  */
	def focusEnabled: Boolean
	/**
	  * @return Whether the created windows should avoid overlap with the screen insets, i.e. the OS tool bar
	  */
	def screenInsetsEnabled: Boolean
	/**
	  * @return Whether transparent windows should be enabled, when possible
	  */
	def transparencyEnabled: Boolean
	
	def withResizeLogic(logic: WindowResizePolicy): WindowContext
	def withScreenBorderMargins(margins: Insets): WindowContext
	def withIcon(icon: Image): WindowContext
	
	def withWindowBordersEnabled(enabled: Boolean): WindowContext
	def withFullScreenEnabled(enabled: Boolean): WindowContext
	def withFocusEnabled(enabled: Boolean): WindowContext
	def withScreenInsetsEnabled(enabled: Boolean): WindowContext
	def withTransparencyEnabled(enabled: Boolean): WindowContext
	
	
	// COMPUTED ---------------------
	
	/**
	  * @return A copy of this context that creates windows which can be resized by the user
	  */
	def resizable = mapResizeLogic { _.allowingResizeByUser }
	/**
	  * @return A copy of this context that creates windows that can be resized by the program
	  */
	def allowingProgrammaticSizeChanges = mapResizeLogic { _.allowingResizeByProgram }
	/**
	  * @return A copy of this context where user-initiated window-resizing is disabled
	  */
	def nonResizable = mapResizeLogic { _.disallowingResizeByUser }
	/**
	  * @return A copy of this context where the program is not allowed to resize windows,
	  *         unless strictly necessary
	  */
	def withoutProgrammaticSizeChanges = mapResizeLogic { _.disallowingResizeByProgram }
	
	/**
	  * @return A copy of this context that doesn't place any additional margins on the screen edges.
	  *         Screen insets may still be respected.
	  */
	def withoutScreenBorderMargins = withScreenBorderMargins(Insets.zero)
	
	/**
	  * @return A copy of this context that creates borderless (i.e. undecorated) windows
	  */
	def borderless = withWindowBordersEnabled(false)
	/**
	  * @return A copy of this context that creates decorated windows,
	  *         meaning that the windows will contain the OS header, etc.
	  */
	def decorated = withWindowBordersEnabled(true)
	
	/**
	  * @return A copy of this context that creates full-screen windows
	  */
	def fullScreen = withFullScreenEnabled(true)
	/**
	  * @return A copy of this context that creates windows that don't attempt to fill the whole screen
	  */
	def windowed = withFullScreenEnabled(false)
	
	/**
	  * @return A copy of this context that creates windows that can receive focus
	  */
	def focusable = withFocusEnabled(true)
	/**
	  * @return A copy of this context that creates windows that can't receive focus
	  */
	def nonFocusable = withFocusEnabled(false)
	
	/**
	  * @return A copy of this context that takes account screen insets (i.e. OS toolbar)
	  */
	def respectingScreenInsets = withScreenInsetsEnabled(true)
	/**
	  * @return A copy of this context that ignores screen insets (i.e. OS toolbar)
	  */
	def ignoringScreenInsets = withScreenInsetsEnabled(false)
	
	/**
	  * @return A copy of this context that allows transparent window creation, when possible and appropriate
	  */
	def transparent = withTransparencyEnabled(true)
	/**
	  * @return A copy of this context that only creates non-transparent windows
	  */
	def nonTransparent = withTransparencyEnabled(false)
	
	/**
	  * @return A copy of this context that constructs decorated windows that don't attempt to fill the whole creen
	  */
	def decoratedWindows = decorated.windowed
	/**
	  * @return A copy of this context that creates borderless full-screen windows
	  */
	def borderlessFullScreen = borderless.fullScreen.withoutScreenBorderMargins.nonResizable
	/**
	  * @return A copy of this context that doesn't avoid the screen edges, not even the OS toolbar
	  */
	def ignoringAllInsets = ignoringScreenInsets.withoutScreenBorderMargins
	
	
	// OTHER    ---------------------
	
	def mapResizeLogic(f: WindowResizePolicy => WindowResizePolicy) =
		withResizeLogic(f(windowResizeLogic))
	def mapScreenBorderMargins(f: Insets => Insets) = withScreenBorderMargins(f(screenBorderMargins))
	def mapIcon(f: Image => Image) = withIcon(f(icon))
}
