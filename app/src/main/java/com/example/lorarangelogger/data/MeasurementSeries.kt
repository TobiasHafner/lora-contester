package com.example.lorarangelogger.data

data class MeasurementSeries(
    val label: String,
    val location: String,
    val description: String,
    val repetitions: Int,
    val delay: Int
) {
    private val _measurements = mutableListOf<Measurement>()
    val measurements: List<Measurement>
        get() = _measurements

    private var _numAnswered = 0
    val numAnswered: Int
        get() = _numAnswered


    fun makeMeasurement(): Long {
        val m = Measurement(System.currentTimeMillis())
        _measurements.add(m)
        return m.sendTime
    }

    fun handleAnswer(data: LoraStatSendData): Boolean {
        try {
            val m = _measurements.first { !it.hasAnswered && it.sendTime == data.sendTime }
            m.answer(data)
            _numAnswered++
            return true
        } catch (e: NoSuchElementException) {
            // couldn't find measurement
            return false
        }
    }

    fun allAnswered(): Boolean {
        return repetitions == _numAnswered
    }

    override fun toString(): String {
        return "lbl: $label, loc: $location, dsc: $description, #: $repetitions, dly: $delay, " +
                "ret/snt: $_numAnswered/${_measurements.size}\n $_measurements"
    }
}
