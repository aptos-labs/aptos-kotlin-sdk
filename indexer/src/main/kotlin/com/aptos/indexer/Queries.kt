package com.aptos.indexer

/**
 * GraphQL query string constants for the Aptos Indexer.
 */
object Queries {
    /** Fetches tokens owned by an account. */
    const val GET_ACCOUNT_TOKENS = """
        query GetAccountTokens(${'$'}owner_address: String!, ${'$'}offset: Int, ${'$'}limit: Int) {
            current_token_ownerships_v2(
                where: { owner_address: { _eq: ${'$'}owner_address } }
                offset: ${'$'}offset
                limit: ${'$'}limit
            ) {
                token_data_id
                token_name
                collection_name
                creator_address
                amount
                token_uri
                token_properties
            }
        }
    """

    /** Fetches NFT collections, optionally filtered by creator. */
    const val GET_COLLECTIONS = """
        query GetCollections(${'$'}creator_address: String, ${'$'}offset: Int, ${'$'}limit: Int) {
            current_collections_v2(
                where: { creator_address: { _eq: ${'$'}creator_address } }
                offset: ${'$'}offset
                limit: ${'$'}limit
            ) {
                collection_id
                collection_name
                creator_address
                description
                uri
                max_supply
                current_supply
            }
        }
    """

    /** Fetches user transactions for a given account with payload data. */
    const val GET_ACCOUNT_TRANSACTIONS = """
        query GetAccountTransactions(${'$'}sender: String!, ${'$'}offset: Int, ${'$'}limit: Int) {
            user_transactions(
                where: { sender: { _eq: ${'$'}sender } }
                order_by: { version: desc }
                offset: ${'$'}offset
                limit: ${'$'}limit
            ) {
                version
                hash
                sender
                sequence_number
                success
                vm_status
                gas_used
                timestamp
                payload
            }
        }
    """

    /** Fetches events, optionally filtered by account address. */
    const val GET_EVENTS = """
        query GetEvents(${'$'}account_address: String, ${'$'}type: String, ${'$'}offset: Int, ${'$'}limit: Int) {
            events(
                where: {
                    account_address: { _eq: ${'$'}account_address }
                    type: { _eq: ${'$'}type }
                }
                order_by: { transaction_version: desc }
                offset: ${'$'}offset
                limit: ${'$'}limit
            ) {
                account_address
                sequence_number
                type
                data
                transaction_version
            }
        }
    """
}
