package com.aptos.core.transaction

import com.aptos.core.account.Account
import com.aptos.core.crypto.Ed25519
import com.aptos.core.crypto.SignatureScheme
import com.aptos.core.error.TransactionBuildException
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId

/**
 * Fluent builder for constructing and signing Aptos transactions.
 *
 * Provides sensible defaults: `maxGasAmount = 200,000`, `gasUnitPrice = 100`,
 * and `expirationTimestampSecs = now + 600 seconds`.
 *
 * Supports single-signer, multi-agent, and fee-payer transaction modes.
 *
 * ```kotlin
 * val signedTxn = TransactionBuilder.builder()
 *     .sender(account.address)
 *     .sequenceNumber(0uL)
 *     .payload(payload)
 *     .chainId(ChainId.TESTNET)
 *     .sign(account)
 * ```
 */
class TransactionBuilder {
    private var sender: AccountAddress? = null
    private var sequenceNumber: ULong? = null
    private var payload: TransactionPayload? = null
    private var maxGasAmount: ULong = 200_000uL
    private var gasUnitPrice: ULong = 100uL
    private var expirationTimestampSecs: ULong? = null
    private var chainId: ChainId? = null
    private var secondarySignersList: List<Account>? = null
    private var feePayerAccount: Account? = null

    /** Sets the transaction sender address (required). */
    fun sender(sender: AccountAddress): TransactionBuilder = apply { this.sender = sender }

    /** Sets the sender's current on-chain sequence number (required). */
    fun sequenceNumber(sequenceNumber: ULong): TransactionBuilder = apply {
        this.sequenceNumber = sequenceNumber
    }

    /** Sets the transaction payload (required). */
    fun payload(payload: TransactionPayload): TransactionBuilder = apply { this.payload = payload }

    /** Sets the maximum gas units for this transaction (default: 200,000). */
    fun maxGasAmount(maxGasAmount: ULong): TransactionBuilder = apply {
        this.maxGasAmount = maxGasAmount
    }

    /** Sets the gas unit price in octas (default: 100). */
    fun gasUnitPrice(gasUnitPrice: ULong): TransactionBuilder = apply {
        this.gasUnitPrice = gasUnitPrice
    }

    /** Sets the expiration timestamp in seconds since epoch (default: now + 600s). */
    fun expirationTimestampSecs(expirationTimestampSecs: ULong): TransactionBuilder = apply {
        this.expirationTimestampSecs = expirationTimestampSecs
    }

    /** Sets the chain ID for the target network (required). */
    fun chainId(chainId: ChainId): TransactionBuilder = apply { this.chainId = chainId }

    /** Sets secondary signers for a multi-agent transaction. */
    fun secondarySigners(signers: List<Account>): TransactionBuilder = apply {
        this.secondarySignersList = signers
    }

    /** Sets a fee payer for the transaction. */
    fun feePayer(payer: Account): TransactionBuilder = apply {
        this.feePayerAccount = payer
    }

    /**
     * Builds the [RawTransaction] from the configured fields.
     *
     * @throws TransactionBuildException if sender, sequenceNumber, payload, or chainId are missing
     */
    fun build(): RawTransaction {
        val s = sender ?: throw TransactionBuildException("sender is required")
        val seq = sequenceNumber ?: throw TransactionBuildException("sequenceNumber is required")
        val p = payload ?: throw TransactionBuildException("payload is required")
        val cid = chainId ?: throw TransactionBuildException("chainId is required")
        val exp =
            expirationTimestampSecs
                ?: (System.currentTimeMillis() / 1000 + 600).toULong()

        return RawTransaction(
            sender = s,
            sequenceNumber = seq,
            payload = p,
            maxGasAmount = maxGasAmount,
            gasUnitPrice = gasUnitPrice,
            expirationTimestampSecs = exp,
            chainId = cid,
        )
    }

    /** Builds and signs the transaction with the given [account]. */
    fun sign(account: Account): SignedTransaction {
        val rawTxn =
            build().also {
                if (sender == null) sender = account.address
            }
        val actualRawTxn =
            if (sender != account.address) {
                rawTxn
            } else {
                rawTxn.copy(sender = account.address)
            }

        val feePayer = feePayerAccount
        val secondarySigners = secondarySignersList

        // Fee payer mode
        if (feePayer != null) {
            val secondaryAddresses = secondarySigners?.map { it.address } ?: emptyList()
            val rawTxnWithData = RawTransactionWithData.FeePayer(
                rawTransaction = actualRawTxn,
                secondarySignerAddresses = secondaryAddresses,
                feePayerAddress = feePayer.address,
            )
            val signingMessage = rawTxnWithData.signingMessage()

            val senderAuth = createAccountAuthenticator(account, signingMessage)
            val secondaryAuths = secondarySigners?.map { createAccountAuthenticator(it, signingMessage) } ?: emptyList()
            val feePayerAuth = createAccountAuthenticator(feePayer, signingMessage)

            val authenticator = TransactionAuthenticator.FeePayer(
                sender = senderAuth,
                secondarySignerAddresses = secondaryAddresses,
                secondarySigners = secondaryAuths,
                feePayerAddress = feePayer.address,
                feePayerAuth = feePayerAuth,
            )
            return SignedTransaction(actualRawTxn, authenticator)
        }

        // Multi-agent mode
        if (secondarySigners != null && secondarySigners.isNotEmpty()) {
            val secondaryAddresses = secondarySigners.map { it.address }
            val rawTxnWithData = RawTransactionWithData.MultiAgent(
                rawTransaction = actualRawTxn,
                secondarySignerAddresses = secondaryAddresses,
            )
            val signingMessage = rawTxnWithData.signingMessage()

            val senderAuth = createAccountAuthenticator(account, signingMessage)
            val secondaryAuths = secondarySigners.map { createAccountAuthenticator(it, signingMessage) }

            val authenticator = TransactionAuthenticator.MultiAgent(
                sender = senderAuth,
                secondarySignerAddresses = secondaryAddresses,
                secondarySigners = secondaryAuths,
            )
            return SignedTransaction(actualRawTxn, authenticator)
        }

        // Single-signer mode
        val signingMessage = actualRawTxn.signingMessage()
        val signatureBytes = account.sign(signingMessage)

        val authenticator =
            when (account.scheme) {
                SignatureScheme.ED25519 ->
                    TransactionAuthenticator.Ed25519Auth(
                        publicKey = Ed25519.PublicKey(account.publicKeyBytes),
                        signature = Ed25519.Signature(signatureBytes),
                    )
                SignatureScheme.SECP256K1 ->
                    TransactionAuthenticator.SingleSender(
                        AccountAuthenticator.SingleKey(
                            publicKeyType = 1u, // Secp256k1 = 1
                            publicKeyBytes = account.publicKeyBytes,
                            signatureType = 1u, // Secp256k1 = 1
                            signatureBytes = signatureBytes,
                        ),
                    )
                else ->
                    TransactionAuthenticator.SingleSender(
                        createAccountAuthenticator(account, signingMessage),
                    )
            }

        return SignedTransaction(actualRawTxn, authenticator)
    }

    companion object {
        /** Creates a new [TransactionBuilder] instance. */
        @JvmStatic
        fun builder(): TransactionBuilder = TransactionBuilder()

        /** Signs a pre-built [rawTransaction] with the given [account]. */
        @JvmStatic
        fun signTransaction(account: Account, rawTransaction: RawTransaction): SignedTransaction {
            val signingMessage = rawTransaction.signingMessage()
            val signatureBytes = account.sign(signingMessage)

            val authenticator =
                when (account.scheme) {
                    SignatureScheme.ED25519 ->
                        TransactionAuthenticator.Ed25519Auth(
                            publicKey = Ed25519.PublicKey(account.publicKeyBytes),
                            signature = Ed25519.Signature(signatureBytes),
                        )
                    SignatureScheme.SECP256K1 ->
                        TransactionAuthenticator.SingleSender(
                            AccountAuthenticator.SingleKey(
                                publicKeyType = 1u,
                                publicKeyBytes = account.publicKeyBytes,
                                signatureType = 1u,
                                signatureBytes = signatureBytes,
                            ),
                        )
                    else ->
                        TransactionAuthenticator.SingleSender(
                            createAccountAuthenticator(account, signingMessage),
                        )
                }

            return SignedTransaction(rawTransaction, authenticator)
        }

        /**
         * Creates an [AccountAuthenticator] from an account and signing message,
         * dispatching on the account's signature scheme.
         */
        @JvmStatic
        fun createAccountAuthenticator(account: Account, signingMessage: ByteArray): AccountAuthenticator {
            val signatureBytes = account.sign(signingMessage)
            return when (account.scheme) {
                SignatureScheme.ED25519 ->
                    AccountAuthenticator.Ed25519Auth(
                        publicKey = Ed25519.PublicKey(account.publicKeyBytes),
                        signature = Ed25519.Signature(signatureBytes),
                    )
                SignatureScheme.MULTI_ED25519 -> {
                    val pubKey = com.aptos.core.account.MultiEd25519Account::class.java
                        .let { account as com.aptos.core.account.MultiEd25519Account }
                    AccountAuthenticator.MultiEd25519Auth(
                        publicKey = pubKey.multiPublicKey,
                        signature = pubKey.signMultiEd25519(signingMessage),
                    )
                }
                SignatureScheme.SECP256K1 ->
                    AccountAuthenticator.SingleKey(
                        publicKeyType = 1u,
                        publicKeyBytes = account.publicKeyBytes,
                        signatureType = 1u,
                        signatureBytes = signatureBytes,
                    )
                else ->
                    AccountAuthenticator.SingleKey(
                        publicKeyType = 0u,
                        publicKeyBytes = account.publicKeyBytes,
                        signatureType = 0u,
                        signatureBytes = signatureBytes,
                    )
            }
        }
    }
}
