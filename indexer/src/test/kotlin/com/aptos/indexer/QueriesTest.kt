package com.aptos.indexer

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class QueriesTest {

    @Test
    fun `GET_ACCOUNT_TOKENS query contains expected fields`() {
        Queries.GET_ACCOUNT_TOKENS shouldContain "current_token_ownerships_v2"
        Queries.GET_ACCOUNT_TOKENS shouldContain "token_data_id"
        Queries.GET_ACCOUNT_TOKENS shouldContain "token_name"
        Queries.GET_ACCOUNT_TOKENS shouldContain "owner_address"
    }

    @Test
    fun `GET_COLLECTIONS query contains expected fields`() {
        Queries.GET_COLLECTIONS shouldContain "current_collections_v2"
        Queries.GET_COLLECTIONS shouldContain "collection_id"
        Queries.GET_COLLECTIONS shouldContain "collection_name"
        Queries.GET_COLLECTIONS shouldContain "creator_address"
    }

    @Test
    fun `GET_ACCOUNT_TRANSACTIONS query contains expected fields`() {
        Queries.GET_ACCOUNT_TRANSACTIONS shouldContain "user_transactions"
        Queries.GET_ACCOUNT_TRANSACTIONS shouldContain "version"
        Queries.GET_ACCOUNT_TRANSACTIONS shouldContain "hash"
        Queries.GET_ACCOUNT_TRANSACTIONS shouldContain "sender"
    }

    @Test
    fun `GET_EVENTS query contains expected fields`() {
        Queries.GET_EVENTS shouldContain "events"
        Queries.GET_EVENTS shouldContain "account_address"
        Queries.GET_EVENTS shouldContain "sequence_number"
        Queries.GET_EVENTS shouldContain "type"
    }
}
