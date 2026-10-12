/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.css

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalReadiumApi::class)
class ReadiumCssInjectorTest {

    private val sut = ReadiumCssInjector(
        rsProperties = RsProperties(),
        userProperties = UserProperties(),
        fontFamilyDeclarations = emptyList(),
        googleFonts = emptyList(),
        assetsBaseHref = AbsoluteUrl("https://assets/")!!,
        readiumCssAssets = RelativeUrl("readium/readium-css/")!!
    )

    private val styledHtml = """
        <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <title>Publication</title>
                <link rel="stylesheet" href="style.css" type="text/css"/>
            </head>
            <body><p><img src="a.jpg"/> Text</p></body>
        </html>
    """.trimIndent()

    private val unstyledHtml = """
        <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>Publication</title></head>
            <body><p>Text</p></body>
        </html>
    """.trimIndent()

    @Test
    fun `A paragraph that starts with an image is not indented`() {
        val html = sut.injectHtml(styledHtml)

        assertTrue(html.contains("p:has(> :is(img, svg):first-child) { text-indent: 0 !important; }"), html)
    }

    @Test
    fun `The rule is in a layer, to beat the unlayered important rules of Readium CSS`() {
        val html = sut.injectHtml(styledHtml)

        val rule = html.indexOf("p:has(> :is(img, svg):first-child)")
        val layer = html.lastIndexOf("@layer readium-fixes", rule)
        assertTrue(layer != -1 && layer < rule, html)
    }

    @Test
    fun `The rule comes after Readium CSS and the style of the book, so that it is the last word`() {
        val html = sut.injectHtml(styledHtml)

        val rule = html.indexOf("p:has(> :is(img, svg):first-child)")
        assertTrue(html.indexOf("href=\"style.css\"") < rule, html)
        assertTrue(html.indexOf("ReadiumCSS-after.css") < rule, html)
    }

    @Test
    fun `The rule does not change whether an unstyled document gets the default stylesheet`() {
        assertTrue(sut.injectHtml(unstyledHtml).contains("ReadiumCSS-default.css"))
        assertFalse(sut.injectHtml(styledHtml).contains("ReadiumCSS-default.css"))
    }

    @Test
    fun `The rule is injected once`() {
        val html = sut.injectHtml(styledHtml)

        assertEquals(1, Regex("@layer readium-fixes").findAll(html).count())
    }
}
