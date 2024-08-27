package utopia.reach.component.button

import utopia.firmament.model.HotKey
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.keyboard.Key
import utopia.genesis.handling.event.keyboard.Key.{Enter, Esc, Space}
import utopia.reach.focus.FocusListener

/**
  * Common trait for button factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ButtonSettingsLike[+Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * A pointer that determines whether this button is interactive or not
	  */
	// TODO: Rename to enabledFlag
	def enabledPointer: Flag
	/**
	  * The keys used for triggering this button even when it doesn't have focus
	  */
	def hotKeys: Set[HotKey]
	/**
	  * Focus listeners that should receive focus events from this button
	  */
	def focusListeners: Seq[FocusListener]
	
	/**
	  * A pointer that determines whether this button is interactive or not
	  * @param p New enabled pointer to use.
	  *          A pointer that determines whether this button is interactive or not
	  * @return Copy of this factory with the specified enabled pointer
	  */
	def withEnabledPointer(p: Changing[Boolean]): Repr
	/**
	  * Focus listeners that should receive focus events from this button
	  * @param listeners New focus listeners to use.
	  *                  Focus listeners that should receive focus events from this button
	  * @return Copy of this factory with the specified focus listeners
	  */
	def withFocusListeners(listeners: Seq[FocusListener]): Repr
	/**
	  * The keys used for triggering this button even when it doesn't have focus
	  * @param keys New hot keys to use.
	  *             The keys used for triggering this button even when it doesn't have focus
	  * @return Copy of this factory with the specified hot keys
	  */
	def withHotKeys(keys: Set[HotKey]): Repr
	
	
	// COMPUTED --------------------
	
	/**
	 * @return Copy of this factory that builds buttons that are triggered by pressing the enter key
	 */
	def triggeredWithEnter = triggeredWith(Enter)
	/**
	 * @return Copy of this factory that builds buttons that are triggered by pressing the space-bar
	 */
	def triggeredWithSpace = triggeredWith(Space)
	/**
	 * @return Copy of this factory that builds buttons that are triggered by pressing the escape key
	 */
	def triggeredWithEscape = triggeredWith(Esc)
	
	
	// OTHER	--------------------
	
	def mapEnabledPointer(f: Mutate[Flag]) = withEnabledPointer(f(enabledPointer))
	def mapFocusListeners(f: Seq[FocusListener] => Seq[FocusListener]) =
		withFocusListeners(f(focusListeners))
	def mapHotKeys(f: Set[HotKey] => Set[HotKey]) = withHotKeys(f(hotKeys))
	
	/**
	  * @param hotKey A new hotkey to trigger this button
	  * @return Copy of this factory that uses the specified hotkey
	  */
	def withHotKey(hotKey: HotKey) = mapHotKeys { _ + hotKey }
	/**
	  * @param keys Additional hotkeys for triggering this button
	  * @return Copy of this factory with the specified hotkeys assigned
	  */
	def withAdditionalHotKeys(keys: IterableOnce[HotKey]) = mapHotKeys { _ ++ keys }
	/**
	  * @param keyIndex An KeyIndex (see [[java.awt.event.KeyEvent]]) of the targeted key
	  * @return Copy of this factory that uses the specified key as a hotkey
	  */
	@deprecated("Please use .triggeredWith(Key) instead", "v1.3")
	def triggeredWithKeyIndex(keyIndex: Int) = triggeredWith(Key(keyIndex))
	/**
	  * @param key A keyboard key that should trigger this button (even when this button doesn't have focus)
	  * @return Copy of this factory that uses the specified key as a hotkey
	  */
	def triggeredWith(key: Key) = withHotKey(HotKey(key))
	
	/**
	  * @param listener A focus listener
	  * @return Copy of this factory that assigns the specified focus listener
	  */
	def withFocusListener(listener: FocusListener) = mapFocusListeners { _ :+ listener }
}

object ButtonSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing buttons
  * @param enabledPointer A pointer that determines whether this button is interactive or not
  * @param hotKeys        The keys used for triggering this button even when it doesn't have focus
  * @param focusListeners Focus listeners that should receive focus events from this button
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ButtonSettings(enabledPointer: Flag = AlwaysTrue, hotKeys: Set[HotKey] = Set(),
                          focusListeners: Seq[FocusListener] = Empty)
	extends ButtonSettingsLike[ButtonSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withEnabledPointer(p: Changing[Boolean]) = copy(enabledPointer = p)
	override def withFocusListeners(listeners: Seq[FocusListener]) = copy(focusListeners = listeners)
	override def withHotKeys(keys: Set[HotKey]) = copy(hotKeys = keys)
}

/**
  * Common trait for factories that wrap a button settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ButtonSettingsWrapper[+Repr] extends ButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ButtonSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def enabledPointer = settings.enabledPointer
	override def focusListeners = settings.focusListeners
	override def hotKeys = settings.hotKeys
	
	override def withEnabledPointer(p: Changing[Boolean]) = mapSettings { _.withEnabledPointer(p) }
	override def withFocusListeners(listeners: Seq[FocusListener]) =
		mapSettings { _.withFocusListeners(listeners) }
	override def withHotKeys(keys: Set[HotKey]) = mapSettings { _.withHotKeys(keys) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ButtonSettings => ButtonSettings) = withSettings(f(settings))
}