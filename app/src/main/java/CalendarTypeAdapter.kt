import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.text.SimpleDateFormat
import java.util.*

class CalendarTypeAdapter : TypeAdapter<Calendar>() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    override fun write(out: JsonWriter, value: Calendar?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(dateFormat.format(value.time))
        }
    }

    override fun read(`in`: JsonReader): Calendar? {
        return if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            null
        } else {
            val date = dateFormat.parse(`in`.nextString())
            Calendar.getInstance().apply { time = date }
        }
    }
}