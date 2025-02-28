package eu.kanade.tachiyomi.animeextension.pt.goanimes.extractors

import android.util.Base64
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response

class LinkfunBypasser(private val client: OkHttpClient) {
    fun getIframeUrl(response: Response): String {
        return response.use { page ->
            val document = page.asJsoup(decodeAtob(page.body.string()))
            val newHeaders = Headers.headersOf("Referer", document.location())

            val iframe = document.selectFirst("iframe")

            if (iframe != null) {
                iframe.attr("src")
            } else {
                val formBody = FormBody.Builder().apply {
                    document.select("input[name]").forEach {
                        add(it.attr("name"), it.attr("value"))
                    }
                }.build()

                val formUrl = document.selectFirst("form")!!.attr("action")
                client.newCall(POST(formUrl, newHeaders, formBody))
                    .execute()
                    .use(::getIframeUrl)
            }
        }
    }

    companion object {
        fun decodeAtob(html: String): String {
            val atobContent = html.substringAfter("atob(\"").substringBefore("\"));")
            val hexAtob = atobContent.replace("\\x", "").decodeHex()
            val decoded = Base64.decode(hexAtob, Base64.DEFAULT)
            return String(decoded)
        }

        // Stolen from AnimixPlay(EN) / GogoCdnExtractor
        private fun String.decodeHex(): ByteArray {
            check(length % 2 == 0) { "Must have an even length" }
            return chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }
    }
}
