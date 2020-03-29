package utopia.reflection.component.swing

import javax.swing.text.{AttributeSet, PlainDocument}
import utopia.reflection.text.TextFilter

object FilterDocument
{
	/**
	  * @return A new document with no filtering and no maximum length
	  */
	def noFilter() = new FilterDocument(None, None)
	
	/**
	  * @param filter Filter
	  * @param maxLength Maximum length
	  * @return A new document with both filter and maximum length
	  */
	def apply(filter: TextFilter, maxLength: Int): FilterDocument = new FilterDocument(Some(filter), Some(maxLength))
	
	/**
	  * @param filter A filter
	  * @return A document with filter and no maximum length
	  */
	def apply(filter: TextFilter): FilterDocument = new FilterDocument(Some(filter), None)
	
	/**
	  * @param maxLength Maximum length
	  * @return A document with no filter but a maximum length
	  */
	def withMaxLength(maxLength: Int): FilterDocument = new FilterDocument(None, Some(maxLength))
}

/**
  * This document limits the available characters and/or string length
  * @author Mikko Hilpinen
  * @since 1.5.2019, v1+
  * @param filter The filter applied to (input) text. None if not filtered
  * @param maxLength The maximum length of input. None if not limited
  */
class FilterDocument(val filter: Option[TextFilter], val maxLength: Option[Int]) extends PlainDocument
{
	// ATTRIBUTES	---------------------
	
	private var unacceptedListeners = Vector[() => Unit]()
	private var lengthLimitReachedListeners = Vector[() => Unit]()
	
	
	// IMPLEMENTED	---------------------
	
	override def insertString(offs: Int, str: String, a: AttributeSet) =
	{
		// May filter the string
		val filtered = filter.map { _.format(str) } getOrElse str
		
		// May hit the length limit
		if (maxLength.exists { getLength + filtered.length > _ })
		{
			val result = filtered.substring(0, maxLength.get - getLength)
			super.insertString(offs, result, a)
			
			// Informs listeners when length limit is reached
			unacceptedListeners.foreach { _() }
			lengthLimitReachedListeners.foreach { _() }
		}
		else
		{
			super.insertString(offs, filtered, a)
			
			// Informs listeners when part of the string was not included
			if (filtered.length < str.length)
				unacceptedListeners.foreach { _() }
		}
	}
	
	
	// OTHER	------------------------
	
	/**
	  * Adds a new function that will be called if insertion was not fully accepted
	  * @param listener A listener function
	  */
	def addUnacceptedListener(listener: () => Unit) = unacceptedListeners :+= listener
	
	/**
	  * Adds a new function that will be called when the length limit is reached
	  * @param listener A listener function
	  */
	def addLengthLimitReachedListener(listener: () => Unit) = lengthLimitReachedListeners :+= listener
}
