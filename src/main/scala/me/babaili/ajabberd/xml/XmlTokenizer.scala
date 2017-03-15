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

/**
  * Created by chengyongqiao on 13/01/2017.
  *
  *
  */

object XMPPXMLTokenizer {

    val logger = new Logger{}

    @tailrec
    def emit(xmlEvents:List[XMLEvent]): (Packet, List[XMLEvent] ) = {
        if(xmlEvents.isEmpty) {
            (NullPacket, xmlEvents)
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
                            val xml = head.asStartElement().getName().getLocalPart()
                            if (StreamHead.qualifies(xml)) {
                                (StreamHead(xml), xmlEvents.tail)
                            } else if(StreamTail.qualifies(xml)) {
                                (StreamTail(), xmlEvents.tail)
                            } else {
                                import scala.collection.mutable.Stack
                                val stack = Stack[XMLEvent]()
                                var tails = xmlEvents.tail
                                var paired = false
                                stack.push(head)
                                while(!tails.isEmpty && !paired){
                                    if(tails.head.isEndElement()) {
                                        if(tails.head.asEndElement().getName().getLocalPart().equalsIgnoreCase(xml)) {
                                            paired = true
                                            stack.push(tails.head)
                                        } else {
                                            stack.push(tails.head)
                                            tails = tails.tail
                                        }
                                    } else {
                                        stack.push(tails.head)
                                        tails = tails.tail
                                    }
                                }
                                if(paired) {
                                    val totalXml = xmlEventsToXml(stack.toList.reverse)
                                    logger.debug(s"paired ${totalXml}")
                                    (NullPacket, tails.tail)
                                } else {
                                    (NullPacket, xmlEvents)
                                }
                            }
                        case false =>
                            //
                            (NullPacket, xmlEvents)
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
            case XMLStreamConstants.CHARACTERS =>
                event.asCharacters().getData()

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
