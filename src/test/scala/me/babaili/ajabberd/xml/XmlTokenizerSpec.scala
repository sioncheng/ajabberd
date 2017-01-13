package me.babaili.ajabberd.xml

import org.scalatest.{FunSpec, FunSpecLike, Matchers}

/**
  * Created by chengyongqiao on 13/01/2017.
  */
class XmlTokenizerSpec extends FunSpecLike with Matchers {

    describe("given a xml tokenizer") {

        it("should be able to parse [<abc/>]") {
            val parseResult = XmlTokenizer.parse("<abc/>")
            parseResult.get.xml should equal("<abc/>")
            parseResult.get.remain should equal("")
        }

        it("should be able to parse [<abc />]") {
            val parseResult = XmlTokenizer.parse("<abc/>")
            parseResult.get.xml should equal("<abc/>")
            parseResult.get.remain should equal("")
        }

        it("should be able to ignore [<abc]") {
            val parseResult = XmlTokenizer.parse("<abc")
            parseResult should equal(None)
        }

        it("should be able to ignore [abc]") {
            val parseResult = XmlTokenizer.parse("abc")
            parseResult.get.isBroken should equal(true)
        }

        it("should be able to ignore [abc/>]") {
            val parseResult = XmlTokenizer.parse("abc/>")
            parseResult.get.isBroken should equal(true)
        }

        it("should be able to ignore [<abc>]") {
            val parseResult = XmlTokenizer.parse("<abc>")
            parseResult.get.isBroken should equal(true)
        }

        it("should be able to ignore [< abc/>]") {
            val parseResult = XmlTokenizer.parse("< abc/>")
            parseResult.get.isBroken should equal(true)
        }

        it("should be able to ignore [<ab c/>]") {
            val parseResult = XmlTokenizer.parse("< abc/>")
            parseResult.get.isBroken should equal(true)
        }
    }

}
