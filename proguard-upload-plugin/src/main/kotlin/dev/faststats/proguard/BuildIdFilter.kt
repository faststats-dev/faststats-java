package dev.faststats.proguard

import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

class BuildIdFilter(`in`: Reader) : FilterReader(`in`) {

    lateinit var buildId: String

    private var delegate: Reader? = null

    private fun ensureInitialized(): Reader {
        var d = delegate
        if (d == null) {
            val content = this.`in`.readText()
            val filtered = content.lines().filter { !it.startsWith("buildId=") }.joinToString("\n")
            val result = "${filtered.trimEnd()}\nbuildId=$buildId\n"
            d = StringReader(result)
            delegate = d
        }
        return d
    }

    override fun read(): Int = ensureInitialized().read()

    override fun read(cbuf: CharArray, off: Int, len: Int): Int = ensureInitialized().read(cbuf, off, len)

    override fun ready(): Boolean = ensureInitialized().ready()

    override fun close() {
        delegate?.close()
        this.`in`.close()
    }
}
