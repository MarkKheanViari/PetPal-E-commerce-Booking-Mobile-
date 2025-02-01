data class Service(
    val id: Int = 0,
    val serviceName: String = "",
    val description: String = "",
    var status: String = "",
    var userId: Int? = null,
    var selectedDate: String? = null
)