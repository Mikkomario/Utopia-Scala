package utopia.vigil.database

import utopia.vault.context.{VaultContext, VaultContextWrapper}

/**
 * Used for setting up DB access in the Utopia Vigil module
 * @author Mikko Hilpinen
 * @since 01.05.2026, v0.1
 */
object VigilContext extends VaultContextWrapper
{
	// ATTRIBUTES   -------------------------
	
	private var vaultContext: Option[VaultContext] = None
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def wrapped: VaultContext =
		vaultContext.getOrElse { throw new IllegalStateException("VigilContext has not yet been set up") }
	
	
	// OTHER    -----------------------------
	
	/**
	 * Sets up this context
	 * @param vaultContext Vault context to wrap
	 */
	def setup(vaultContext: VaultContext) = this.vaultContext = Some(vaultContext)
}
