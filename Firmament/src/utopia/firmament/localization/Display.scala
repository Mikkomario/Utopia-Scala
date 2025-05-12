package utopia.firmament.localization

import utopia.firmament.localization.Display.{InterpolatingDisplay, NamedInterpolatingDisplay, OptionDisplay}
import utopia.flow.collection.immutable.Single
import utopia.flow.time.TimeExtensions._

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import scala.language.implicitConversions

object Display
{
	// TYPES	----------------------
	
	/**
	  * A function used for converting an item into a local string format
	  */
	type Transform[-A] = A => LocalString
	/**
	  * A function used for localizing strings
	  */
	type Localize = LocalString => LocalizedString
	
	
	// ATTRIBUTES	------------------
	
	/**
	  * A display function that preserves strings which are already localized
	  */
	val noOp: Display[LocalizedString] = _Identity
	/**
	  * @return A display function that converts the items to strings using .toString, skipping localization.
	  *         Does not specify language.
	  * @see [[localIdentity]]
	  */
	lazy val identity = localIdentity(Language.none)
	
	/**
	  * A display function that shows hours and minutes, like '13:26'
	  */
	lazy val hoursAndMinutes = time(DateTimeFormatter.ofPattern("HH:mm"))
	/**
	  * A display function that shows hours, minutes and seconds. Like '03:22:42'
	  */
	lazy val hoursMinutesAndSeconds = time(DateTimeFormatter.ofPattern("HH:mm:ss"))
	/**
	  * A display function that shows day of month of year, like '13.05.2025'
	  */
	lazy val date = time(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
	/**
	  * A display function that shows day of month of year and hour and minute information. Like '13.05.2025 13:26'
	  */
	lazy val dateAndTime = time(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
	
	
	// COMPUTED ----------------------
	
	/**
	  * @param language Implicit language applicable to the converted items
	  * @return A display function that converts the items to strings using .toString, skipping localization
	  */
	def localIdentity(implicit language: Language): Display[Any] = apply {
		case localized: LocalizedString => localized
		case local: LocalString => local.skipLocalization
		case a => LocalString.in(language)(a.toString).skipLocalization
	}
	/**
	  * @param language Implicit language of the displayed items
	  * @param localizer Implicit localizer to apply
	  * @return A display function that converts items using toString and localizes them using the specified localizer
	  */
	def localize(implicit language: Language, localizer: Localizer): Display[Any] = apply {
		case localized: LocalizedString => localized
		case local: LocalString => localizer(local)
		case a => localizer(LocalString(a.toString, language))
	}
	/**
	  * @param localizer Utilized localizer (implicit)
	  * @return A display function for local strings
	  */
	def localizeLocal(implicit localizer: Localizer) = apply(localizer.apply)
	
	/**
	  * A DisplayFunction that displays the item as is, no transformation or localization applied
	  */
	@deprecated("Renamed to .identity", "v1.5")
	def raw = identity
	/**
	  * A displayFunction that displays option's value as is and an empty string for empty values
	  */
	@deprecated("Please use .identity.optional instead", "v1.5")
	def rawOption = identity.optional
	
	
	@deprecated("Renamed to .hoursAndMinutes", "v1.5")
	def hhmm = hoursAndMinutes
	@deprecated("Renamed to .hoursMinutesAndSeconds", "v1.5")
	def hhmmss = hoursMinutesAndSeconds
	@deprecated("Renamed to .date", "v1.5")
	def ddmmyyyy = date
	@deprecated("Please use .dateAndTime instead", "v1.5")
	def ddmmyyyyhhmm = dateAndTime
	
	
	// IMPLICIT	----------------------
	
	/**
	  * @param formatter A date-time formatter
	  * @return A display function for dates, times, date-times & instants
	  */
	implicit def time(formatter: DateTimeFormatter): Display[TemporalAccessor] =
		new TimeDisplay(formatter)
	
	/**
	  * @param f A function to wrap. Converts items into localized strings.
	  * @tparam A Type of accepted items
	  * @return A display function wrapping the specified function
	  */
	implicit def apply[A](f: A => LocalizedString): Display[A] = new _Display(f)
	
	
	// OTHER	----------------------
	
	/**
	  * @param transform A function that converts items into (local) strings
	  * @tparam A Source item type
	  * @return A new display function that applies the specified transformation, but skips localization
	  */
	def skipLocalization[A](transform: Transform[A]) =
		apply[A]{ a => transform(a).skipLocalization }
	
	/**
	  * @param transform A function that converts an item into a (local) string format
	  * @param localizer Implicit localizer utilized
	  * @tparam A Type of displayed items
	  * @return A display function that converts the item using 'transform'
	  *         and localizes it using the implicit 'localizer'.
	  */
	def mapAndLocalize[A](transform: Transform[A])(implicit localizer: Localizer): Display[A] =
		mapAndLocalizeWith(transform)(localizer.apply)
	/**
	  * Creates a new display function from two separate functions
	  * @param transform A transform function that transforms an item to string
	  * @param localize  A localization function that localizes the produced string
	  * @tparam A The source item type
	  * @return A new display function
	  */
	def mapAndLocalizeWith[A](transform: Transform[A])(localize: Localize): Display[A] =
		apply[A] { item => localize(transform(item)) }
	
	/**
	  * @param string A string to which values are placed through interpolation
	  * @return A display function that places the displayed items within the specified (localized) string
	  */
	def interpolateTo(string: LocalizedString): Display[Any] = apply { string.interpolate(_) }
	
	/**
	  * Wraps a function as a display function
	  * @param handle A function for handling conversion and localization
	  * @tparam A Type of converted item
	  * @return A new display function
	  */
	@deprecated("Please use .apply(...) instead", "v1.5")
	def wrap[A](handle: A => LocalizedString) = apply(handle)
	
	/**
	  * This displayfunction takes in local strings and localizes them with specified function
	  * @param localize A localization function
	  * @return A new display function
	  */
	@deprecated("Please use .apply(...) instead", "v1.5")
	def stringLocalized(localize: Localize) = Display(localize)
	/**
	  * This displayFunction takes in local strings and localizes them using the specified localizer (implicit)
	  * @param localizer A localizer that does the localization (implicit)
	  * @return A new display function
	  */
	@deprecated("Please use .localizeLocal instead", "v1.5")
	def stringLocalized()(implicit localizer: Localizer): Display[LocalString] =
		stringLocalized(localizer.localize)
	
	/**
	  * This display function uses an item's toString function to transform it to string and then a localization
	  * function to localize the produced string
	  * @param localize A localization function
	  * @return A new display function
	  */
	@deprecated("Deprecated for removal", "v1.5")
	def localizeOnly(localize: Localize)(implicit language: Language) =
		apply[Any]{ a => localize(LocalString(a.toString)) }
	
	/**
	  * This display function only transforms an item into string form but doesn't then perform a localization on the
	  * string
	  * @param transform A transform function for transforming the item to local string format
	  * @tparam A Source item type
	  * @return A new display function
	  */
	@deprecated("Please use .skipLocalization(...) instead", "v1.5")
	def noLocalization[A](transform: Transform[A]) = skipLocalization(transform)
	
	/**
	  * This display function transforms an item into string format and then localizes it using a localizer (implicit)
	  * @param transform A transform function for transforming the item to string format
	  * @param localizer A localizer for localization (implicit)
	  * @tparam A Source item type
	  * @return A new display function
	  */
	@deprecated("Please use .mapAndLocalize instead", "v1.5")
	def localized[A](transform: Transform[A])(implicit localizer: Localizer) =
		mapAndLocalize(transform)
	/**
	  * This display function uses toString to convert an item to string and then localizes that string using a
	  * localizer (implicit)
	  * @param localizer A localizer that handles localization (implicit)
	  * @return A new display function
	  */
	@deprecated("Please use .localize instead", "v1.5")
	def localized()(implicit language: Language, localizer: Localizer) =
		localizeOnly(localizer.localize)
	
	/**
	  * Creates a new display function that uses string interpolation
	  * @param transform A transform function that converts an item to a local string
	  * @param localize A function that localizes a string
	  * @param getArgs A function for retrieving interpolation arguments (as key value pairs)
	  * @tparam A source parameter type
	  * @return A new display function that interpolates the localized string
	  */
	@deprecated("Please use .mapAndLocalizeWith(...).interpolateNamed(...) instead", "v1.5")
	def interpolating[A](transform: Transform[A])(localize: Localize)(getArgs: A => Map[String, String]) =
		apply[A] { a => localize(transform(a)).interpolateNamed(getArgs(a)) }
	/**
	  * Creates a new display function that uses string interpolation
	  * @param transform A transform function that converts an item to a local string
	  * @param getArgs A function for retrieving interpolation arguments (as key value pairs)
	  * @param localizer A localizer that localizes the local string (implicit)
	  * @tparam A source parameter type
	  * @return A new display function that interpolates the localized string
	  */
	@deprecated("Please use .mapAndLocalize(...).interpolateNamed(...) instead", "v1.5")
	def interpolatingLocalized[A](transform: Transform[A])(getArgs: A => Map[String, String])
	                             (implicit localizer: Localizer) =
		mapAndLocalize(transform).interpolateNamed(getArgs)
	/**
	  * Creates a new display function that simply interpolates a string
	  * @param string The string that will be interpolated using parameter item (single argument)
	  * @return A new display function that uses a static string with interpolation
	  */
	@deprecated("Please use interpolateTo(LocalizedString)", "v1.5")
	def interpolating(string: LocalizedString) = interpolateTo(string)
	
	/**
	 * @param whenDefined A display function to use when an item has been defined
	 * @param whenNotDefined A string to display for undefined (None) items
	 * @tparam A Type of displayed values, when defined
	 * @return A new display function that works with optional values
	 */
	@deprecated("Please use whenDefined.optional instead", "v1.5")
	def option[A](whenDefined: Display[A], whenNotDefined: LocalizedString = LocalizedString.empty) =
		whenDefined.orElse(whenNotDefined)
	
	/**
	  * This display function is used for displaying time in a specific format
	  * @param formatter A date time formatter
	  * @return A display function for time elements
	  */
	@deprecated("Please use .time(DateTimeFormatter) instead", "v1.5")
	def forTime(formatter: DateTimeFormatter) = time(formatter)
	
	
	// NESTED   ---------------------------
	
	private object _Identity extends Display[LocalizedString]
	{
		override def apply(item: LocalizedString): LocalizedString = item
	}
	
	private class _Display[-A](f: A => LocalizedString) extends Display[A]
	{
		override def apply(item: A): LocalizedString = f(item)
	}
	
	private class TimeDisplay(formatter: DateTimeFormatter) extends Display[TemporalAccessor]
	{
		override def apply(item: TemporalAccessor): LocalizedString = item match {
			case i: Instant => LocalizedString.noLanguage(i.toStringWith(formatter))
			case a => LocalizedString.noLanguage(formatter.format(a))
		}
	}
	
	private class OptionDisplay[-A](wrapped: Display[A], onEmpty: LocalizedString)
		extends Display[Option[A]]
	{
		override def apply(item: Option[A]): LocalizedString = item match {
			case Some(item) => wrapped(item)
			case None => onEmpty
		}
	}
	
	private class NamedInterpolatingDisplay[-A](wrapped: Display[A], f: A => Map[String, Any])
		extends Display[A]
	{
		override def apply(item: A): LocalizedString = wrapped(item).interpolateNamed(f(item))
	}
	private class InterpolatingDisplay[-A](wrapped: Display[A], f: A => Seq[Any])
		extends Display[A]
	{
		override def apply(item: A): LocalizedString = wrapped(item).interpolateAll(f(item))
	}
}

/**
  * Common trait for functions that are used for converting data into localized text format
  * @author Mikko Hilpinen
  * @since 22.4.2019, Reflection v1+
  */
trait Display[-A]
{
	// ABSTRACT ---------------------------
	
	/**
	  * Displays an item
	  * @param item The source item
	  * @return A localized display version for the item
	  */
	def apply(item: A): LocalizedString
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return A copy of this function (applicable to optional items), which yields an empty string for None.
	  */
	def optional = orElse(LocalizedString.empty)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param default String displayed on empty (None) items
	  * @return A copy of this function (applicable to optional items), which yields 'default' for None.
	  */
	def orElse(default: LocalizedString): Display[Option[A]] = new OptionDisplay[A](this, default)
	
	/**
	  * @param f A function that acquires named interpolation parameters from the displayed items
	  * @tparam B Type of accepted items
	  * @return A display function that adds interpolation to this function's results
	  */
	def interpolateNamed[B <: A](f: B => Map[String, Any]): Display[B] =
		new NamedInterpolatingDisplay[B](this, f)
	/**
	  * @param f A function that acquires interpolation parameters from the displayed items
	  * @tparam B Type of accepted items
	  * @return A display function that adds interpolation to this function's results
	  */
	def interpolateAll[B <: A](f: B => Seq[Any]): Display[B] = new InterpolatingDisplay[B](this, f)
	/**
	  * @param f A function that acquires an interpolation parameter from each displayed item
	  * @tparam B Type of accepted items
	  * @return A display function that adds interpolation to this function's results
	  */
	def interpolate[B <: A](f: B => Any): Display[B] = interpolateAll { a => Single(f(a)) }
}
