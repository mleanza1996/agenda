package com.agenda.Class

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.agenda.R
import com.agenda.Utilities.DatePredictor
import com.agenda.Utilities.FirestoreHandler
import com.agendapersonale.src.Class.Note
import com.agendapersonale.src.Class.NoteData
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NotificationService : Service() {

    // Esecutore per pianificare attività periodiche, come il controllo degli eventi
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    // Set che contiene gli eventi già notificati per evitare duplicati
    private val notifiedEvents = mutableSetOf<NoteIdentifier>()

    // Contatore per generare ID univoci per le notifiche
    private val notificationId = AtomicInteger(0)

    // Gestore delle preferenze per le impostazioni delle notifiche
    private lateinit var preferences: SharedPreferencesManager

    companion object {
        private const val CHANNEL_ID = "NotificationServiceChannel" // ID del canale delle notifiche
        private const val FOREGROUND_NOTIFICATION_ID = 1 // ID della notifica del servizio in foreground
    }

    override fun onCreate() {
        super.onCreate()
        preferences = SharedPreferencesManager(this) // Inizializza le preferenze
        createNotificationChannel() // Crea il canale delle notifiche
        startForegroundService() // Avvia il servizio in foreground
        startNotificationScheduler() // Pianifica il controllo periodico degli eventi
    }

    // Avvia il servizio in foreground con una notifica persistente
    private fun startForegroundService() {
        val notification = createServiceNotification(
            "Servizio attivo",
            "Il servizio sta girando in background." // Messaggio informativo
        )
        startForeground(FOREGROUND_NOTIFICATION_ID, notification) // Avvia il servizio in foreground
    }

    // Pianifica il controllo periodico degli eventi per inviare notifiche ogni minuto
    private fun startNotificationScheduler() {
        executor.scheduleAtFixedRate({
            if (!preferences.isNotifyEnabled) {
                // Esce se le notifiche sono disabilitate nelle preferenze
                return@scheduleAtFixedRate
            }

            // Recupera gli eventi dal database Firestore
            FirestoreHandler.viewNote { events ->
                events.forEach { (noteDate, note, noteID) ->
                    // Controlla se l'evento non è già stato notificato
                    if (noteID !in notifiedEvents) {
                        DatePredictor.addDate(noteDate) // Aggiunge la data dell'evento al predittore
                        val notificationMessage = buildNotificationMessage(note, noteDate) // Crea il messaggio della notifica

                        when {
                            noteDate.inPreavviso() -> {
                                // Invia notifica di preavviso se l'evento è imminente
                                sendNotification("Preavviso evento", notificationMessage)
                                notifiedEvents.add(noteID) // Aggiunge l'evento al set di notificati
                            }
                            noteDate.inProgress() -> {
                                // Invia notifica se l'evento è in corso
                                sendNotification("Evento in corso", notificationMessage)
                                notifiedEvents.add(noteID) // Aggiunge l'evento al set di notificati
                            }
                            noteDate.isPast() -> {
                                // Aggiungi evento passato agli eventi notificati per evitare future notifiche
                                notifiedEvents.add(noteID)
                            }
                        }
                    }
                }
            }
        }, 0, 1, TimeUnit.MINUTES) // Esegue ogni minuto
    }

    // Crea il messaggio della notifica combinando i dettagli della nota e della data
    private fun buildNotificationMessage(note: Note, noteDate: NoteData): String {
        return "${note.toString()} ${noteDate.toString()}" // Combina il testo della nota e della data
    }

    // Invia una notifica all'utente con titolo e contenuto specificati
    private fun sendNotification(title: String, content: String) {
        // Verifica che le notifiche siano abilitate e i permessi siano concessi
        if (!preferences.isNotifyEnabled || (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) {
            // Non inviare la notifica se sono disabilitate o manca il permesso
            return
        }

        val notificationManager = NotificationManagerCompat.from(this)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.event_note) // Icona della notifica
            .setContentTitle(title) // Titolo della notifica
            .setContentText(content) // Contenuto della notifica
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // Stile di testo esteso per il contenuto
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Priorità alta per dare enfasi
            .build()

        notificationManager.notify(notificationId.incrementAndGet(), notification) // Mostra la notifica con un ID univoco
    }

    // Crea la notifica per il servizio in foreground con titolo e contenuto forniti
    private fun createServiceNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.event_note) // Icona della notifica di servizio
            .setContentTitle(title) // Titolo della notifica di servizio
            .setContentText(content) // Testo della notifica di servizio
            .setPriority(NotificationCompat.PRIORITY_LOW) // Priorità bassa per il servizio in background
            .build()
    }

    // Crea il canale di notifica necessario per Android 8.0 e versioni successive
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Crea un canale di notifica con ID, nome e livello di importanza
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notification Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Notification Service" // Descrizione del canale
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel) // Registra il canale di notifica
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown() // Ferma l'esecutore quando il servizio viene distrutto
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Servizio non vincolato, ritorna null
    }
}