package me.babaili.ajabberd.xml

import javax.xml.namespace.QName
import javax.xml.stream.events.{Attribute, XMLEvent, Namespace}
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

            def nameToXml(name:QName) = {
                val prefix = name.getPrefix()
                prefix.isEmpty() match {
                    case true =>
                        name.getLocalPart()
                    case false =>
                        s"${name.getPrefix()}:${name.getLocalPart()}"
                }
            }

            def attributesToXml(attributes: java.util.Iterator[_]) = {
                val sb = new StringBuilder()
                if(attributes.hasNext()) {
                    val next = attributes.next().asInstanceOf[Attribute]
                    sb.append(" ")
                    sb.append(nameToXml(next.getName()))
                    sb.append("=")
                    sb.append("\"")
                    sb.append(next.getValue())
                    sb.append("\"")
                }
                sb.toString()
            }

            def namespacesToXml(attributes: java.util.Iterator[_]) = {
                val sb = new StringBuilder()
                if(attributes.hasNext()) {
                    val next = attributes.next().asInstanceOf[Namespace]
                    sb.append(" ")
                    sb.append(nameToXml(next.getName()))
                    sb.append("=")
                    sb.append("\"")
                    sb.append(next.getValue())
                    sb.append("\"")
                }
                sb.toString()
            }


            def xmlEventToXml(event:XMLEvent) = {
                event.getEventType() match {
                    case XMLStreamConstants.START_ELEMENT =>
                        val se = event.asStartElement()
                        val sb = new StringBuilder()
                        sb.append("<")
                        sb.append(nameToXml(se.getName()))
                        sb.append(attributesToXml(se.getAttributes()))
                        sb.append(namespacesToXml(se.getNamespaces()))
                        val prefix = se.getName().getPrefix()
                        if (prefix != null && !prefix.isEmpty()) {
                            val namespaceURI = se.getNamespaceURI(prefix)
                            if(namespaceURI != null && !namespaceURI.isEmpty()) {
                                sb.append(" ")
                                sb.append("xmlns:")
                                sb.append(prefix)
                                sb.append("=")
                                sb.append("\"")
                                sb.append(namespaceURI)
                                sb.append("\"")
                            }
                        }
                        sb.append(">")
                        sb.toString()
                    case XMLStreamConstants.END_ELEMENT =>
                        val ee = event.asEndElement()
                        val sb = new StringBuilder()
                        sb.append("</")
                        sb.append(nameToXml(ee.getName()))
                        sb.append(">")
                        sb.toString()

                }
            }


            def combile(prefix:String, localPart:String, namespaceURI: String, end: Boolean) = {
                val endChar = if(end) "/" else ""
                prefix.isEmpty() match {
                    case true =>
                        s"<${endChar}${localPart}'>"
                    case false =>
                        s"<${endChar}${prefix}:${localPart}'>"
                }
            }

            def outputXmlEvents(xmlEvents: List[XMLEvent]): Unit = {
                xmlEvents match {
                    case head :: tail =>
                        //
                        head.getEventType() match {
                            case XMLStreamConstants.ATTRIBUTE =>
                                println(s"attribute ${head.getSchemaType().getLocalPart()}")
                            case XMLStreamConstants.CDATA => println("cdata")
                            case XMLStreamConstants.CHARACTERS =>
                                println("characters")
                                val se = head.asCharacters()
                                println(s"${se.getData()}")
                            case XMLStreamConstants.COMMENT => println("comment")
                            case XMLStreamConstants.DTD => println("dtd")
                            case XMLStreamConstants.END_DOCUMENT => println("end document")
                            case XMLStreamConstants.END_ELEMENT =>
                                println("end element")
                                println(xmlEventToXml(head))
                            case XMLStreamConstants.ENTITY_DECLARATION => println("entity declaration")
                            case XMLStreamConstants.ENTITY_REFERENCE => println("entity reference")
                            case XMLStreamConstants.NAMESPACE => println("namespace")
                            case XMLStreamConstants.NOTATION_DECLARATION => println("notation declaration")
                            case XMLStreamConstants.PROCESSING_INSTRUCTION => println("processing instruction")
                            case XMLStreamConstants.SPACE => println("space")
                            case XMLStreamConstants.START_DOCUMENT =>
                                println("start document")
                            case XMLStreamConstants.START_ELEMENT =>
                                println("start element")
                                println(xmlEventToXml(head))
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
