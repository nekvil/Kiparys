package com.example.kiparys.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kiparys.Constants.APP_PREFERENCES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreRepository private constructor(context: Context) {

    private val Context.dataStore by preferencesDataStore(name = APP_PREFERENCES)
    private val dataStore = context.dataStore

    private suspend fun <T> putValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    private fun <T> getValue(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }
    }

    suspend fun putString(key: String, value: String) {
        putValue(stringPreferencesKey(key), value)
    }

    fun getString(key: String, defaultValue: String = ""): Flow<String> {
        return getValue(stringPreferencesKey(key), defaultValue)
    }

    suspend fun putBoolean(key: String, value: Boolean) {
        putValue(booleanPreferencesKey(key), value)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean> {
        return getValue(booleanPreferencesKey(key), defaultValue)
    }

    suspend fun putInt(key: String, value: Int) {
        putValue(intPreferencesKey(key), value)
    }

    fun getInt(key: String, defaultValue: Int = 0): Flow<Int> {
        return getValue(intPreferencesKey(key), defaultValue)
    }

    suspend fun remove(key: String) {
        dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(key))
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DataStoreRepository? = null

        fun getInstance(context: Context): DataStoreRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataStoreRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
