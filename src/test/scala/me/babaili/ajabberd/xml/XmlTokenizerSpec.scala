package me.babaili.ajabberd.xml

import org.scalatest.{FunSpec, FunSpecLike, Matchers}

/**
  * Created by chengyongqiao on 13/01/2017.
  */
class XmlTokenizerSpec extends FunSpecLike with Matchers {

    describe("given a xml tokenizer") {

        it("should be able to parse [<abc/>]") {
            val xmlTokenizer = new XmlTokenizer()
            val resultEither = xmlTokenizer.decode("<abc/>".getBytes())
            resultEither.isLeft should equal(true)
            resultEither match {
                case Left(result) =>
                    result.length should equal (3)
                    result.head.isStartDocument () should equal (true)
                    result.tail.head.isStartElement should equal (true)
                    result.tail.head.asStartElement ().getName ().getLocalPart () should equal ("abc")
                    result.tail.tail.head.isEndElement should equal (true)
                case Right(_) =>
                    assert(false)
            }
        }


        it("should be able to parse [<abc />]") {
            val xmlTokenizer = new XmlTokenizer()
            val resultEither = xmlTokenizer.decode("<abc />".getBytes())
            resultEither.isLeft should equal(true)
            resultEither match {
                case Left(result) =>
                    result.length should equal (3)
                    result.head.isStartDocument () should equal (true)
                    result.tail.head.isStartElement should equal (true)
                    result.tail.head.asStartElement ().getName ().getLocalPart () should equal ("abc")
                    result.tail.tail.head.isEndElement should equal (true)
                case Right(_) =>
                    assert(false)
            }
        }


        it("should be able to ignore [abc]") {
            val xmlTokenizer = new XmlTokenizer()
            val resultEither = xmlTokenizer.decode("abc".getBytes())
            resultEither.isRight should equal(true)
        }


        it("should be able to ignore [abc/>]") {
            val xmlTokenizer = new XmlTokenizer()
            val resultEither = xmlTokenizer.decode("abc/>".getBytes())
            resultEither.isRight should equal(true)
        }

        it("should be able to parse <a>t") {
            val xmlTokenizer = new XmlTokenizer()
            val resultEither = xmlTokenizer.decode("<a>t".getBytes())
            resultEither.isLeft should equal(true)
            resultEither match {
                case Left(result) =>
                    result.length should equal(3)
                    result.head.isStartDocument() should equal(true)
                    result.tail.head.isStartElement() should equal(true)
                    result.tail.head.asStartElement().getName().getLocalPart() should equal("a")
                    result.tail.tail.head.isCharacters() should equal(true)
                    result.tail.tail.head.asCharacters().getData() should equal("t")
                case Right(_) => assert(false)
            }
        }
    }

}
