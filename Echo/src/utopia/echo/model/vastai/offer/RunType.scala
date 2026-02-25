package utopia.echo.model.vastai.offer

import utopia.flow.collection.CollectionExtensions._

/**
 * An enumeration for different ways to run a Vast AI instance
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
sealed trait RunType
{
	/**
	 * @return Key used for this run-type in Vast AI
	 */
	def key: String
}

object RunType
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * All run-type values
	 */
	val values = Vector[RunType](Ssh, DirectSsh, SshViaProxy, Args, Jupyter, DirectJupyter, JupyterViaProxy)
	
	
	// OTHER    --------------------------
	
	/**
	 * @param key A key representing a run-type
	 * @return Run-type matching that key. None if no such run-type was found.
	 */
	def findForKey(key: String) = {
		val lower = key.toLowerCase
		values.find { _.key == lower }
	}
	/**
	 * @param key A key representing a run-type
	 * @return Run-type matching that key. Failure if no such run-type was found.
	 */
	def forKey(key: String) = findForKey(key)
		.toTry { new NoSuchElementException(s"No RunType value matches \"$key\"") }
	
	
	// VALUES   --------------------------
	
	/**
	 * Container exposes SSH
	 */
	case object Ssh extends RunType
	{
		override val key: String = "ssh"
		
		def direct = DirectSsh
		def proxy = SshViaProxy
	}
	/**
	 * Direct SSH, fewer proxies
	 */
	case object DirectSsh extends RunType
	{
		override val key: String = "ssh_direct"
	}
	/**
	 * SSH via Vast proxy
	 */
	case object SshViaProxy extends RunType
	{
		override val key: String = "ssh_proxy"
	}
	
	/**
	 * Only runs entrypoint with args
	 */
	case object Args extends RunType
	{
		override val key: String = "args"
	}
	
	/**
	 * Starts Jupyter server
	 */
	case object Jupyter extends RunType
	{
		override val key: String = "jupyter"
		
		def direct = DirectJupyter
		def proxy = JupyterViaProxy
	}
	/**
	 * Direct Jupyter
	 */
	case object DirectJupyter extends RunType
	{
		override val key: String = "jupyter_direct"
	}
	/**
	 * Jupyter via proxy
	 */
	case object JupyterViaProxy extends RunType
	{
		override val key: String = "jupyter_proxy"
	}
}