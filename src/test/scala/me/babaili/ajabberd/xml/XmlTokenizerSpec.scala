package me.babaili.ajabberd.xml

import javax.xml.stream.events.XMLEvent
import javax.xml.stream.XMLStreamConstants

import org.scalatest.{FunSpecLike, Matchers}

/**
  * Created by chengyongqiao on 13/01/2017.
  */
class XmlTokenizerSpec extends FunSpecLike with Matchers {

    describe("given a xml tokenizer") {

        it("should be able to parse [<abc/>]") {
            val xmlTokenizer = new XmlTokenizer()
            val resultEither = xmlTokenizer.decode("<abc/>".getBytes())
            resultEither match {
                case Left(result) =>
                    result.length should equal (3)
                    result.head.isStartDocument () should equal (true)
                    result.tail.head.isStartElement should equal (true)
                    result.tail.head.asStartElement ().getName ().getLocalPart () should equal ("abc")
                    result.tail.tail.head.isEndElement should equal (true)
                case Right(x) =>
                    println("======= exception: " + x.getMessage())
                    assert(false)
            }
        }


        it("should be able to parse [<abc />]") {
            val xmlTokenizer = new XmlTokenizer()
            val resultEither = xmlTokenizer.decode("<abc />".getBytes())
            resultEither match {
                case Left(result) =>
                    result.length should equal (3)
                    result.head.isStartDocument () should equal (true)
                    result.tail.head.isStartElement should equal (true)
                    result.tail.head.asStartElement ().getName ().getLocalPart () should equal ("abc")
                    result.tail.tail.head.isEndElement should equal (true)
                case Right(x) =>
                    println("======= exception: " + x.getMessage())
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
            resultEither match {
                case Left(result) =>
                    result.length should equal(3)
                    result.head.isStartDocument() should equal(true)
                    result.tail.head.isStartElement() should equal(true)
                    result.tail.head.asStartElement().getName().getLocalPart() should equal("a")
                    result.tail.tail.head.isCharacters() should equal(true)
                    result.tail.tail.head.asCharacters().getData() should equal("t")
                case Right(x) =>
                    println("======= exception: " + x.getMessage())
                    assert(false)
            }
        }

        val startStream = "<?xml version=\"1.0\"?> \n" +
            "<stream:stream to=\"onepiece.com\" xmlns=\"jabber:client\" \n " +
            "xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">"

        val startTls = "<stream:features> " +
            " <starttls xmlns=\"urn:ietf:params:xml:ns: xmpp-tls\"></starttls> " +
            " </stream:features>"

        it("should be able to parse " + startStream + " " + startTls) {

            def outputXmlEvents(xmlEvents: List[XMLEvent]): Unit = {
                xmlEvents match {
                    case head :: tail =>
                        //
                        head.getEventType() match {
                            case XMLStreamConstants.ATTRIBUTE => println("attribute")
                            case XMLStreamConstants.CDATA => println("cdata")
                            case XMLStreamConstants.CHARACTERS => println("characters")
                            case XMLStreamConstants.COMMENT => println("comment")
                            case XMLStreamConstants.DTD => println("dtd")
                            case XMLStreamConstants.END_DOCUMENT => println("end document")
                            case XMLStreamConstants.END_ELEMENT => println("end element")
                            case XMLStreamConstants.ENTITY_DECLARATION => println("entity declaration")
                            case XMLStreamConstants.ENTITY_REFERENCE => println("entity reference")
                            case XMLStreamConstants.NAMESPACE => println("namespace")
                            case XMLStreamConstants.NOTATION_DECLARATION => println("notation declaration")
                            case XMLStreamConstants.PROCESSING_INSTRUCTION => println("processing instruction")
                            case XMLStreamConstants.SPACE => println("space")
                            case XMLStreamConstants.START_DOCUMENT => println("start document")
                            case XMLStreamConstants.START_ELEMENT => println("start element")
                        }

                        outputXmlEvents(tail)
                    case Nil =>
                        println("nil")
                }
            }

            val xmlTokenizer = new XmlTokenizer()
            val xml = startStream + startTls
            val resultEither = xmlTokenizer.decode(xml.getBytes())
            resultEither match {
                case Left(result) =>
                    result.length should equal(8)
                    val first = result.head
                    val last = result.tail.head
                    first.isStartDocument() should equal(true)
                    last.isStartElement() should equal(true)
                    last.asStartElement().getName().getLocalPart() should equal("stream")
                    outputXmlEvents(result)
                case Right(x) =>
                    println("======= exception: " + x.getMessage())
                    assert(false)
            }
        }

        val parseXmppStartStream1 = "<?xml version=\"1.0\"?> \n" +
            "<stream:stream to=\"onepiece.com\" xmlns=\"jabber:client\" \n "
        val parseXmppStartStream2 = "xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">"
        it ("should be able to parse '" + parseXmppStartStream1 + "' + '" + parseXmppStartStream2 + "'") {
            val xmlTokenizer = new XmlTokenizer()
            val resultEither1 = xmlTokenizer.decode(parseXmppStartStream1.getBytes())
            resultEither1 match {
                case Left(result) =>
                    result.length should equal(1)
                    result.head.isStartDocument() should equal(true)
                case Right(x) =>
                    println("======= exception: " + x.getMessage())
                    assert(false)
            }
            val resultEither2 = xmlTokenizer.decode(parseXmppStartStream2.getBytes())
            resultEither2.isLeft should equal(true)
            resultEither2 match {
                case Left(result) =>
                    result.length should equal(1)
                    result.head.isStartElement() should equal(true)
                    result.head.asStartElement().getName().getLocalPart() should equal("stream")
                case Right(_) => assert(false)
            }
        }
    }

}
