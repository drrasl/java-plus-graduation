package ru.practicum.stats.avro.deserializer;

import org.apache.avro.Schema;
import ru.practicum.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.avro.UserActionAvro;

public class EventsSimilarityDeserializer extends BaseAvroDeserializer<EventSimilarityAvro> {
    public EventsSimilarityDeserializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}
