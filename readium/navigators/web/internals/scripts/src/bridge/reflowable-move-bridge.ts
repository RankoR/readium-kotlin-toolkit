import { getCssSelector } from "css-selector-generator"
import { TextQuoteAnchor } from "../vendor/hypothesis/annotator/anchoring/types"
import { log } from "../util/log"

export class ReflowableMoveBridge {
  readonly document: HTMLDocument

  constructor(document: HTMLDocument) {
    this.document = document
  }

  getOffsetForLocation(location: string, vertical: boolean): number | null {
    const actualLocation = parseLocation(location)

    if (actualLocation.textAfter || actualLocation.textBefore) {
      return this.getOffsetForTextAnchor(
        actualLocation.textBefore ?? "",
        actualLocation.textAfter ?? "",
        vertical
      )
    }

    if (actualLocation.cssSelector) {
      return this.getOffsetForCssSelector(actualLocation.cssSelector, vertical)
    }

    if (actualLocation.htmlId) {
      return this.getOffsetForHtmlId(actualLocation.htmlId, vertical)
    }

    return null
  }

  /**
   * Returns the CSS selector of the first block element visible on the screen, even if
   * it begins on a previous screen page, or null when there is no such element.
   */
  getFirstVisibleElement(vertical: boolean): string | null {
    const element = this.findFirstVisibleElement(this.document.body, vertical)
    if (element === this.document.body) {
      return null
    }

    return getCssSelector(element)
  }

  private findFirstVisibleElement(root: Element, vertical: boolean): Element {
    for (const child of Array.from(root.children)) {
      if (this.isVisibleBlock(child, vertical)) {
        return this.findFirstVisibleElement(child, vertical)
      }
    }
    return root
  }

  private isVisibleBlock(element: Element, vertical: boolean): boolean {
    const style = getComputedStyle(element)
    if (style.display != "block" || style.opacity === "0") {
      return false
    }

    const rect = element.getBoundingClientRect()
    if (vertical) {
      return rect.bottom > 0 && rect.top < window.innerHeight
    } else {
      return rect.right > 0 && rect.left < window.innerWidth
    }
  }

  private getOffsetForTextAnchor(
    textBefore: string,
    textAfter: string,
    vertical: boolean
  ): number | null {
    const root = this.document.body

    const textAfterStart = Math.min(1, textAfter.length)

    const textBeforeEnd = Math.max(0, textBefore.length - 1)

    const anchor = new TextQuoteAnchor(
      root,
      textBefore.substring(textBeforeEnd) +
        textAfter.substring(0, textAfterStart),
      {
        prefix: textBefore.substring(0, textBeforeEnd),
        suffix: textAfter.substring(textAfterStart),
      }
    )

    try {
      const range = anchor.toRange()
      return this.getOffsetForRect(range.getBoundingClientRect(), vertical)
    } catch (e) {
      log(e)
      return null
    }
  }

  private getOffsetForCssSelector(
    cssSelector: string,
    vertical: boolean
  ): number | null {
    let element
    try {
      element = this.document.querySelector(cssSelector)
    } catch (e) {
      log(e)
    }

    if (!element) {
      return null
    }

    return this.getOffsetForElement(element, vertical)
  }

  private getOffsetForHtmlId(htmlId: string, vertical: boolean): number | null {
    const element = this.document.getElementById(htmlId)
    if (!element) {
      return null
    }

    return this.getOffsetForElement(element, vertical)
  }

  private getOffsetForElement(element: Element, vertical: boolean): number {
    const rect = element.getBoundingClientRect()
    return this.getOffsetForRect(rect, vertical)
  }

  private getOffsetForRect(rect: DOMRect, vertical: boolean): number {
    if (vertical) {
      return rect.top + window.scrollY
    } else {
      const offset = rect.left + window.scrollX
      return offset
    }
  }
}

interface Location {
  progression: number
  htmlId: string
  cssSelector: string
  textBefore: string
  textAfter: string
}

function parseLocation(location: string): Location {
  const jsonLocation: Location = JSON.parse(location)
  return jsonLocation
}
