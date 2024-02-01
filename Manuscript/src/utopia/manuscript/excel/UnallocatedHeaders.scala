package utopia.manuscript.excel

import utopia.flow.operator.equality.EqualsExtensions._

import scala.language.implicitConversions

object UnallocatedHeaders
{
	// IMPLICIT --------------------
	
	/**
	  * @param headers Set of (primary) headers to include
	  * @return A wrapper for the specified headers
	  */
	implicit def from(headers: IterableOnce[String]): UnallocatedHeaders =
		new UnallocatedHeaders(Set.from(headers.iterator.map { _ -> Set[String]() }))
		
	
	// OTHER    -------------------
	
	/**
	  * @param first The first header
	  * @param more additional headers
	  * @return A set of headers containing each of the listed values
	  */
	def apply(first: String, more: String*) = from(first +: more)
}

/**
  * A set of headers expected to be found from a document.
  * These headers are not associated with any specific cells, though.
  * @author Mikko Hilpinen
  * @since 01/02/2024, v1.0
  * @param headers The wrapped header names. Each name is linked to a set of alternative names for that header.
  */
case class UnallocatedHeaders(headers: Set[(String, Set[String])])
{
	// ATTRIBUTES   -----------------------
	
	// Lower-case versions of all listed headers
	private lazy val all = headers
		.flatMap { case (primary, alternatives) => alternatives + primary }.map { _.toLowerCase }
	/**
	  * A mapping where the alternative header names (lower-cased) are keys and primary header names are values
	  */
	lazy val secondaryToPrimary = headers
		.flatMap { case (primary, alternatives) => alternatives.map { _.toLowerCase -> primary } }.toMap
		
	
	// OTHER    ---------------------------
	
	/**
	  * @param key Targeted header key
	  * @return Whether that key is present in this set of headers
	  */
	def contains(key: String) = all.contains(key.toLowerCase)
	/**
	  * @param keys Keys that are present in some field
	  * @return Whether those fields contain all of these headers (in primary or alternative form)
	  */
	def areFoundFrom(keys: Iterable[String]) = {
		headers.forall { case (header, altForms) =>
			val lower = altForms.map { _.toLowerCase } + header.toLowerCase
			keys.exists { k => lower.contains(k.toLowerCase) }
		}
	}
	
	/**
	  * @param key A key
	  * @return Primary key that matches that key (or that key itself), plus alternative names for that key
	  */
	def apply(key: String): (String, Set[String]) = {
		val primary = primaryFor(key)
		headers.find { _._1 ~== primary }.getOrElse { primary -> Set() }
	}
	/**
	  * @param key A key
	  * @return The primary form of that key. That key if not present in these headers.
	  */
	def primaryFor(key: String) = secondaryToPrimary.getOrElse(key.toLowerCase, key)
	
	/**
	  * @param header A new header to include
	  * @return Copy of these headers with the specified header included
	  */
	def +(header: String) = {
		if (contains(header))
			this
		else
			copy(headers = headers + (header -> Set()))
	}
	/**
	  * @param header A header to include, including its alternative names
	  * @return Copy of these headers with that header included
	  */
	def +(header: (String, Set[String])): UnallocatedHeaders = {
		if (header._2.nonEmpty) {
			headers.find { _._1 ~== header._1 } match {
				// Case: Header already exists => Adds new alternatives
				case Some((existing, existingAlternatives)) =>
					copy(headers =
						headers.filterNot { _._1 == existing }
							.map { case (h, alt) => h -> (alt -- header._2) } +
							(header._1 -> (header._2 ++ existingAlternatives)))
				case None => copy(headers = headers.map { case (h, alt) => h -> (alt -- header._2) } + header)
			}
		}
		// Case: No alternatives => Uses the simpler implementation
		else
			this + header._1
	}
	/**
	  * @param headers New headers to include
	  * @return Copy of these headers with the specified headers included
	  */
	def ++(headers: IterableOnce[String]) = {
		val newHeadersIter = headers.iterator.filterNot(contains)
		if (newHeadersIter.hasNext)
			copy(headers = this.headers ++ newHeadersIter.map { _ -> Set[String]() })
		else
			this
	}
}
