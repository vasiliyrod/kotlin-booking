package edu.booking.hotel_booking.dao


interface Dao<K, E> {
    fun create(entity: E): E?
    fun findById(id: K): E?
    fun update(entity: E): Boolean
    fun delete(id: K): Boolean
}
