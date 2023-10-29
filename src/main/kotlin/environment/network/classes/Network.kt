package environment.network.classes

import jsonParser
import kotlinx.serialization.decodeFromString
import logs
import environment.network.enums.ActivationFunction
import environment.network.enums.ErrorType
import environment.network.json.jNetwork
import java.io.File

class Network {
    private var header = ""
    private val network = ArrayList<Layer>()

    //-> обратное распространение ошибки

    /**
     * Прямое распространение сигнала в нейронной сети
     */
    fun doNextCycle(input: ArrayList<Double>): ArrayList<Double> {
        for (index in 0 until network[0].neurons.size) {
            network[0].neurons[index].signal = input[index]
        }

        for (layer in network) {
            //TODO: здесь можно выполнить параллелизацию вычислений
            for (neuron in layer.neurons) {
                neuron.pullSignal()
            }
        }

        val result = ArrayList<Double>()

        for (neuron in network[network.size - 1].neurons) {
            result.add(neuron.pushSignal())
        }

        return result
    }

    /**
     * Обратное распространение ошибки в нейронной сети
     */
    fun deepLearning() {
        for (index in network.indices.reversed()) {
            //TODO: здесь можно выполнить параллелизацию вычислений
            for (neuron in network[index].neurons) {
                neuron.pullError()
            }
        }
    }


    private fun fileParser(path: String): ErrorType {

        val jsonText: String
        try {
            val file = File(path)
            jsonText = file.readText()
        } catch (_: Exception) {
            return ErrorType.IOError
        }

        val jNetworkDesc: jNetwork
        try {
            jNetworkDesc = jsonParser.decodeFromString(jsonText)
        } catch (e: Exception) {
            return ErrorType.ParseError
        }

        if (logs[0]) {
            println("____/Input JSON\\____")
            for (layer in jNetworkDesc.layers) {
                for (neuron in layer.neurons) {
                    print("|-> ${neuron.activation} : [ ")
                    for (synapse in neuron.synapses) print("$synapse ")
                    print("]\n")
                }
                println("--------------------")
            }
        }

        val result = ErrorType.OK
        result.data = jNetworkDesc
        return result
    }

    private fun activationParser(
        activation: String
        ): ActivationFunction {

        return when (activation) {
            "none" -> ActivationFunction.NONE
            "threshold" -> ActivationFunction.THRESHOLD
            else -> ActivationFunction.NONE
        }
    }

    fun genNeuralNetwork(path: String): ErrorType {
        val parseResult = fileParser(path)

        if (parseResult != ErrorType.OK)
            return parseResult

        network.clear()

        val jNetworkJSON = parseResult.data as jNetwork

        header = jNetworkJSON.header

        //Добавляем множество слоёв нейронной сети
        for (layerJSON in jNetworkJSON.layers) {
            val layer = Layer()
            layer.learnable = layerJSON.learnable
            network.add(layer)
        }

        //Проходимся от первого до последнего слоя НС
        for (layerIndex in 0 until jNetworkJSON.layers.size) {
            //Для каждого нейрона в слое выполняем некоторые действия
            for (neuronJSON in jNetworkJSON.layers[layerIndex].neurons) {
                //Создаём множество синапсов
                val synapses = ArrayList<Synapse>(jNetworkJSON.layers.size)

                val neuronCurrLayer = Neuron(
                    neuronJSON.name,
                    synapses,
                    ArrayList(),
                    activationParser(
                        neuronJSON.activation
                    ),
                    neuronJSON.parameters
                )

                //Если слой не первый, то создаём соединения
                if (layerIndex > 0) {
                    //Уравниваем влияние синапсов на нейрон
                    val weight = 1.0 / neuronJSON.synapses.size
                    //Проходимся по списку предполагаемым синапсов до индексов нейронов
                    for (synapse in neuronJSON.synapses) {
                        //Проходимся по всем индексам нейронов в следующем слое
                        for (neuronPrevLayer in network[layerIndex - 1].neurons)
                            //Если есть такой нейрон по номеру, синапс для которого было определён,то ...
                            if (neuronPrevLayer.name == synapse) {
                                //... создаём синапс на основе данных, прочитанных из файла
                                synapses.add(
                                    Synapse(
                                        neuronPrevLayer,
                                        weight = weight
                                    )
                                )
                                //... ну и добавляем предыдущему нейрону обратную связь с текущим
                                // для дальнейшего ускоренного параллельного обучения нейронной сети
                                neuronPrevLayer.errorConnections.add(
                                    Synapse(
                                        neuronCurrLayer,
                                        weight = weight
                                    )
                                )
                            }
                    }
                }

                //Создаём нейрон
                network[layerIndex].neurons.add(
                    neuronCurrLayer
                )

            }
        }

        return ErrorType.OK
    }

    fun getNetwork(): ArrayList<Layer> {
        return network
    }

    fun printNetwork() {
        var amount = 0
        val header = "___/ Network: \"$header\" \\___"

        println(header)
        for (layer in network) {
            for (neuron in layer.neurons) {
                amount++
                print("|-> ${neuron.name} - ${neuron.getActivationFunction()} ( ")
                for (parameter in neuron.getActivationParameters()) print("$parameter")
                print(" ) - ")
                for (synapse in neuron.getSynapsesString()) print("[ $synapse ]")
                if (neuron.getSynapsesString().size == 0) print("NO SYNAPSES")
                print("\n")
            }
            for (length in 0 until header.count()) print("-")
            println()
        }
        println("Neuron amount: $amount\n")
    }
}