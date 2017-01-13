package me.babaili.ajabberd.xml

/**
  * Created by chengyongqiao on 13/01/2017.
  */
object XmlTokenizer {


    val init = 0
    val meetLeftArrow = 1
    val expectValidIdentifier = 2
    val expectRightSlash = 3
    val expectRightArrow = 4

    case class ParseResult(val xml: String, val remain: String, val isBroken: Boolean)

    def parse(xmlString: String): Option[ParseResult] = {

        var result:Option[ParseResult] = None

        var i = 0
        val leftArrowIndex = new scala.collection.mutable.ArrayBuffer[Int]()
        val rightArrowIndex = new scala.collection.mutable.ArrayBuffer[Int]()
        var shouldQuit = false
        var expectUnWhiteChar = false
        var status = init
        while ( i < xmlString.length() && shouldQuit == false) {
            status match {
                case `init` =>
                    println("init")
                    val ch = xmlString.charAt(i)
                    if (!isWhiteChar(ch)) {
                        if (ch == '<') {
                            leftArrowIndex.+=(i)
                            expectUnWhiteChar = true
                            status = meetLeftArrow
                            println("change to meetLeftArrow")
                        } else {
                            println("quit at", ch)
                            shouldQuit = true
                        }
                    }
                case `meetLeftArrow` =>
                    println("meetLeftArrow")
                    val ch = xmlString.charAt(i)
                    if (!isValidIdentifier(ch)) {
                        println("quit at", ch)
                        shouldQuit = true
                    } else {
                        status = expectValidIdentifier
                        println("change to expectValidIdentifier")
                    }
                case `expectValidIdentifier` =>
                    println("expectValidIdentifier")
                    val ch = xmlString.charAt(i)
                    if (isWhiteChar(ch)) {
                        status = expectRightSlash
                        println("change to expectRightSlash")
                    } else if(ch == '/') {
                        status = expectRightArrow
                        println("change to expectRightArrow")
                    } else if(!isValidIdentifier(ch)) {
                        println("quit at", ch)
                        shouldQuit = true
                    }
                case `expectRightSlash` =>
                    println("expectRightSlash")
                    val ch = xmlString.charAt(i)
                    if (ch == '/') {
                        status = expectRightArrow
                        println("change to expectRightArrow")
                    } else if(!isWhiteChar(ch)) {
                        println("quit at", ch)
                        shouldQuit = true
                    }
                case `expectRightArrow` =>
                    println("expectRightArrow")
                    val ch = xmlString.charAt(i)
                    if (ch == '>') {
                        rightArrowIndex.+=(i)
                    } else {
                        println("quit at", ch)
                        shouldQuit = true
                    }

            }
            i += 1
        }

        if(shouldQuit == true) {
            result = Some(ParseResult("", "", true))
        }
        else if (leftArrowIndex.size == 0) {
            result = None
        } else {
            if(leftArrowIndex.size > rightArrowIndex.size) {
                result = None
            } else {
                while(rightArrowIndex.size > leftArrowIndex.size) {
                    rightArrowIndex.dropRight(rightArrowIndex.size - 1)
                }
                val startIndex = leftArrowIndex.head
                val endIndex = rightArrowIndex.last
                val xml = xmlString.substring(startIndex, endIndex+1)
                var remain = ""
                if (xmlString.length() > endIndex) {
                    remain = xmlString.substring(endIndex+1)
                }

                result = Some(ParseResult(xml, remain, false))
            }
        }

        result
    }

    private def isWhiteChar(ch: Char) = {
        ch == ' ' || ch == '\r' || ch == '\n'
    }

    private def isValidIdentifier(ch: Char) = {
        !isWhiteChar(ch) && ch != '"' && ch != '>' && ch != '<'
    }

}
