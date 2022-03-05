package dreifa.app.ses

import dreifa.app.types.LikeString
import org.junit.jupiter.api.Test

class EventStoreTest {

    @Test
    fun `should match underscore in like string`() {
        val like = LikeString("ab_d").toRegex()

        // good matches
        assert(like.matches("abcd"))
        assert(like.matches("abCd"))
        assert(like.matches("ab7d"))
        assert(like.matches("ab!d"))
        assert(like.matches("ab_d"))
        assert(like.matches("ab%d"))


        // bad macthes
        assert(!like.matches("abd"))
        assert(!like.matches("ab..d"))
        assert(!like.matches("Abcd"))
        assert(!like.matches("_abcd"))
        assert(!like.matches("abcd_"))
    }

    @Test
    fun `should match percent in like string`() {
        val like = LikeString("ab%d").toRegex()

        // good matches
        assert(like.matches("abcd"))
        assert(like.matches("ab7d"))
        assert(like.matches("ab!d"))
        assert(like.matches("abd"))
        assert(like.matches("ab..d"))
        assert(like.matches("ab_d"))
        assert(like.matches("ab%d"))


        // bad matches
        assert(!like.matches("Abcd"))
        assert(!like.matches("_abcd"))
        assert(!like.matches("abcd_"))
    }

}