package me.babaili.ajabberd.xml

import javax.xml.namespace.QName
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.events.{Attribute, Namespace, XMLEvent}

import com.fasterxml.aalto.AsyncXMLStreamReader
import com.fasterxml.aalto.evt.EventAllocatorImpl
import com.fasterxml.aalto.stax.InputFactoryImpl
import me.babaili.ajabberd.protocol._
import me.babaili.ajabberd.util.Logger

import scala.annotation.tailrec
import scala.collection.{mutable}
import scala.xml.pull._
import scala.xml._

/**
  * Created by chengyongqiao on 13/01/2017.
  *
  *
  */

object XMPPXMLTokenizer {

    val logger = new Logger{}

    @tailrec
    def emit(xmlEvents:List[XMLEvent]): (List[Packet], List[XMLEvent] ) = {
        if(xmlEvents.isEmpty) {
            (List(NullPacket), xmlEvents)
        } else {
            val head = xmlEvents(0)
            head.isStartDocument() match {
                case true =>
                    logger.debug("start document")
                    emit(xmlEvents.tail)
                case false =>
                    head.isStartElement() match {
                        case true =>
                            //
                            logger.debug("start element")
                            val headQName = head.asStartElement().getName()
                            val eventXml = xmlEventToXml(head)
                            if (StreamHead.qualifies(eventXml)) {
                                (List(StreamHead(eventXml)), xmlEvents.tail)
                            } else if(StreamTail.qualifies(eventXml)) {
                                (List(StreamTail()), xmlEvents.tail)
                            } else {
                                import scala.collection.mutable.Stack
                                val stack = Stack[XMLEvent]()
                                var tails = xmlEvents.tail
                                var paired = false
                                stack.push(head)
                                while(!tails.isEmpty && !paired){
                                    if(tails.head.isEndElement()) {
                                        val endQName = tails.head.asEndElement().getName()
                                        if(endQName.equals(headQName)) {
                                            paired = true
                                        } else {
                                            //
                                        }
                                    } else {
                                        //
                                    }

                                    stack.push(tails.head)
                                    tails = tails.tail
                                }
                                if(paired) {
                                    val totalXml = xmlEventsToXml(stack.toList.reverse)
                                    logger.debug(s"paired ${totalXml}")
                                    (toPacket(totalXml), tails)
                                } else {
                                    (List(NullPacket), xmlEvents)
                                }
                            }
                        case false =>
                            //
                            (List(NullPacket), xmlEvents)
                    }
            }

        }
    }

    def xmlEventsToXml(xmlEvents: List[XMLEvent]) = {
        val sb = new StringBuilder()
        xmlEvents.foreach(x => sb.append(xmlEventToXml(x)))
        sb.toString()
    }

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
        while (attributes.hasNext()) {
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
            case XMLStreamConstants.START_DOCUMENT =>
                ""
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
            case XMLStreamConstants.CHARACTERS =>
                event.asCharacters().getData()

        }
    }

    def toPacket(totalXml: String) = {
        val parsedResult = parseXml(totalXml)

        parsedResult match {
            case Some(xmls) =>
            {
                val ouput = xmls.map( xml =>
                {
                    xml.namespace match
                    {
                        case Tls.namespace => xml.label match
                        {
                            case StartTls.tag => StartTls(xml)
                            case TlsProceed.tag => TlsProceed(xml)
                            case TlsFailure.tag => TlsFailure(xml)
                            case _ => throw new Exception("unknown tls packet %s".format(xml.label))
                        }
                        case Sasl.namespace => xml.label match
                        {
                            case SaslAuth.tag => SaslAuth(xml)
                            case SaslSuccess.tag => SaslSuccess(xml)
                            case SaslAbort.tag => SaslAbort(xml)
                            case SaslError.tag => SaslError(xml)
                            case SaslResponse.tag => SaslResponse(xml)
                            case _ => throw new Exception("unknown sasl packet %s".format(xml.label))
                        }
                        case _ => xml.label match
                        {
                            case Features.tag => Features(xml)
                            case Handshake.tag => Handshake(xml)
                            case StreamError.tag => StreamError(xml)
                            case _ => Stanza(xml)
                        }
                    }
                })

                ouput.toList
            }
            case None =>
            {
                List(NullPacket)
            }
        }
    }

    def parseXml(input:String):Option[Seq[Node]] = {
        var level = 0
        val children = mutable.HashMap[Int, mutable.ListBuffer[Node]]()
        val attributes = mutable.HashMap[Int, MetaData]()
        val scope = mutable.HashMap[Int, NamespaceBinding]()
        val nodes = mutable.ListBuffer[Node]()

        try
        {
            // using a customized version of XMLEventReadr as it is buggy, see
            // http://scala-programming-language.1934581.n4.nabble.com/OutOfMemoryError-when-using-XMLEventReader-td2341263.html
            //  should be fixed in scala 2.9, need to test when it is released
            val tokenizer = new XMLEventReaderEx(scala.io.Source.fromString(input))
            tokenizer.foreach( token =>
            {
                token match
                {
                    case tag:EvText => children(level) += new Text(tag.text)
                    case tag:EvProcInstr => children(level) += new ProcInstr(tag.target, tag.text)
                    case tag:EvComment => children(level) += new Comment(tag.text)
                    case tag:EvEntityRef => children(level) += new EntityRef(tag.entity)
                    case tag:EvElemStart =>
                    {
                        level += 1
                        if (!attributes.contains(level)) attributes += level -> tag.attrs else attributes(level) = tag.attrs
                        if (!scope.contains(level)) scope += level -> tag.scope else scope(level) = tag.scope
                        if (!children.contains(level)) children += level -> new mutable.ListBuffer[Node]() else children(level) = new mutable.ListBuffer[Node]()
                    }
                    case tag:EvElemEnd =>
                    {
                        val node = Elem(tag.pre, tag.label, attributes(level), scope(level), children(level):_*)

                        level -= 1
                        if (0 == level)
                        {
                            nodes += node
                        }
                        else
                        {
                            children(level) += node
                        }
                    }
                }
            })

            return if (nodes.length > 0) Some(nodes) else None
        }
        catch
            {
                // TODO: would be nice to handle bad vs. partial xml, only the latter is important to us (for buffering)
                case e: scala.xml.parsing.FatalError => None
                case e: Throwable => throw e
            }
    }
}

class XmlTokenizer {

    val factory = new InputFactoryImpl()
    val allocator = EventAllocatorImpl.getDefaultInstance()
    val reader = factory.createAsyncForByteArray()

    def decode(data: Array[Byte]): Either[List[XMLEvent], Exception] = {

        try {

            var result: List[XMLEvent] = List.empty
            reader.getInputFeeder().feedInput(data, 0, data.length)

            var hasXmlFrame = reader.hasNext() && reader.next() != AsyncXMLStreamReader.EVENT_INCOMPLETE
            while (hasXmlFrame) {
                val xmlEvent = allocator.allocate(reader)

                result = result :+ xmlEvent

                hasXmlFrame = reader.hasNext() && reader.next() != AsyncXMLStreamReader.EVENT_INCOMPLETE
            }

            Left(result)

        } catch {
            case ex: Exception => Right(ex)
        }

    }

}
