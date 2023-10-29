package environment.outdoors.classes

import environment.backrooms.classes.House
import environment.globals.EnvGlobals
import environment.outdoors.json.jOutdoor
import jsonParser
import kotlinx.serialization.decodeFromString
import environment.network.enums.ErrorType
import java.io.File

class Outdoor(
    val envGlobals: EnvGlobals
) {
    private var header = ""

    public fun getHeader() = header

    //-------------------------------------------------------------------------------------------------------------House

    private val house = House(this)

    fun getHouse() = house


    //------------------------------------------------------------------------------------------------------WEATHER ZONE

    private var weather = Weather(this)

    fun getWeather() = weather


    //--------------------------------------------------------------------------------------------------Outdoor map path

    private var outdoorMap = String

    fun getOutdoorMap() = outdoorMap


    //------------------------------------------------------------------------------------------------Outdoor generation

    private fun parseOutdoor(path: String): ErrorType {

        val jsonText: String
        try {
            val file = File(path)
            jsonText = file.readText()
        } catch (_: Exception) {
            return ErrorType.IOError
        }

        val jOutdoorDesc: jOutdoor
        try {
            jOutdoorDesc = jsonParser.decodeFromString(jsonText)
        } catch (_: Exception) {
            return ErrorType.ParseError
        }

        val result = ErrorType.OK
        result.data = jOutdoorDesc
        return result
    }

    fun genOutdoor(path: String): ErrorType {

        val parseResult = parseOutdoor(path)
        if (parseResult != ErrorType.OK) {
            return parseResult
        }
        val jOutdoorJSON = parseResult.data as jOutdoor

        header = jOutdoorJSON.header

        house.funHouseGen(jOutdoorJSON.path_house)

        weather.genWeather(jOutdoorJSON.path_weather)

        return ErrorType.OK
    }
}