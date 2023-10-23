package environment.network.default_NN.enums

enum class ErrorType {
    IOError {
        override lateinit var data: Any
    },
    ParseError {
        override lateinit var data: Any
    },
    GenError {
        override lateinit var data: Any
    },
    OK {
        override lateinit var data: Any
    };

    abstract var data: Any
}