package com.paolonata.shoppinglist.receipts

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceTest {

    @Test
    fun `a complete address reads like someone wrote it`() {
        assertEquals(
            "Viale Papiniano 44, 20123 Milano, Italia",
            AddressFormatter.compose(
                road = "Viale Papiniano",
                houseNumber = "44",
                city = "Milano",
                postcode = "20123",
                country = "Italia",
            ),
        )
    }

    @Test
    fun `missing pieces leave no empty commas`() {
        // Fuori dai centri storici il civico non c'è, e in certi paesi
        // nemmeno il CAP: concatenare alla cieca darebbe «, , Milano».
        assertEquals(
            "Via Roma, Milano",
            AddressFormatter.compose(road = "Via Roma", city = "Milano"),
        )
        assertEquals("Milano", AddressFormatter.compose(city = "Milano"))
        assertEquals("", AddressFormatter.compose())
    }

    @Test
    fun `the district is dropped when it just repeats the city`() {
        assertEquals(
            "Via Roma, Milano",
            AddressFormatter.compose(road = "Via Roma", suburb = "Milano", city = "Milano"),
        )
        assertEquals(
            "Via Roma, Navigli, Milano",
            AddressFormatter.compose(road = "Via Roma", suburb = "Navigli", city = "Milano"),
        )
    }

    @Test
    fun `blank pieces count as missing`() {
        assertEquals(
            "Via Roma, Milano",
            AddressFormatter.compose(road = "Via Roma", houseNumber = "   ", city = "Milano"),
        )
    }

    @Test
    fun `a place without a name falls back to something useful`() {
        assertEquals(
            "Esselunga",
            AddressFormatter.fallbackName("Esselunga, Viale Papiniano, Milano", null, null),
        )
        assertEquals("Via Roma", AddressFormatter.fallbackName(null, "Via Roma", "Milano"))
        assertEquals("Milano", AddressFormatter.fallbackName(null, null, "Milano"))
        assertEquals("Luogo", AddressFormatter.fallbackName(null, null, null))
    }

    @Test
    fun `one line joins name and address, or just the name`() {
        assertEquals(
            "Esselunga · Viale Papiniano 44, Milano",
            Place("Esselunga", "Viale Papiniano 44, Milano", 45.4, 9.1).oneLine(),
        )
        assertEquals("Esselunga", Place("Esselunga", "", 45.4, 9.1).oneLine())
    }
}
