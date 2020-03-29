package utopia.reflection.localization

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

import scala.language.implicitConversions
import utopia.flow.util.TimeExtensions._

object DisplayFunction
{
	// TYPES	----------------------
	
	type Transform[A] = A => LocalString
	type Localize = LocalString => LocalizedString
	
	
	// ATTRIBUTES	------------------
	
	/**
	  * A DisplayFunction that displays the item as is, no transformation or localization applied
	  */
	val raw = noLocalization[Any] { a => LocalString(a.toString) }
	
	/**
	  * A displayFunction that displays option's value as is and an empty string for empty values
	  */
	val rawOption = noLocalization[Option[Any]] { _.map { a => LocalString(a.toString) }.getOrElse(LocalString.empty) }
	
	/**
	  * A display function that shows hours and minutes, like '13:26'
	  */
	val hhmm = forTime(DateTimeFormatter.ofPattern("HH:mm"))
	
	/**
	  * A display function that shows hours, minutes and seconds. Like '03:22:42'
	  */
	val hhmmss = forTime(DateTimeFormatter.ofPattern("HH:mm:ss"))
	
	/**
	  * A display function that shows day of month of year, like '13.05.2025'
	  */
	val ddmmyyyy = forTime(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
	
	/**
	  * A display function that shows day of month of year and hour and minute information. Like '13.05.2025 13:26'
	  */
	val ddmmyyyyhhmm = forTime(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
	
	
	// IMPLICIT	----------------------
	
	/**
	  * Converts a function to displayfunction
	  * @param f A function that converts an item to a localized string
	  * @tparam A The source item type
	  * @return A displayfunction that uses the specified function
	  */
	implicit def functionToDisplayFunction[A](f: A => LocalizedString): DisplayFunction[A] = new DisplayFunction(f)
	
	
	// OTHER	----------------------
	
	/**
	  * Creates a new display function from two separate functions
	  * @param transform A transform function that transforms an item to string
	  * @param localize A localization function that localizes the produced string
	  * @tparam A The source item type
	  * @return A new display function
	  */
	def apply[A](transform: Transform[A])(localize: Localize) = new DisplayFunction[A](item => localize(transform(item)))
	
	/**
	  * This displayfunction takes in local strings and localizes them with specified function
	  * @param localize A localization function
	  * @return A new display function
	  */
	def stringLocalized(localize: Localize) = DisplayFunction[LocalString]{ s => s }(localize)
	
	/**
	  * This displayFunction takes in local strings and localizes them using the specified localizer (implicit)
	  * @param localizer A localizer that does the localization (implicit)
	  * @return A new display function
	  */
	def stringLocalized()(implicit localizer: Localizer): DisplayFunction[LocalString] = stringLocalized(localizer.localize)
	
	/**
	  * This display function uses an item's toString function to transform it to string and then a localization
	  * function to localize the produced string
	  * @param localize A localization function
	  * @return A new display function
	  */
	def localizeOnly(localize: Localize) = DisplayFunction[Any]{ a => LocalString(a.toString) }(localize)
	
	/**
	  * This display function only transforms an item into string form but doesn't then perform a localization on the
	  * string
	  * @param transform A transform function for transforming the item to local string format
	  * @tparam A Source item type
	  * @return A new display function
	  */
	def noLocalization[A](transform: Transform[A]) = DisplayFunction[A](transform){ s => s.localizationSkipped }
	
	/**
	  * This display function transforms an item into string format and then localizes it using a localizer (implicit)
	  * @param transform A transform function for transforming the item to string format
	  * @param localizer A localizer for localization (implicit)
	  * @tparam A Source item type
	  * @return A new display function
	  */
	def localized[A](transform: Transform[A])(implicit localizer: Localizer) = DisplayFunction[A](transform)(localizer.localize)
	
	/**
	  * This display function uses toString to convert an item to string and then localizes that string using a
	  * localizer (implicit)
	  * @param localizer A localizer that handles localization (implicit)
	  * @return A new display function
	  */
	def localized()(implicit localizer: Localizer) = localizeOnly(localizer.localize)
	
	/**
	  * Creates a new display function that uses string interpolation
	  * @param transform A transform function that converts an item to a local string
	  * @param localize A function that localizes a string
	  * @param getArgs A function for retrieving interpolation arguments
	  * @tparam A source parameter type
	  * @return A new display function that interpolates the localized string
	  */
	def interpolating[A](transform: Transform[A], localize: Localize, getArgs: A => Seq[Any]) =
		new DisplayFunction[A](item => localize(transform(item)).interpolate(getArgs(item)))
	
	/**
	  * Creates a new display function that uses string interpolation
	  * @param transform A transform function that converts an item to a local string
	  * @param getArgs A function for retrieving interpolation arguments
	  * @param localizer A localizer that localizes the local string (implicit)
	  * @tparam A source parameter type
	  * @return A new display function that interpolates the localized string
	  */
	def interpolatingLocalized[A](transform: Transform[A], getArgs: A => Seq[Any])(implicit localizer: Localizer) =
		interpolating(transform, localizer.localize, getArgs)
	
	/**
	  * Creates a new display function that simply interpolates a string
	  * @param string The string that will be interpolated using parameter item (single argument)
	  * @return A new display function that uses a static string with interpolation
	  */
	def interpolating(string: LocalizedString) = new DisplayFunction[Any](item => string.interpolate(Vector(item)))
	
	/**
	  * This display function is used for displaying time in a specific format
	  * @param formatter A date time formatter
	  * @return A display function for time elements
	  */
	def forTime(formatter: DateTimeFormatter) =
	{
		noLocalization[TemporalAccessor] {
			case instant: Instant => LocalString(instant.toStringWith(formatter))
			case default => LocalString(formatter.format(default))
		}
	}
}

/**
  * These functions are used for converting data into localized text format
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
  */
class DisplayFunction[-A](f: A => LocalizedString)
{
	// OPERATORS	-------------------
	
	/**
	  * Displays an item
	  * @param item The source item
	  * @return A localized display version for the item
	  */
	def apply(item: A) = f(item)
}
