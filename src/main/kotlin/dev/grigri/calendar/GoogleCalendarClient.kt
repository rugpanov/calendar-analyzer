package dev.grigri.calendar

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class GoogleCalendarClient {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val scopes = listOf(CalendarScopes.CALENDAR_READONLY)
    private val credentialsFolder = File(System.getProperty("user.home"), ".calendar-analyzer")

    private fun getCredentials(): Credential {
        val credentialsFile = File(credentialsFolder, "credentials.json")
        if (!credentialsFile.exists()) {
            throw IllegalStateException(
                "Missing credentials.json. Please place your Google OAuth credentials at: ${credentialsFile.absolutePath}"
            )
        }

        val clientSecrets = GoogleClientSecrets.load(
            jsonFactory,
            InputStreamReader(FileInputStream(credentialsFile))
        )

        val tokensDirectory = File(credentialsFolder, "tokens")
        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, scopes
        )
            .setDataStoreFactory(FileDataStoreFactory(tokensDirectory))
            .setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    private val service: Calendar by lazy {
        Calendar.Builder(httpTransport, jsonFactory, getCredentials())
            .setApplicationName("Calendar Analyzer")
            .build()
    }

    fun getEvents(timeMin: DateTime, timeMax: DateTime): List<Event> {
        val events = mutableListOf<Event>()
        var pageToken: String? = null

        do {
            val eventList = service.events().list("primary")
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setSingleEvents(true)
                .setOrderBy("startTime")
                .setPageToken(pageToken)
                .execute()

            events.addAll(eventList.items ?: emptyList())
            pageToken = eventList.nextPageToken
        } while (pageToken != null)

        return events
    }
}
