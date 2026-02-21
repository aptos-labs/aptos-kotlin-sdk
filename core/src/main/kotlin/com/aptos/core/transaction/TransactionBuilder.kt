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

    fun sender(sender: AccountAddress): TransactionBuilder = apply { this.sender = sender }

    fun sequenceNumber(sequenceNumber: ULong): TransactionBuilder = apply {
        this.sequenceNumber = sequenceNumber
    }

    fun payload(payload: TransactionPayload): TransactionBuilder = apply { this.payload = payload }

    fun maxGasAmount(maxGasAmount: ULong): TransactionBuilder = apply {
        this.maxGasAmount = maxGasAmount
    }

    fun gasUnitPrice(gasUnitPrice: ULong): TransactionBuilder = apply {
        this.gasUnitPrice = gasUnitPrice
    }

    fun expirationTimestampSecs(expirationTimestampSecs: ULong): TransactionBuilder = apply {
        this.expirationTimestampSecs = expirationTimestampSecs
    }

    fun chainId(chainId: ChainId): TransactionBuilder = apply { this.chainId = chainId }

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
            }

        return SignedTransaction(actualRawTxn, authenticator)
    }

    companion object {
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
                }

            return SignedTransaction(rawTransaction, authenticator)
        }
    }
}
