package tech.ecom.booking.unit.validation

import dto.RoomCreateDto
import dto.RoomEditDto
import dto.RoomReadDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("Валидация DTO комнат")
class RoomValidationTests {

    @Test
    fun `RoomCreateDto valid data should pass validation`() {
        //given
        val dto = RoomCreateDto(floor = 5, number = "101", capacity = 4)

        //when
        val result = dto.validate()

        //then
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `RoomCreateDto invalid floor should fail validation`() {
        //given
        val dtoLow = RoomCreateDto(floor = -101, number = "101", capacity = 4)
        val dtoHigh = RoomCreateDto(floor = 301, number = "101", capacity = 4)

        //when then
        assertFalse(dtoLow.validate().isValid)
        assertFalse(dtoHigh.validate().isValid)
    }

    @Test
    fun `RoomCreateDto invalid number should fail validation`() {
        //given
        val dtoEmpty = RoomCreateDto(floor = 5, number = "", capacity = 4)
        val dtoBlank = RoomCreateDto(floor = 5, number = "   ", capacity = 4)
        val dtoLong = RoomCreateDto(floor = 5, number = "A".repeat(51), capacity = 4)

        //when then
        assertFalse(dtoEmpty.validate().isValid)
        assertFalse(dtoBlank.validate().isValid)
        assertFalse(dtoLong.validate().isValid)
    }

    @Test
    fun `RoomCreateDto invalid capacity should fail validation`() {
        //given
        val dtoZero = RoomCreateDto(floor = 5, number = "101", capacity = 0)
        val dtoNegative = RoomCreateDto(floor = 5, number = "101", capacity = -5)
        val dtoTooHigh = RoomCreateDto(floor = 5, number = "101", capacity = 101)

        //when then
        assertFalse(dtoZero.validate().isValid)
        assertFalse(dtoNegative.validate().isValid)
        assertFalse(dtoTooHigh.validate().isValid)
    }

    @Test
    fun `RoomCreateDto null capacity should pass validation`() {
        //given
        val dto = RoomCreateDto(floor = 5, number = "101", capacity = null)

        //when then
        assertTrue(dto.validate().isValid)
    }

    @Test
    fun `RoomCreateDto multiple validation errors should be returned`() {
        //given
        val dto = RoomCreateDto(floor = -101, number = "", capacity = 0)

        //when
        val result = dto.validate()

        //then
        assertFalse(result.isValid)
        assertEquals(3, result.errors.size)
    }

    @Test
    fun `RoomEditDto valid data should pass validation`() {
        //given
        val dto = RoomEditDto(id = 1, floor = 5, number = "101", capacity = 4)

        //when
        val result = dto.validate()

        //then
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `RoomEditDto invalid id should fail validation`() {
        //given
        val dtoZero = RoomEditDto(id = 0, floor = 5, number = "101", capacity = 4)
        val dtoNegative = RoomEditDto(id = -1, floor = 5, number = "101", capacity = 4)

        //when then
        assertFalse(dtoZero.validate().isValid)
        assertFalse(dtoNegative.validate().isValid)
    }

    @Test
    fun `RoomEditDto invalid floor and number should fail validation`() {
        //given
        val dtoFloor = RoomEditDto(id = 1, floor = -101, number = "101", capacity = 4)
        val dtoEmptyNumber = RoomEditDto(id = 1, floor = 5, number = "", capacity = 4)

        //when then
        assertFalse(dtoFloor.validate().isValid)
        assertFalse(dtoEmptyNumber.validate().isValid)
    }

    @Test
    fun `RoomEditDto invalid capacity should fail validation`() {
        //given
        val dtoZero = RoomEditDto(id = 1, floor = -101, number = "101", capacity = 0)
        val dtoNegative = RoomEditDto(id = 1, floor = -101, number = "101", capacity = -5)
        val dtoTooHigh = RoomEditDto(id = 1, floor = -101, number = "101", capacity = 101)

        //when then
        assertFalse(dtoZero.validate().isValid)
        assertFalse(dtoNegative.validate().isValid)
        assertFalse(dtoTooHigh.validate().isValid)
    }

    @Test
    fun `RoomEditDto null capacity should pass validation`() {
        //given
        val dto = RoomEditDto(id = 1, floor = 5, number = "101", capacity = null)

        //when then
        assertTrue(dto.validate().isValid)
    }

    @Test
    fun `RoomEditDto multiple validation errors should be returned`() {
        //given
        val dto = RoomEditDto(id = 0, floor = -101, number = "", capacity = 0)

        //when
        val result = dto.validate()

        //then
        assertFalse(result.isValid)
        assertEquals(4, result.errors.size)
    }
}
