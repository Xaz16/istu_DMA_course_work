package edu.istu.achipiga.coursework

import android.app.Application
import edu.istu.achipiga.coursework.data.local.AppDatabase
import edu.istu.achipiga.coursework.data.local.SavedCityEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CourseWorkApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.build(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            val dao = database.savedCityDao()
            if (dao.count() == 0) {
                DEFAULT_CITIES.forEachIndexed { index, name ->
                    dao.insert(SavedCityEntity(apiName = name, sortOrder = index))
                }
            }
        }
    }

    private companion object {
        val DEFAULT_CITIES = listOf(
            "Izhevsk",
            "Moscow",
            "Saint Petersburg",
            "Novosibirsk",
            "Yekaterinburg",
            "Kazan",
            "Chelyabinsk",
            "Samara",
            "Omsk",
            "Ufa",
            "Krasnoyarsk",
            "Voronezh",
            "Perm",
            "Volgograd"
        )
    }
}
