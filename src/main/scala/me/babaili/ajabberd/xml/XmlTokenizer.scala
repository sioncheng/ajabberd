package me.babaili.ajabberd.xml

import javax.xml.stream.events.XMLEvent

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
                    emit(xmlEvents.tail)
                case false =>
                    head.isStartElement() match {
                        case true =>
                            //
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
                                while(!tails.isEmpty && !paired){
                                    if(tails.head.isEndElement()) {
                                        if(tails.head.asEndElement().getName().getLocalPart().equalsIgnoreCase(xml)) {
                                            paired = true
                                        }
                                    } else {
                                        stack.push(tails.head)
                                        tails = tails.tail
                                    }
                                }
                                if(paired) {
                                    logger.debug(s"paired ${xml}")
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
