package data.entity

import java.time.LocalDateTime
import java.util.UUID


class Booking(
    var id: UUID,
    var startDate: LocalDateTime,
    var endDate: LocalDateTime,
    var room: Room,
    var guests: MutableList<Guest> = mutableListOf()
)


