package ireader.hizomanga

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.parametersOf
import ireader.core.source.Dependencies
import ireader.core.source.model.Command
import ireader.core.source.model.CommandList
import ireader.core.source.model.Filter
import ireader.core.source.model.FilterList
import ireader.core.source.model.MangaInfo
import ireader.core.source.SourceFactory
import ireader.core.source.asJsoup
import org.jsoup.nodes.Document
import tachiyomix.annotations.Extension

@Extension
abstract class Hizomanga(deps: Dependencies) : SourceFactory(
    deps = deps,
) {
    override val lang: String
        get() = "ar"
    override val baseUrl: String
        get() = "https://hizomanga.com"
    override val id: Long
        get() = 52
    override val name: String
        get() = "Hizomanga"

    override fun getFilters(): FilterList = listOf(
        Filter.Title(),

    )

    override fun getCommands(): CommandList = listOf(
        Command.Detail.Fetch(),
        Command.Content.Fetch(),
        Command.Chapter.Fetch(),
    )

    override val exploreFetchers: List<BaseExploreFetcher>
        get() = listOf(
            BaseExploreFetcher(
                "Latest",
                endpoint = "/series/?page={page}&status=&order=latest",
                selector = "div.inmain div.mdthumb",
                nameSelector = "a",
                nameAtt = "title",
                linkSelector = "a",
                linkAtt = "href",
                coverSelector = "a img",
                coverAtt = "data-src",
                nextPageSelector = "a.r"
            ),
            BaseExploreFetcher(
                "Search",
                endpoint = "/page/{page}/?s={query}",
                selector = "div.inmain div.mdthumb",
                nameSelector = "a",
                nameAtt = "title",
                linkSelector = "a",
                linkAtt = "href",
                coverSelector = "a img",
                coverAtt = "data-src",
                nextPageSelector = "a.next",
                type = SourceFactory.Type.Search
            ),
            BaseExploreFetcher(
                "Trending",
                endpoint = "/series/?page={page}&status=&order=popular",
                selector = "div.inmain div.mdthumb",
                nameSelector = "a",
                nameAtt = "title",
                linkSelector = "a",
                linkAtt = "href",
                coverSelector = "a img",
                coverAtt = "data-src",
                nextPageSelector = "a.r"
            ),
            BaseExploreFetcher(
                "New",
                endpoint = "/series/?page={page}&order=update",
                selector = "div.inmain div.mdthumb",
                nameSelector = "a",
                nameAtt = "title",
                linkSelector = "a",
                linkAtt = "href",
                coverSelector = "a img",
                coverAtt = "data-src",
                nextPageSelector = "a.rs"
            ),

        )

    override suspend fun getListRequest(
        baseExploreFetcher: BaseExploreFetcher,
        page: Int,
        query: String,
    ): Document {
        val url = "${getCustomBaseUrl()}${
            (baseExploreFetcher.endpoint)?.replace(this.page, baseExploreFetcher.onPage(page.toString()))?.replace(
                this
                    .query,
                query.let { baseExploreFetcher.onQuery(query) }
            )
        }"

        return client.submitForm(formParameters = Parameters.build {
            append("action", "madara_load_more")
            append("page", page.toString())
            append("template", "madara-core/content/content-archive")
            append("vars[paged]", "1")
            append("vars[orderby]", "date")
            append("vars[template]", "archive")
            append("vars[sidebar]", "full")
            append("vars[post_type]", "wp-manga")
            append("vars[post_status]", "publish")
            append("vars[meta_query][relation]", "OR")
            append("vars[manga_archives_item_layout]", "big_thumbnail")
        }, url = url).asJsoup()
    }

    override val detailFetcher: Detail
        get() = SourceFactory.Detail(
            nameSelector = "h1.entry-title",
            coverSelector = "div.sertothumb img",
            coverAtt = "data-src",
            descriptionSelector = "div.entry-content[itemprop=description] p",
            authorBookSelector = "div.serl:contains(الكاتب) span a",
            categorySelector = "div.sertogenre a",
            statusSelector = "div.sertostat span",
            onStatus = { status ->
                if (status.contains("Ongoing")) {
                    MangaInfo.ONGOING
                } else if (status.contains("Hiatus")) {
                    MangaInfo.ON_HIATUS
                } else {
                    MangaInfo.COMPLETED
                }
            },
        )
    override fun HttpRequestBuilder.headersBuilder(block: HeadersBuilder.() -> Unit) {
        headers {
            append(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
            )
            append(HttpHeaders.Referrer, baseUrl)
        }
    }

    override val chapterFetcher: Chapters
        get() = SourceFactory.Chapters(
            selector = "li[data-id]",
            nameSelector = "a div.epl-num ,a div.epl-title",
            linkSelector = "a",
            linkAtt = "href",
            // reverseChapterList = true,
        )

    override val contentFetcher: Content
        get() = SourceFactory.Content(
            pageTitleSelector = ".epheader",
            pageContentSelector = "div.entry-content p:not([style~=opacity]), div.entry-content ol li",
        )
}
