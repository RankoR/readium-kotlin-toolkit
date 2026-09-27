@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.opds

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.toInstant
import org.readium.r2.shared.opds.Facet
import org.readium.r2.shared.opds.Feed
import org.readium.r2.shared.opds.OpdsMetadata
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.publication.Contributor
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.publication.PublicationCollection
import org.readium.r2.shared.publication.Subject
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.readium.r2.shared.util.http.HttpTry
import org.readium.r2.shared.util.http.HttpStreamResponse
import org.readium.r2.shared.util.http.HttpStatus
import org.readium.r2.shared.util.http.HttpResponse
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.Try
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class OPDS1ParserTest {

    private val fixtures = Fixtures("opds1")

    @Test fun `parse navigation feed`() {
        val parseData = parse("nav-feed.atom")
        assertEquals(
            ParseData(
                feed = Feed(
                    title = "OPDS Catalog Root Example",
                    type = 1,
                    href = Url("https://example.com")!!,
                    metadata = OpdsMetadata(
                        title = "OPDS Catalog Root Example",
                        modified = ("2010-01-10T10:03:10Z").toInstant()
                    ),
                    links = mutableListOf(
                        Link(
                            href = Href("https://example.com/opds-catalogs/root.xml")!!,
                            mediaType = MediaType(
                                "application/atom+xml;profile=opds-catalog;kind=navigation"
                            )!!,
                            rels = setOf("self"),
                            properties = Properties()
                        ),
                        Link(
                            href = Href("https://example.com/opds-catalogs/root.xml")!!,
                            mediaType = MediaType(
                                "application/atom+xml;profile=opds-catalog;kind=navigation"
                            )!!,
                            rels = setOf("start")
                        )
                    ),
                    navigation = mutableListOf(
                        Link(
                            href = Href("https://example.com/opds-catalogs/popular.xml")!!,
                            mediaType = MediaType(
                                "application/atom+xml;profile=opds-catalog;kind=acquisition"
                            )!!,
                            title = "Popular Publications",
                            rels = setOf("http://opds-spec.org/sort/popular")
                        ),
                        Link(
                            href = Href("https://example.com/opds-catalogs/new.xml")!!,
                            mediaType = MediaType(
                                "application/atom+xml;profile=opds-catalog;kind=acquisition"
                            )!!,
                            title = "New Publications",
                            rels = setOf("http://opds-spec.org/sort/new")
                        ),
                        Link(
                            href = Href("https://example.com/opds-catalogs/unpopular.xml")!!,
                            mediaType = MediaType(
                                "application/atom+xml;profile=opds-catalog;kind=acquisition"
                            )!!,
                            title = "Unpopular Publications",
                            rels = setOf("subsection")
                        )
                    )
                ),
                publication = null,
                type = 1
            ),
            parseData
        )
    }

    @Test fun `parse acquisition feed`() {
        val parseData = parse("acquisition-feed.atom")
        assertEquals(1, parseData.type)
        assertNull(parseData.publication)
        assertNotNull(parseData.feed)
        val feed = parseData.feed!!
        assertEquals("Unpopular Publications", feed.title)
        assertEquals(1, feed.type)
        assertEquals(Url("https://example.com"), feed.href)
        assertEquals(
            OpdsMetadata(
                title = "Unpopular Publications",
                modified = ("2010-01-10T10:01:11Z").toInstant()
            ),
            feed.metadata
        )
        assertEquals(
            mutableListOf(
                Link(
                    href = Href("https://example.com/opds-catalogs/vampire.farming.xml")!!,
                    mediaType = MediaType(
                        "application/atom+xml;profile=opds-catalog;kind=acquisition"
                    )!!,
                    rels = setOf("related")
                ),
                Link(
                    href = Href("https://example.com/opds-catalogs/unpopular.xml")!!,
                    mediaType = MediaType(
                        "application/atom+xml;profile=opds-catalog;kind=acquisition"
                    )!!,
                    rels = setOf("self")
                ),
                Link(
                    href = Href("https://example.com/opds-catalogs/root.xml")!!,
                    mediaType = MediaType(
                        "application/atom+xml;profile=opds-catalog;kind=navigation"
                    )!!,
                    rels = setOf("start")
                ),
                Link(
                    href = Href("https://example.com/opds-catalogs/root.xml")!!,
                    mediaType = MediaType(
                        "application/atom+xml;profile=opds-catalog;kind=navigation"
                    )!!,
                    rels = setOf("up")
                )
            ),
            feed.links
        )
        assertEquals(
            mutableListOf(
                Facet(
                    title = "Categories",
                    metadata = OpdsMetadata(title = "Categories"),
                    links = mutableListOf(
                        Link(
                            href = Href("https://example.com/sci-fi")!!,
                            title = "Science-Fiction",
                            rels = setOf("http://opds-spec.org/facet")
                        ),
                        Link(
                            href = Href("https://example.com/romance")!!,
                            title = "Romance",
                            rels = setOf("http://opds-spec.org/facet"),
                            properties = Properties(mapOf("numberOfItems" to 600))
                        )
                    )
                )
            ),
            feed.facets
        )
        assertEquals(2, feed.publications.size)

        assertJSONEquals(
            Manifest(
                metadata = Metadata(
                    identifier = "urn:uuid:6409a00b-7bf2-405e-826c-3fdff0fd0734",
                    localizedTitle = LocalizedString("Bob, Son of Bob"),
                    modified = ("2010-01-10T10:01:11Z").toInstant(),
                    published = null,
                    languages = listOf("en"),
                    subjects = listOf(
                        Subject(
                            localizedName = LocalizedString("FICTION / Men's Adventure"),
                            scheme = "http://www.bisg.org/standards/bisac_subject/index.html",
                            code = "FIC020000"
                        )
                    ),
                    authors = listOf(
                        Contributor(
                            localizedName = LocalizedString("Bob the Recursive"),
                            links = listOf(
                                Link(href = Href("http://opds-spec.org/authors/1285")!!)
                            )
                        )
                    ),
                    description = "The story of the son of the Bob and the gallant part he played in the lives of a man and a woman."
                ),
                links = listOf(
                    Link(
                        href = Href("https://example.com/covers/4561.thmb.gif")!!,
                        mediaType = MediaType("image/gif")!!,
                        rels = setOf("http://opds-spec.org/image/thumbnail")
                    ),
                    Link(
                        href = Href(
                            "https://example.com/opds-catalogs/entries/4571.complete.xml"
                        )!!,
                        mediaType = MediaType(
                            "application/atom+xml;type=entry;profile=opds-catalog"
                        )!!,
                        title = "Complete Catalog Entry for Bob, Son of Bob",
                        rels = setOf("alternate")
                    ),
                    Link(
                        href = Href("https://example.com/content/free/4561.epub")!!,
                        mediaType = MediaType("application/epub+zip")!!,
                        rels = setOf("http://opds-spec.org/acquisition")
                    ),
                    Link(
                        href = Href("https://example.com/content/free/4561.mobi")!!,
                        mediaType = MediaType("application/x-mobipocket-ebook")!!,
                        rels = setOf("http://opds-spec.org/acquisition")
                    )
                ),
                subcollections = mapOf(
                    "images" to listOf(
                        PublicationCollection(
                            links = listOf(
                                Link(
                                    href = Href("https://example.com/covers/4561.lrg.png")!!,
                                    mediaType = MediaType("image/png")!!,
                                    rels = setOf("http://opds-spec.org/image")
                                )
                            )
                        )
                    )
                )
            ).toJSON(),
            feed.publications[0].manifest.toJSON()
        )

        assertJSONEquals(
            Manifest(
                metadata = Metadata(
                    identifier = "urn:uuid:7b595b0c-e15c-4755-bf9a-b7019f5c1dab",
                    localizedTitle = LocalizedString("Modern Online Philately"),
                    modified = ("2010-01-10T10:01:10Z").toInstant(),
                    languages = listOf("en"),
                    authors = listOf(
                        Contributor(
                            localizedName = LocalizedString("Stampy McGee"),
                            links = listOf(
                                Link(href = Href("http://opds-spec.org/authors/21285")!!)
                            )
                        ),
                        Contributor(
                            localizedName = LocalizedString("Alice McGee"),
                            links = listOf(
                                Link(href = Href("http://opds-spec.org/authors/21284")!!)
                            )
                        ),
                        Contributor(
                            localizedName = LocalizedString("Harold McGee"),
                            links = listOf(
                                Link(href = Href("http://opds-spec.org/authors/21283")!!)
                            )
                        )
                    ),
                    publishers = listOf(
                        Contributor(localizedName = LocalizedString("StampMeOnline, Inc."))
                    ),
                    description = "The definitive reference for the web-curious philatelist."
                ),
                links = listOf(
                    Link(
                        href = Href("https://example.com/content/buy/11241.epub")!!,
                        mediaType = MediaType("application/epub+zip")!!,
                        rels = setOf("http://opds-spec.org/acquisition/buy"),
                        properties = Properties(
                            mapOf("price" to mapOf("currency" to "USD", "value" to 18.99))
                        )
                    )
                ),
                subcollections = mapOf(
                    "images" to listOf(
                        PublicationCollection(
                            links = listOf(
                                Link(
                                    href = Href("https://example.com/covers/11241.lrg.jpg")!!,
                                    mediaType = MediaType("image/jpeg")!!,
                                    rels = setOf("http://opds-spec.org/image")
                                )
                            )
                        )
                    )
                )
            ).toJSON(),
            feed.publications[1].manifest.toJSON()
        )
    }

    @Test fun `parse full entry`() {
        val parseData = parse("entry.atom")
        assertNull(parseData.feed)
        assertEquals(1, parseData.type)
        val publication = parseData.publication
        assertNotNull(publication)
        assertJSONEquals(
            Manifest(
                metadata = Metadata(
                    identifier = "urn:uuid:6409a00b-7bf2-405e-826c-3fdff0fd0734",
                    localizedTitle = LocalizedString("Bob, Son of Bob"),
                    modified = ("2010-01-10T10:01:11Z").toInstant(),
                    languages = listOf("en"),
                    subjects = listOf(
                        Subject(
                            localizedName = LocalizedString("FICTION / Men's Adventure"),
                            scheme = "http://www.bisg.org/standards/bisac_subject/index.html",
                            code = "FIC020000"
                        )
                    ),
                    authors = listOf(
                        Contributor(
                            localizedName = LocalizedString("Bob the Recursive"),
                            links = listOf(
                                Link(href = Href("http://opds-spec.org/authors/1285")!!)
                            )
                        )
                    ),
                    description = "The story of the son of the Bob and the gallant part he played in the lives of a man and a woman. Bob begins his humble life under the wandering eye of his senile mother, but quickly learns how to escape into the wilder world. Follow Bob as he uncovers his father's past and uses those lessons to improve the lives of others."
                ),
                links = listOf(
                    Link(
                        href = Href("https://example.com/covers/4561.thmb.gif")!!,
                        mediaType = MediaType("image/gif")!!,
                        rels = setOf("http://opds-spec.org/image/thumbnail")
                    ),
                    Link(
                        href = Href(
                            "https://example.com/opds-catalogs/entries/4571.complete.xml"
                        )!!,
                        mediaType = MediaType(
                            "application/atom+xml;type=entry;profile=opds-catalog"
                        )!!,
                        rels = setOf("self")
                    ),
                    Link(
                        href = Href("https://example.com/content/free/4561.epub")!!,
                        mediaType = MediaType("application/epub+zip")!!,
                        rels = setOf("http://opds-spec.org/acquisition")
                    ),
                    Link(
                        href = Href("https://example.com/content/free/4561.mobi")!!,
                        mediaType = MediaType("application/x-mobipocket-ebook")!!,
                        rels = setOf("http://opds-spec.org/acquisition")
                    )
                ),
                subcollections = mapOf(
                    "images" to listOf(
                        PublicationCollection(
                            links = listOf(
                                Link(
                                    href = Href("https://example.com/covers/4561.lrg.png")!!,
                                    mediaType = MediaType("image/png")!!,
                                    rels = setOf("http://opds-spec.org/image")
                                )
                            )
                        )
                    )
                )
            ).toJSON(),
            publication!!.manifest.toJSON()
        )
    }

    @Test fun `a templated search link is the search URL template`() {
        val feed = parse("calibre-root.atom", Url("http://localhost:8080/opds")!!).feed!!

        val template = runBlocking { OPDS1Parser.retrieveOpenSearchTemplate(feed) }.getOrNull()

        assertEquals("http://localhost:8080/opds/search/{searchTerms}?library_id=library", template)
        assertEquals(
            Url("http://localhost:8080/opds/navcatalog/4f7469746c65?library_id=library"),
            feed.navigation.single().url()
        )
    }

    @Test fun `the OpenSearch template for feed results is found`() {
        val template = retrieveGutenbergTemplate(
            selfType = "application/atom+xml;profile=opds-catalog"
        )

        assertEquals("http://m.gutenberg.org/ebooks/search.opds/?query={searchTerms}", template)
    }

    @Test fun `without the feed's own type the OpenSearch template for Atom results is found`() {
        val template = retrieveGutenbergTemplate(selfType = null)

        assertEquals("http://m.gutenberg.org/ebooks/search.opds/?query={searchTerms}", template)
    }

    private fun retrieveGutenbergTemplate(selfType: String?): String? {
        val selfLink = selfType
            ?.let { "<link rel=\"self\" type=\"$it\" href=\"/ebooks/search.opds/\"/>" }
            .orEmpty()
        val feedXml = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>All Books</title>
              <id>http://www.gutenberg.org/ebooks/search.opds/</id>
              <updated>2026-09-27T21:44:00Z</updated>
              $selfLink
              <link rel="search" type="application/opensearchdescription+xml" href="https://www.gutenberg.org/catalog/osd-books.xml"/>
            </feed>
        """.trimIndent()
        val feed = OPDS1Parser.parse(feedXml.toByteArray(), Url("https://www.gutenberg.org/ebooks/search.opds/")!!).feed!!
        val client = TestHttpClient(fixtures.bytesAt("gutenberg-osd.xml"))

        return runBlocking { OPDS1Parser.retrieveOpenSearchTemplate(feed, client) }.getOrNull()
    }

    private class TestHttpClient(private val body: ByteArray) : HttpClient {
        override suspend fun stream(request: HttpRequest): HttpTry<HttpStreamResponse> =
            Try.success(
                HttpStreamResponse(
                    HttpResponse(
                        request = request,
                        url = request.url,
                        statusCode = HttpStatus.Success,
                        headers = emptyMap(),
                        mediaType = null
                    ),
                    ByteArrayInputStream(body)
                )
            )
    }

    private fun parse(filename: String, url: Url = Url("https://example.com")!!): ParseData =
        OPDS1Parser.parse(fixtures.bytesAt(filename), url)
}
