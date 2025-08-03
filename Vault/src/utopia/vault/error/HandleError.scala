package utopia.vault.error

import utopia.vault.context.VaultContext
import utopia.vault.error.ErrorHandler.Rethrow

/**
  * An interface for handling errors thrown during program execution
  * @author Mikko Hilpinen
  * @since 03.08.2025, v2.0
  */
object HandleError
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * The default method for handling errors
	  */
	var default: ErrorHandler = Rethrow
	
	private var _connection: Option[ErrorHandler] = None
	private var _parse: Option[ErrorHandler] = None
	private var _clip: ErrorHandler = ErrorHandler.logUsing(VaultContext.log)
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return A handler for handling errors during database query execution
	  */
	def duringDbQuery = _connection.getOrElse(default)
	/**
	  * @return A handler for handling errors during database row -processing
	  */
	def duringRowParsing = _parse.getOrElse(default)
	/**
	  * @return A handler for handling cases where some data is excluded from inserts,
	  *         because said data belonged to a different table
	  */
	def fromInsertClipping = _clip
	
	/**
	  * Assigns an error handler for dealing with errors during database query execution
	  * @param handler An error handler
	  */
	def handleQueryErrorsWith(handler: ErrorHandler) = _connection = Some(handler)
	/**
	  * Assigns an error handler for dealing with errors during database row processing
	  * @param handler An error handler
	  */
	def handleRowParseErrorsWith(handler: ErrorHandler) = _parse = Some(handler)
	/**
	  * Assigns an error handler for dealing with insert clipping errors
	  * @param handler An error handler
	  */
	def handleInsertClipErrorsWith(handler: ErrorHandler) = _clip = handler
	
	/**
	  * Assigns an error handler to deal with all situations
	  * @param handler An error handler
	  */
	def handleAllWith(handler: ErrorHandler) = {
		default = handler
		_connection = None
		_parse = None
		_clip = handler
	}
}
