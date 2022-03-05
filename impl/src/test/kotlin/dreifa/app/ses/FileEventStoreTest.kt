package dreifa.app.ses

import dreifa.app.helpers.random
import java.io.File

class FileEventStoreTest : BaseEventStoreTest() {


    private fun newTestDir(): String {
        val dir = "../.testing/${String.random()}"
        println("Test data in: ${File(dir).canonicalPath}")
        return dir
    }

    override fun createEventStore(): EventStore {
        val testDir = newTestDir()
        val fes = FileEventStore(testDir)
        return fes
    }
}